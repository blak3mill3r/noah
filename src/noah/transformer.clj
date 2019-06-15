(ns noah.transformer
  (:require [noah.fn-wrap :as fn-wrap]
            [clojure.spec.alpha :as s]
            [com.rpl.specter :refer :all])
  (:import [org.apache.kafka.streams.kstream Transformer TransformerSupplier]))

;; how about some deftype and a macro to do transformers or processors
;; okay that's not bad I guess:

(def ^:dynamic *context* nil)
(def ^:dynamic *stores* nil)

(def punctuation-types
  {:noah.core/wall-clock-time org.apache.kafka.streams.processor.PunctuationType/WALL_CLOCK_TIME
   :noah.core/stream-time     org.apache.kafka.streams.processor.PunctuationType/STREAM_TIME})

(defn wrap-punctuation-type [t]
  (cond (keyword? t) (get punctuation-types t)
        (= (type t) org.apache.kafka.streams.processor.PunctuationType) t
        :or (throw (ex-info "No such punctuation type " {:t t}))))

(defn wrap-frequency [d]
  (cond-> d
    (int? d) java.time.Duration/ofMillis))

;; A glue type which holds mutable references to a clojure map of StateStore instances and to the ProcessorContext
;; it calls the clojure fns supplied, using reasonable defaults, with bindings for the mutable stuff from the Kafka Streams library
(deftype NoahTransformer [init-fn transform-fn close-fn schedules store-names
                          ^:unsynchronized-mutable stores
                          ^:unsynchronized-mutable context]
  Transformer
  (init [_ c]
    (set! context c)
    (set! stores (into {} (for [n store-names] [n (.getStateStore context (name n))])))
    (doseq [{:keys [freq type fn]} schedules]
      (.schedule context
                 (wrap-frequency freq)
                 (wrap-punctuation-type type)
                 (fn-wrap/punctuator #(binding [*context* context *stores* stores] (fn %)))))
    (when init-fn        (binding [*stores* stores] (init-fn context))))
  (transform [this k v]
    (when transform-fn   (binding [*context* context *stores* stores] (transform-fn k v))))
  (close [_]
    (when close-fn       (binding [*context* context *stores* stores] (close-fn)))))

(defn- binding-forms-for-store-names
  "The bits inside of a let binding, so that each store-name is bound for use in `deftransformer` fn bodies"
  [store-names]
  (into [] cat (for [n store-names] [n `(get *stores* ~(keyword n))])))

(defn- wrap-with-bindings
  "Part of the `deftransformer` macro, wrap the `transform` fn body with special let bindings for the NoahTransformer instance"
  [store-names params+body]
  (let [[params & body] (s/unform :clojure.core.specs.alpha/params+body params+body)]
    `(~(vec params) ;; unform is broken, turn it back into a vector
      (let [~'context *context* ~@(binding-forms-for-store-names store-names)]
        ~@body))))

(defn- wrap-fn-with-bindings
  "Part of the `deftransformer` macro, wrap the punctuator fn bodies with special let bindings for the NoahTransformer instance"
  [store-names [_fn & name+tail]]
  (assert (= 'fn _fn))
  (let [{:keys [fn-name fn-tail]} (s/conform (:args (s/get-spec `fn)) name+tail)
        [_arity1 tail] fn-tail
        _ (assert (= :arity-1 _arity1))
        [params & body] (s/unform :clojure.core.specs.alpha/params+body tail)]
    `(clojure.core/fn ~@(when fn-name [fn-name])
       ~(vec params) ;; unform is broken, turn it back into a vector
       (let [~'context *context* ~@(binding-forms-for-store-names store-names)]
         ~@body))))

(defmacro deftransformer
  "Macro to ease Transformer implementation.

  (deftransformer name docstring? stores schedules+ init? close? & params+body)"
  [& args]
  (let [{:keys [name docstring store-names schedules init close params+body]} (s/conform ::deftransformer-args args)]
    `(def ~name ~@(when docstring [docstring])
       (reify TransformerSupplier
         (get [~'this]
           (->NoahTransformer
            ~(:fn init) ;; FIXME wrap with bindings?
            (fn ~(symbol (str name"-transform"))
              ~@(wrap-with-bindings store-names params+body))
            ~(:fn close) ;; FIXME wrap with bindings?
            ~(->> schedules (transform [ALL :fn] #(wrap-fn-with-bindings store-names %)))
            ~(into [] (map keyword) store-names)
            nil
            nil))))))

(s/def ::deftransformer-args
  (s/cat :name simple-symbol?
         :docstring (s/? string?)
         :store-names (s/coll-of simple-symbol? :kind vector?)
         :schedules (s/* ::schedule-stanza)
         :init      (s/? ::init-stanza)
         :close     (s/? ::close-stanza)
         :params+body :clojure.core.specs.alpha/params+body
         ))

(s/def ::schedule-stanza (s/cat :_ #{:schedule} :freq any? :type any? :fn any?))
(s/def ::init-stanza     (s/cat :_ #{:init}  :fn any?))
(s/def ::close-stanza    (s/cat :_ #{:close} :fn any?))

(s/fdef deftransformer
  :args ::deftransformer-args
  :ret any?)
