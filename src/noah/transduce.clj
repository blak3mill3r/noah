(ns noah.transduce
  "Glue to allow stateful transducers to use a StateStore for their composed state."
  (:refer-clojure :exclude [transduce])
  (:import [org.apache.kafka.streams.processor ProcessorContext]
           [org.apache.kafka.streams.kstream
            Materialized
            Transformer
            TransformerSupplier
            ValueTransformer
            ValueTransformerSupplier
            ValueTransformerWithKey
            ValueTransformerWithKeySupplier])
  (:require
   [coddled-super-centaurs.core :as csc]
   [noah.transformer :as tr]))

;;; Glue implementation

;; this returns the step-fn that will be transduced
;; the key is not passed to the step-fn
(defn value-transform-step [k]
  (fn
    ([^ProcessorContext context] context)
    ([^ProcessorContext context v]
     (.forward context k v)
     context)))

;; the trick here is to do a dummy transduce
;; which will populate an atom with the initial value for each state in xform
;; this makes the transducer code look relatively normal, as though its states were being constructed in a lexical scope
;; that is not what is happening though, we need to feed it states that were read from a StateStore
(defn make-initial-state [xform]
  (let [state-info (atom [])]
    (binding [csc/*state* (fn [v] (swap! state-info conj v) (volatile! v))]
      (clojure.core/transduce xform (constantly nil) []))
    @state-info))

;; when KStreams is actually running this transducer in a ValueTransformer,
;; this will be dynamically bound to a stateful fn which yields the nth state, as a volatile, and increments n on each call
;; this root binding is here to gather up a vector of initial states for the composed transducer stack
;;   (in the order they are called when wrapping the reducing fn)

;; This StateGluer thing will jigger up this whole mutable mess
(defprotocol StateGlue
  (init! [this k init-states])
  (finish! [this k])
  (yield-state! [this k]))

;; it is basically a place to store states in memory between actual writes to the StateStore
;; which also statefully yields the nth state volatile on the nth call to yield-state!
(deftype StateGluer
    [store-name context
     ^:unsynchronized-mutable states
     ^:unsynchronized-mutable n]
  StateGlue
  (init! [this k init-states]
    (set! states (mapv volatile! (-> context (.getStateStore (name store-name)) (.get k) (or init-states))))
    (set! n 0))
  (finish! [this k]
    (let [next-states (mapv deref states)]
      (-> context (.getStateStore (name store-name)) (.put k next-states))))
  (yield-state! [this k]
    (set! n (inc n))
    (nth states (dec n))))

(defn new-gluer [store-name context]
  (->StateGluer store-name context nil nil))

;;; Transformer implementation

(defn- make-transducing-transformer
  [xform & [state-store-name initial-state]]
  (let [theglue (volatile! nil)]
    (reify
      org.apache.kafka.streams.kstream.TransformerSupplier
      ;; org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
      (get [this]
        (tr/->NoahTransformer
         nil
         (fn [k v]
           (let [step-fn (value-transform-step k)
                 context (tr/context)
                 g (or @theglue (let [v (new-gluer state-store-name context)] (vreset! theglue v) v))]
             (init! g k initial-state)
             (binding [csc/*state* (fn [_] (yield-state! g k))]
               ((xform step-fn) context v))
             (finish! g k)
             nil))
         nil
         nil
         (if state-store-name [state-store-name] [])
         nil
         nil)))))

(defn transducing-transformer
  ([xform]
   (when (not-empty (make-initial-state xform)) (throw (IllegalArgumentException. "Fault-tolerant stateful transduction requires a state-store-name")))
   (make-transducing-transformer xform))
  ([xform state-store-name]
   (let [initial-state (make-initial-state xform)]
     (make-transducing-transformer xform state-store-name initial-state))))
