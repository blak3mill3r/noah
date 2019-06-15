(ns noah.transduce
  "Implementation of composable stateful transducers which use a StateStore for their composed state, and some
  glue to support them."
  (:refer-clojure :exclude [partition-by partition-all transduce interpose take drop drop-while distinct map-indexed keep-indexed])
  (:import [org.apache.kafka.streams.processor ProcessorContext]
           [org.apache.kafka.streams.kstream
            Materialized
            Transformer
            TransformerSupplier
            ValueTransformer
            ValueTransformerSupplier
            ValueTransformerWithKey
            ValueTransformerWithKeySupplier])
  (:require [noah.transformer :as tr]))

;;; Glue implementation

;; this returns the step-fn that will be transduced
;; the key is not passed to the step-fn
(defn value-transform-step [k]
  (fn
    ([^ProcessorContext context] context)
    ([^ProcessorContext context v]
     (.forward context k v)
     context)))

(def ^:dynamic *state-info* nil)

;; the trick here is to do a dummy transduce
;; which will populate an atom with the initial value for each state in xform
;; this makes the transducer code look relatively normal, as though its states were being constructed in a lexical scope
;; that is not what is happening though, we need to feed it states that were read from a StateStore
(defn make-initial-state [xform]
  (let [state-info (atom [])]
    (binding [*state-info* state-info] (clojure.core/transduce xform (constantly nil) []))
    @state-info))

;; when KStreams is actually running this transducer in a ValueTransformer,
;; this will be dynamically bound to a stateful fn which yields the nth state, as a volatile, and increments n on each call
;; this root binding is here to gather up a vector of initial states for the composed transducer stack
;;   (in the order they are called when wrapping the reducing fn)
(defn ^:dynamic *state* [v]
  (swap! *state-info* conj v)
  (volatile! v))

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
                 context tr/*context*
                 g (or @theglue (let [v (new-gluer state-store-name context)] (vreset! theglue v) v))]
             (init! g k initial-state)
             (binding [*state* (fn [_] (yield-state! g k))]
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

;;; Stateful transducer implementations

;; see `clojure.core/partition-by`
;; https://github.com/clojure/clojure/blob/clojure-1.10.1/src/clj/clojure/core.clj#L7160
(defn partition-by
  "Returns a noah stateful transducer which applies f to each value, outputting vectors
  which are split each time f returns a new value."
  [f]
  (fn [rf]
    (let [a (*state* [])
          p (*state* ::none)]
      (fn
        ([] (rf))
        ([result]
         (let [av @a result (if (empty? av)
                              result
                              (do
                                (reset! a [])
                                (unreduced (rf result av))))]
           (rf result)))
        ([result input]
         (let [pv @p val (f input)]
           (vreset! p val)
           (if (or (identical? pv ::none) (= val pv))
             (do (vswap! a conj input) result)
             (let [av @a]
               (vreset! a [])
               (let [ret (rf result av)]
                 (when-not (reduced? ret)
                   (vswap! a conj input))
                 ret)))))))))

;; see `clojure.core/partition-all`
;; https://github.com/clojure/clojure/blob/clojure-1.10.1/src/clj/clojure/core.clj#L7240
(defn partition-all
  "Returns a noah stateful transducer which partitions values together into output vectors of a fixed length n."
  [^long n]
  (fn [rf]
    (let [a (*state* [])]
      (fn
        ([] (rf))
        ([result]
         (let [av @a result (if (empty? av)
                              result
                              (do
                                (reset! a [])
                                (unreduced (rf result av))))]
           (rf result)))
        ([result input]
         (let [av (vswap! a conj input)]
           (if (= n (count av))
             (do 
               (vreset! a [])
               (rf result av))
             result)))))))

;; see `clojure.core/interpose`
;; https://github.com/clojure/clojure/blob/clojure-1.10.1/src/clj/clojure/core.clj#L5206
(defn interpose
  "Returns a noah stateful transducer which separates elements by sep."
  [sep]
  (fn [rf]
    (let [started (*state* false)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (if @started
           (let [sepr (rf result sep)]
             (if (reduced? sepr)
               sepr
               (rf sepr input)))
           (do
             (vreset! started true)
             (rf result input))))))))

(defn take
  "Returns a noah stateful transducer which stops after n items. This can cause records to be skipped."
  [n]
  (fn [rf]
    (let [nv (*state* n)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (let [n @nv
               nn (vswap! nv dec)
               result (if (pos? n)
                        (rf result input)
                        result)]
           (if (not (pos? nn))
             (ensure-reduced result)
             result)))))))

(defn drop
  "Returns a noah stateful transducer which drops n items before transducing any remaining items."
  [n]
  (fn [rf]
    (let [nv (*state* n)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (let [n @nv]
           (vswap! nv dec)
           (if (pos? n)
             result
             (rf result input))))))))

(defn drop-while
  "Returns a noah stateful transducer which drops items as long as they all pass the predicate pred, then transduces the remaining items."
  [pred]
  (fn [rf]
    (let [dv (*state* true)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (let [drop? @dv]
           (if (and drop? (pred input))
             result
             (do
               (vreset! dv nil)
               (rf result input)))))))))

(defn distinct
  "Returns a noah stateful transducer which removes duplicate items and transduces each distinct item."
  []
  (fn [rf]
    (let [seen (*state* #{})]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (if (contains? @seen input)
           result
           (do (vswap! seen conj input)
               (rf result input))))))))

(defn map-indexed
  "Returns a noah stateful transducer which transduces the result of applying f to 0 and the first item,
  followed by applying f to 1 and the second item, etc."
  [f]
  (fn [rf]
    (let [i (*state* -1)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (rf result (f (vswap! i inc) input)))))))

(defn keep-indexed
  "Returns a noah stateful transducer which transduces the non-nil results of (f index item).
  Note, this means false return values will be included."
  [f]
  (fn [rf]
    (let [iv (*state* -1)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (let [i (vswap! iv inc)
               v (f i input)]
           (if (nil? v)
             result
             (rf result v))))))))
