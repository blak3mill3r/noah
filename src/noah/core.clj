(ns noah.core
  "Provides an interface to the Kafka Streams Java API"
  (:refer-clojure
   :rename {filter   core-filter
            map      core-map
            reduce   core-reduce
            count    core-count
            group-by core-group-by
            peek     core-peek
            print    core-print
            merge    core-merge})
  (:require [clojure.reflect :as ref]
            [camel-snake-kebab.core :refer [->kebab-case]]
            [noah.fn-wrap :as fn-wrap]
            [noah.serdes]
            [clojure.string :as str]
            [franzy.serialization.nippy.serializers :refer [nippy-serializer]]
            [franzy.serialization.nippy.deserializers :refer [nippy-deserializer]]
            [potemkin])
  (:import
   [org.apache.kafka.clients.consumer ConsumerConfig]
   [org.apache.kafka.common.serialization Serdes Serde]
   [org.apache.kafka.common.utils Bytes]
   [org.apache.kafka.streams KafkaStreams StreamsBuilder StreamsConfig KeyValue]
   [org.apache.kafka.streams.kstream Aggregator Consumed GlobalKTable Initializer Joined JoinWindows KeyValueMapper ValueMapperWithKey KGroupedStream KGroupedTable KStream KTable Materialized Merger Predicate Produced Reducer Serialized SessionWindowedKStream SessionWindows ValueJoiner ValueMapper Windows TransformerSupplier Transformer ValueTransformerWithKeySupplier ValueTransformerWithKey ValueTransformerSupplier ValueTransformer]
   [org.apache.kafka.streams.kstream.internals KTableImpl KStreamImpl KGroupedStreamImpl]
   [org.apache.kafka.streams.state KeyValueStore]
   [org.apache.kafka.streams.processor TopicNameExtractor]
   [java.util Collections Map Properties]))

(potemkin/import-vars [noah.serdes serdes consumed produced serialized])

#_(ref/reflect TransformerSupplier)
#_(ref/reflect Transformer)
;; what to do with Transformer?
;; steal that transducer-transformer idea
;; can I make new implementations of clojure's standard stateful transducers
;; which use StateStore, instead of volatiles?
;; 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; a few hand-written wrappers

(defn materialized-as
  [store-name]
  (Materialized/as store-name))

(defn kafka-streams
  [builder config]
  (KafkaStreams. (.build builder) (StreamsConfig. config)))

(defn streams-builder [] (StreamsBuilder.))

(defn kv [k v] (KeyValue/pair k v))

;; useless?
#_(def ^:dynamic *builder* nil)
#_(defmacro with-builder [& body]
    `(let [b# (streams-builder)]
       (binding [*builder* b#]
         ~@body)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; * Implementation of `(defwrappers)` macro
;; first let's make it less Java-y with a hierarchy for multimethod dispatch
;; this means you can pass either a ValueJoiner or a Clojure function
;; multimethod dispatch will take care of finding the matching signature on the
;; right target class

(derive clojure.lang.IPersistentMap ::consumed)   (derive Consumed ::consumed)
(derive clojure.lang.IPersistentMap ::produced)   (derive Produced ::produced)
(derive clojure.lang.IPersistentMap ::serialized) (derive Serialized ::serialized)
;; (derive clojure.lang.Fn        ::fn)
(derive Initializer            ::fn-0)
(derive ValueMapper            ::fn-1)
(derive ValueMapperWithKey     ::fn-2)
(derive KeyValueMapper         ::fn-2)
(derive ValueJoiner            ::fn-2)
(derive Reducer                ::fn-2)
(derive Predicate              ::fn-2)
(derive Aggregator             ::fn-3)
(derive Merger                 ::fn-3)
(derive TopicNameExtractor     ::fn-3)
(derive KStream                ::stream)
(derive KGroupedStream         ::stream)
(derive SessionWindowedKStream ::stream)
(derive KTable                 ::table)
(derive KTableImpl             ::table) ;; can automate using java reflection?
(derive KGroupedTable          ::table)
(derive GlobalKTable           ::global-table)
(derive JoinWindows            ::join-windows)
(derive Windows                ::windows)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; For each type in Kafka Streams API: fn to convert a Clojure value
;; to be passed as a parameter of a that type

(def ^:private conversion-fn
  {Aggregator                      `fn-wrap/aggregator
   Initializer                     `fn-wrap/initializer
   KeyValueMapper                  `fn-wrap/key-value-mapper
   Merger                          `fn-wrap/merger
   Predicate                       `fn-wrap/predicate
   Reducer                         `fn-wrap/reducer
   ValueJoiner                     `fn-wrap/value-joiner
   ValueMapper                     `fn-wrap/value-mapper
   ValueMapperWithKey              `fn-wrap/value-mapper-with-key
   Consumed                        `consumed
   Produced                        `produced
   Serialized                      `serialized
   GlobalKTable                    `identity
   KGroupedTable                   `identity
   java.util.regex.Pattern         `identity
   String                          `identity
   Joined                          `identity
   JoinWindows                     `identity
   KGroupedStream                  `identity
   KStream                         `identity
   KTable                          `identity
   Materialized                    `identity
   SessionWindowedKStream          `identity
   SessionWindows                  `identity
   TransformerSupplier             `identity
   ValueTransformerSupplier        `identity
   ValueTransformerWithKeySupplier `identity
   Windows                         `identity
   StreamsBuilder                  `identity
   TopicNameExtractor              `identity})

(defn type-with-fn-arity [o]
  (if (fn? o)
    (or (try (nth [::fn-0 ::fn-1 ::fn-2 ::fn-3] (fn-wrap/arity o)) (catch Exception _ nil))
        (throw (ex-info "You can only pass noah functions of arity 0,1,2,3." {:arity (fn-wrap/arity o)})))
    (type o)))

(defn contained-type [collection]
  (type-with-fn-arity (first collection)))

;; this is the multimethod dispatch fn: a vector of the types of the arguments
(defn- types-vector [& args] (into [] (core-map type-with-fn-arity) args))
(defn- types-vector-varargs [& args] (into []
                                           (core-map type-with-fn-arity)
                                           (concat (drop-last args)
                                                   (list (contained-type (last args))))))

;; implementation details:

(defn- remove-varargs [type-sym]
  (cond-> type-sym
    (str/ends-with? (str type-sym) "<>")
    (-> str (str/replace-first "<>" "") symbol)))

(defn- try-resolve [type]
  (->> type remove-varargs (ns-resolve 'noah.core)))

(defn- noah-parent [type]
  (when (nil? (first (for [t (ancestors type) :when (and (keyword? t) (= "noah.core" (namespace t)))] t)))
    (println "noah parent is nil for " type))
  (first (for [t (ancestors type) :when (and (keyword? t) (= "noah.core" (namespace t)))] t)))

(defn- docstring-part [sym]
  (let [type-rep (-> sym try-resolve noah-parent (or (-> sym try-resolve)))]
    (if (keyword? type-rep)
      (name type-rep)
      (.getSimpleName type-rep))))

(defn- docstring-wrapper
  [name sigs]
  (->> (for [{:keys [parameter-types declaring-class]} sigs]
         (str "[" (str/join " " (into [(docstring-part declaring-class)] (core-map docstring-part) parameter-types)) "]"))
       (str/join "\n")))

(defn- defwrapper-defmulti [name sigs]
  `(defmulti ~(->kebab-case name)
     ~(docstring-wrapper name sigs)
     ~(if (-> sigs first :varargs)
        `types-vector-varargs
        `types-vector)))

;; as in
#_(noah-parent KStream)     ;; => :noah.core/stream
#_(noah-parent ValueJoiner) ;; => :noah.core/fn-2
#_(noah-parent Aggregator)  ;; => :noah.core/fn-3
#_(noah-parent Produced)    ;; => :noah.core/produced
#_(noah-parent Consumed)    ;; => :noah.core/consumed
#_(noah-parent clojure.lang.IPersistentMap) ;; => :noah.core/consumed ;; hmm... assumption violated?

;; and also
#_(noah-parent Materialized) ;; => org.apache.kafka.streams.kstream.Materialized

#_(noah-parent org.apache.kafka.streams.kstream.internals.KTableImpl)

(def ^:private arg-symbols (into [] (core-map (comp symbol str)) "abcdefg"))

(defn- generate-convert-params-code
  [arg-types-and-syms-and-varargs]
  (for [[type sym var?] arg-types-and-syms-and-varargs]
    (if-let [f (conversion-fn (try-resolve type))]
      (cond
        ;; for the final argument of a varargs method we need to convert a collection into an array of that type
        var?
        `(into-array ~var? (clojure.core/map ~f ~'vararg))

        ;; some types are not converted, rather than emitting (identity x) as a no-op, just emit the symbol for readability
        (= 'clojure.core/identity f)
        sym

        ;; otherwise emit code that wraps the Clojure argument with the conversion function
        :else
        `(~f ~sym))
      (println "Warn: no conversion for " type))))

(defn- defwrapper-defmethod [{:keys [name parameter-types flags return-type declaring-class] :as member-fn}]
  (let [arg-types-and-syms (into [[declaring-class 'this]] (core-map vector parameter-types arg-symbols))
        convert-params-code (generate-convert-params-code arg-types-and-syms)]

    ;; need a conversion-fn for every parameter
    (if (some nil? convert-params-code) `(comment skipped ~name ~(str (prn-str convert-params-code) " <- " arg-types-and-syms))
        `(defmethod
           ;; method name
           ~(->kebab-case name)
           ;; dispatch value
           [~@(for [[t _] arg-types-and-syms] (-> t try-resolve noah-parent (or t)))]
           ;; argument vector
           [~@(core-map second arg-types-and-syms)]
           ;; function body: a call to the Java method, forwarding converted Clojure args
           (~(symbol (str "." name)) ~@convert-params-code)))))

(defn- defwrapper-varargs [{:keys [name parameter-types flags return-type declaring-class] :as member-fn}]
  (let [vararg-type (-> parameter-types last remove-varargs)
        arg-types-and-syms-and-varargs
        (into [[declaring-class 'this nil]]
              (core-map vector
                        (concat (drop-last parameter-types)
                                [vararg-type])
                        arg-symbols
                        (concat (-> parameter-types core-count dec (repeat nil))
                                (list vararg-type))))
        convert-params-code (generate-convert-params-code arg-types-and-syms-and-varargs)]

    ;; need a conversion-fn for every parameter
    (if (some nil? convert-params-code) `(comment skipped ~name ~(str (prn-str convert-params-code) " <- " arg-types-and-syms-and-varargs))
        `(defmethod
           ;; method name
           ~(->kebab-case name)
           ;; dispatch value
           [~@(for [[t _] arg-types-and-syms-and-varargs] (-> t try-resolve noah-parent (or t)))]
           ;; argument vector
           [~@(core-map second (drop-last arg-types-and-syms-and-varargs))
            ~'vararg]
           ;; function body: a call to the Java method, forwarding converted Clojure args
           (~(symbol (str "." name)) ~@convert-params-code)))))

#_(defn- wrapper-kind [[name sigs]] (if (= 1 (core-count sigs)) :wrapper-function :wrapper-multimethod))

#_(-> (reflected-methods-by-name)
      (get 'aggregate)
      (->> (core-map (juxt :declaring-class :parameter-types))))

(defn- reflected-methods-by-name
  []
  (->> (for [klass [KStream KGroupedStream KTable KGroupedTable SessionWindowedKStream StreamsBuilder]
             m (:members (ref/reflect klass))
             :when (and (-> m :flags :public)
                        (not (instance? clojure.reflect.Constructor m)))] m)
       (core-group-by :name)))

(defn is-varargs? [type-symbol]
  (-> type-symbol name
      (str/ends-with? "<>")))

#_(-> (->> (for [klass [KStream]
                 m (:members (ref/reflect klass))
                 :when (and (-> m :flags :public)
                            (not (instance? clojure.reflect.Constructor m))
                            ;; TODO support varargs
                            (-> m :flags :varargs))] m)
           (core-group-by :name))
      #_(get 'transform)
      #_(->> (into {}))
      #_(defwrapper-varargs))

(defmacro ^:private defwrappers
  []
  (let [wrapper-input (reflected-methods-by-name)]
    `(do ~@(for [[name sigs] wrapper-input]        (defwrapper-defmulti name sigs))
         ~@(for [[name sigs] wrapper-input s sigs] (if-not (:varargs (:flags s))
                                                     (defwrapper-defmethod s)
                                                     (defwrapper-varargs s))))))

(declare branch)

;; credit for this idea goes to https://github.com/bobby/kafka-streams-clojure/blob/5bdc79dee283eb97e2c26596040c20e02dc9c383/src/kafka_streams_clojure/api.clj#L70
(defn branch-map
  "Given a KStream instance and a map of
  `keyword-branch-name -> (arity-2 predicate of: k v)`
   returns a map of
  `keyword-branch-name -> KStream`
  as per KStream.branch()"
  [kstream branch-predicate-map]
  (let [[branch-names predicates] (->> branch-predicate-map seq (apply core-map vector))]
    (zipmap branch-names (branch kstream predicates))))

;; the macro expansion of this:
#_(defwrappers)

;; produces the remainder of this source file
;; which is included inline for developer convenience

(defmulti reduce "[stream fn-2 Materialized]\n[stream fn-2]\n[table fn-2 fn-2 Materialized]\n[table fn-2 fn-2]\n[stream fn-2]\n[stream fn-2 Materialized]" types-vector)
(defmulti windowed-by "[stream SessionWindows]\n[stream windows]" types-vector)
(defmulti aggregate "[stream fn-0 fn-3 Materialized]\n[stream fn-0 fn-3]\n[table fn-0 fn-3 fn-3 Materialized]\n[table fn-0 fn-3 fn-3]\n[stream fn-0 fn-3 fn-3 Materialized]\n[stream fn-0 fn-3 fn-3]" types-vector)
(defmulti peek "[stream ForeachAction]" types-vector)
(defmulti branch "[stream fn-2]" types-vector)
(defmulti map "[stream fn-2]" types-vector)
(defmulti join "[stream table fn-2 Joined]\n[stream table fn-2]\n[stream stream fn-2 join-windows Joined]\n[stream stream fn-2 join-windows]\n[stream global-table fn-2 fn-2]\n[table table fn-2]\n[table table fn-2 Materialized]" types-vector)
(defmulti flat-map-values "[stream fn-1]\n[stream fn-2]" types-vector)
(defmulti to "[stream String]\n[stream fn-3]\n[stream fn-3 produced]\n[stream String produced]" types-vector)
(defmulti queryable-store-name "[table]" types-vector)
(defmulti group-by-key "[stream serialized]\n[stream]" types-vector)
(defmulti transform "[stream TransformerSupplier String]" types-vector)
(defmulti table "[StreamsBuilder String consumed Materialized]\n[StreamsBuilder String]\n[StreamsBuilder String Materialized]\n[StreamsBuilder String consumed]" types-vector)
(defmulti add-state-store "[StreamsBuilder StoreBuilder]" types-vector)
(defmulti left-join "[stream stream fn-2 join-windows Joined]\n[stream table fn-2]\n[stream stream fn-2 join-windows]\n[stream global-table fn-2 fn-2]\n[stream table fn-2 Joined]\n[table table fn-2 Materialized]\n[table table fn-2]" types-vector)
(defmulti filter-not "[stream fn-2]\n[table fn-2]\n[table fn-2 Materialized]" types-vector)
(defmulti map-values "[stream fn-1]\n[stream fn-2]\n[table fn-2 Materialized]\n[table fn-2]\n[table fn-1 Materialized]\n[table fn-1]" types-vector)
(defmulti through "[stream String]\n[stream String produced]" types-vector)
(defmulti process "[stream ProcessorSupplier String]" types-vector)
(defmulti to-stream "[table]\n[table fn-2]" types-vector)
(defmulti print "[stream Printed]" types-vector)
(defmulti outer-join "[stream stream fn-2 join-windows]\n[stream stream fn-2 join-windows Joined]\n[table table fn-2]\n[table table fn-2 Materialized]" types-vector)
(defmulti merge "[stream stream]" types-vector)
(defmulti stream "[StreamsBuilder Pattern]\n[StreamsBuilder Collection consumed]\n[StreamsBuilder String]\n[StreamsBuilder Collection]\n[StreamsBuilder String consumed]\n[StreamsBuilder Pattern consumed]" types-vector)
(defmulti flat-map "[stream fn-2]" types-vector)
(defmulti global-table "[StreamsBuilder String consumed]\n[StreamsBuilder String consumed Materialized]\n[StreamsBuilder String Materialized]\n[StreamsBuilder String]" types-vector)
(defmulti transform-values "[stream ValueTransformerWithKeySupplier String]\n[stream ValueTransformerSupplier String]\n[table ValueTransformerWithKeySupplier String]\n[table ValueTransformerWithKeySupplier Materialized String]" types-vector)
(defmulti build "[StreamsBuilder]" types-vector)
(defmulti filter "[stream fn-2]\n[table fn-2]\n[table fn-2 Materialized]" types-vector)
(defmulti foreach "[stream ForeachAction]" types-vector)
(defmulti count "[stream Materialized]\n[stream]\n[table Materialized]\n[table]\n[stream Materialized]\n[stream]" types-vector)
(defmulti group-by "[stream fn-2 serialized]\n[stream fn-2]\n[table fn-2 serialized]\n[table fn-2]" types-vector)
(defmulti select-key "[stream fn-2]" types-vector)
(defmulti add-global-store "[StreamsBuilder StoreBuilder String consumed ProcessorSupplier]\n[StreamsBuilder StoreBuilder String String consumed String ProcessorSupplier]" types-vector)
(defmethod
  reduce
  [:noah.core/stream
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.reduce this (fn-wrap/reducer a) b))
(defmethod
  reduce
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.reduce this (fn-wrap/reducer a)))
(defmethod
  reduce
  [:noah.core/table
   :noah.core/fn-2
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.reduce this (fn-wrap/reducer a) (fn-wrap/reducer b) c))
(defmethod
  reduce
  [:noah.core/table :noah.core/fn-2 :noah.core/fn-2]
  [this a b]
  (.reduce this (fn-wrap/reducer a) (fn-wrap/reducer b)))
(defmethod
  reduce
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.reduce this (fn-wrap/reducer a)))
(defmethod
  reduce
  [:noah.core/stream
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.reduce this (fn-wrap/reducer a) b))
(defmethod
  windowed-by
  [:noah.core/stream org.apache.kafka.streams.kstream.SessionWindows]
  [this a]
  (.windowedBy this a))
(defmethod
  windowed-by
  [:noah.core/stream :noah.core/windows]
  [this a]
  (.windowedBy this a))
(defmethod
  aggregate
  [:noah.core/stream
   :noah.core/fn-0
   :noah.core/fn-3
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.aggregate this (fn-wrap/initializer a) (fn-wrap/aggregator b) c))
(defmethod
  aggregate
  [:noah.core/stream :noah.core/fn-0 :noah.core/fn-3]
  [this a b]
  (.aggregate this (fn-wrap/initializer a) (fn-wrap/aggregator b)))
(defmethod
  aggregate
  [:noah.core/table
   :noah.core/fn-0
   :noah.core/fn-3
   :noah.core/fn-3
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c d]
  (.aggregate
   this
   (fn-wrap/initializer a)
   (fn-wrap/aggregator b)
   (fn-wrap/aggregator c)
   d))
(defmethod
  aggregate
  [:noah.core/table :noah.core/fn-0 :noah.core/fn-3 :noah.core/fn-3]
  [this a b c]
  (.aggregate
   this
   (fn-wrap/initializer a)
   (fn-wrap/aggregator b)
   (fn-wrap/aggregator c)))
(defmethod
  aggregate
  [:noah.core/stream
   :noah.core/fn-0
   :noah.core/fn-3
   :noah.core/fn-3
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c d]
  (.aggregate
   this
   (fn-wrap/initializer a)
   (fn-wrap/aggregator b)
   (fn-wrap/merger c)
   d))
(defmethod
  aggregate
  [:noah.core/stream :noah.core/fn-0 :noah.core/fn-3 :noah.core/fn-3]
  [this a b c]
  (.aggregate
   this
   (fn-wrap/initializer a)
   (fn-wrap/aggregator b)
   (fn-wrap/merger c)))
(comment
  skipped
  peek
  "(this nil)\n <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.ForeachAction a]]")
(defmethod
  branch
  [:noah.core/stream :noah.core/fn-2]
  [this vararg]
  (.branch
   this
   (into-array
    org.apache.kafka.streams.kstream.Predicate
    (clojure.core/map fn-wrap/predicate vararg))))
(defmethod
  map
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.map this (fn-wrap/key-value-mapper a)))
(defmethod
  join
  [:noah.core/stream
   :noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Joined]
  [this a b c]
  (.join this a (fn-wrap/value-joiner b) c))
(defmethod
  join
  [:noah.core/stream :noah.core/table :noah.core/fn-2]
  [this a b]
  (.join this a (fn-wrap/value-joiner b)))
(defmethod
  join
  [:noah.core/stream
   :noah.core/stream
   :noah.core/fn-2
   :noah.core/join-windows
   org.apache.kafka.streams.kstream.Joined]
  [this a b c d]
  (.join this a (fn-wrap/value-joiner b) c d))
(defmethod
  join
  [:noah.core/stream
   :noah.core/stream
   :noah.core/fn-2
   :noah.core/join-windows]
  [this a b c]
  (.join this a (fn-wrap/value-joiner b) c))
(defmethod
  join
  [:noah.core/stream
   :noah.core/global-table
   :noah.core/fn-2
   :noah.core/fn-2]
  [this a b c]
  (.join
   this
   a
   (fn-wrap/key-value-mapper b)
   (fn-wrap/value-joiner c)))
(defmethod
  join
  [:noah.core/table :noah.core/table :noah.core/fn-2]
  [this a b]
  (.join this a (fn-wrap/value-joiner b)))
(defmethod
  join
  [:noah.core/table
   :noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.join this a (fn-wrap/value-joiner b) c))
(defmethod
  flat-map-values
  [:noah.core/stream :noah.core/fn-1]
  [this a]
  (.flatMapValues this (fn-wrap/value-mapper a)))
(defmethod
  flat-map-values
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.flatMapValues this (fn-wrap/value-mapper-with-key a)))
(defmethod
  to
  [:noah.core/stream java.lang.String]
  [this a]
  (.to this a))
(defmethod
  to
  [:noah.core/stream :noah.core/fn-3]
  [this a]
  (.to this a))
(defmethod
  to
  [:noah.core/stream :noah.core/fn-3 :noah.core/produced]
  [this a b]
  (.to this a (produced b)))
(defmethod
  to
  [:noah.core/stream java.lang.String :noah.core/produced]
  [this a b]
  (.to this a (produced b)))
(defmethod
  queryable-store-name
  [:noah.core/table]
  [this]
  (.queryableStoreName this))
(defmethod
  group-by-key
  [:noah.core/stream :noah.core/serialized]
  [this a]
  (.groupByKey this (serialized a)))
(defmethod
  group-by-key
  [:noah.core/stream]
  [this]
  (.groupByKey this))
(defmethod
  transform
  [:noah.core/stream
   org.apache.kafka.streams.kstream.TransformerSupplier
   java.lang.String]
  [this a vararg]
  (.transform
   this
   a
   (into-array
    java.lang.String
    (clojure.core/map identity vararg))))
(defmethod
  table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.table this a (consumed b) c))
(defmethod
  table
  [org.apache.kafka.streams.StreamsBuilder java.lang.String]
  [this a]
  (.table this a))
(defmethod
  table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.table this a b))
(defmethod
  table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed]
  [this a b]
  (.table this a (consumed b)))
(comment
  skipped
  addStateStore
  "(this nil)\n <- [[org.apache.kafka.streams.StreamsBuilder this] [org.apache.kafka.streams.state.StoreBuilder a]]")
(defmethod
  left-join
  [:noah.core/stream
   :noah.core/stream
   :noah.core/fn-2
   :noah.core/join-windows
   org.apache.kafka.streams.kstream.Joined]
  [this a b c d]
  (.leftJoin this a (fn-wrap/value-joiner b) c d))
(defmethod
  left-join
  [:noah.core/stream :noah.core/table :noah.core/fn-2]
  [this a b]
  (.leftJoin this a (fn-wrap/value-joiner b)))
(defmethod
  left-join
  [:noah.core/stream
   :noah.core/stream
   :noah.core/fn-2
   :noah.core/join-windows]
  [this a b c]
  (.leftJoin this a (fn-wrap/value-joiner b) c))
(defmethod
  left-join
  [:noah.core/stream
   :noah.core/global-table
   :noah.core/fn-2
   :noah.core/fn-2]
  [this a b c]
  (.leftJoin
   this
   a
   (fn-wrap/key-value-mapper b)
   (fn-wrap/value-joiner c)))
(defmethod
  left-join
  [:noah.core/stream
   :noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Joined]
  [this a b c]
  (.leftJoin this a (fn-wrap/value-joiner b) c))
(defmethod
  left-join
  [:noah.core/table
   :noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.leftJoin this a (fn-wrap/value-joiner b) c))
(defmethod
  left-join
  [:noah.core/table :noah.core/table :noah.core/fn-2]
  [this a b]
  (.leftJoin this a (fn-wrap/value-joiner b)))
(defmethod
  filter-not
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.filterNot this (fn-wrap/predicate a)))
(defmethod
  filter-not
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.filterNot this (fn-wrap/predicate a)))
(defmethod
  filter-not
  [:noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.filterNot this (fn-wrap/predicate a) b))
(defmethod
  map-values
  [:noah.core/stream :noah.core/fn-1]
  [this a]
  (.mapValues this (fn-wrap/value-mapper a)))
(defmethod
  map-values
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.mapValues this (fn-wrap/value-mapper-with-key a)))
(defmethod
  map-values
  [:noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.mapValues this (fn-wrap/value-mapper-with-key a) b))
(defmethod
  map-values
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.mapValues this (fn-wrap/value-mapper-with-key a)))
(defmethod
  map-values
  [:noah.core/table
   :noah.core/fn-1
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.mapValues this (fn-wrap/value-mapper a) b))
(defmethod
  map-values
  [:noah.core/table :noah.core/fn-1]
  [this a]
  (.mapValues this (fn-wrap/value-mapper a)))
(defmethod
  through
  [:noah.core/stream java.lang.String]
  [this a]
  (.through this a))
(defmethod
  through
  [:noah.core/stream java.lang.String :noah.core/produced]
  [this a b]
  (.through this a (produced b)))
(comment
  skipped
  process
  "(this nil (clojure.core/into-array java.lang.String (clojure.core/map clojure.core/identity vararg)))\n <- [[org.apache.kafka.streams.kstream.KStream this nil] [org.apache.kafka.streams.processor.ProcessorSupplier a nil] [java.lang.String b java.lang.String]]")
(defmethod to-stream [:noah.core/table] [this] (.toStream this))
(defmethod
  to-stream
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.toStream this (fn-wrap/key-value-mapper a)))
(comment
  skipped
  print
  "(this nil)\n <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.Printed a]]")
(defmethod
  outer-join
  [:noah.core/stream
   :noah.core/stream
   :noah.core/fn-2
   :noah.core/join-windows]
  [this a b c]
  (.outerJoin this a (fn-wrap/value-joiner b) c))
(defmethod
  outer-join
  [:noah.core/stream
   :noah.core/stream
   :noah.core/fn-2
   :noah.core/join-windows
   org.apache.kafka.streams.kstream.Joined]
  [this a b c d]
  (.outerJoin this a (fn-wrap/value-joiner b) c d))
(defmethod
  outer-join
  [:noah.core/table :noah.core/table :noah.core/fn-2]
  [this a b]
  (.outerJoin this a (fn-wrap/value-joiner b)))
(defmethod
  outer-join
  [:noah.core/table
   :noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.outerJoin this a (fn-wrap/value-joiner b) c))
(defmethod
  merge
  [:noah.core/stream :noah.core/stream]
  [this a]
  (.merge this a))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder java.util.regex.Pattern]
  [this a]
  (.stream this a))
(comment
  skipped
  stream
  "(this nil (noah.core/consumed b))\n <- [[org.apache.kafka.streams.StreamsBuilder this] [java.util.Collection a] [org.apache.kafka.streams.kstream.Consumed b]]")
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder java.lang.String]
  [this a]
  (.stream this a))
(comment
  skipped
  stream
  "(this nil)\n <- [[org.apache.kafka.streams.StreamsBuilder this] [java.util.Collection a]]")
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed]
  [this a b]
  (.stream this a (consumed b)))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder
   java.util.regex.Pattern
   :noah.core/consumed]
  [this a b]
  (.stream this a (consumed b)))
(defmethod
  flat-map
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.flatMap this (fn-wrap/key-value-mapper a)))
(defmethod
  global-table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed]
  [this a b]
  (.globalTable this a (consumed b)))
(defmethod
  global-table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.globalTable this a (consumed b) c))
(defmethod
  global-table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.globalTable this a b))
(defmethod
  global-table
  [org.apache.kafka.streams.StreamsBuilder java.lang.String]
  [this a]
  (.globalTable this a))
(defmethod
  transform-values
  [:noah.core/stream
   org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
   java.lang.String]
  [this a vararg]
  (.transformValues
   this
   a
   (into-array
    java.lang.String
    (clojure.core/map identity vararg))))
(defmethod
  transform-values
  [:noah.core/stream
   org.apache.kafka.streams.kstream.ValueTransformerSupplier
   java.lang.String]
  [this a vararg]
  (.transformValues
   this
   a
   (into-array
    java.lang.String
    (clojure.core/map identity vararg))))
(defmethod
  transform-values
  [:noah.core/table
   org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
   java.lang.String]
  [this a vararg]
  (.transformValues
   this
   a
   (into-array
    java.lang.String
    (clojure.core/map identity vararg))))
(defmethod
  transform-values
  [:noah.core/table
   org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
   org.apache.kafka.streams.kstream.Materialized
   java.lang.String]
  [this a b vararg]
  (.transformValues
   this
   a
   b
   (into-array
    java.lang.String
    (clojure.core/map identity vararg))))
(defmethod
  build
  [org.apache.kafka.streams.StreamsBuilder]
  [this]
  (.build this))
(defmethod
  filter
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.filter this (fn-wrap/predicate a)))
(defmethod
  filter
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.filter this (fn-wrap/predicate a)))
(defmethod
  filter
  [:noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.filter this (fn-wrap/predicate a) b))
(comment
  skipped
  foreach
  "(this nil)\n <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.ForeachAction a]]")
(defmethod
  count
  [:noah.core/stream org.apache.kafka.streams.kstream.Materialized]
  [this a]
  (.count this a))
(defmethod count [:noah.core/stream] [this] (.count this))
(defmethod
  count
  [:noah.core/table org.apache.kafka.streams.kstream.Materialized]
  [this a]
  (.count this a))
(defmethod count [:noah.core/table] [this] (.count this))
(defmethod
  count
  [:noah.core/stream org.apache.kafka.streams.kstream.Materialized]
  [this a]
  (.count this a))
(defmethod count [:noah.core/stream] [this] (.count this))
(defmethod
  group-by
  [:noah.core/stream :noah.core/fn-2 :noah.core/serialized]
  [this a b]
  (.groupBy this (fn-wrap/key-value-mapper a) (serialized b)))
(defmethod
  group-by
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.groupBy this (fn-wrap/key-value-mapper a)))
(defmethod
  group-by
  [:noah.core/table :noah.core/fn-2 :noah.core/serialized]
  [this a b]
  (.groupBy this (fn-wrap/key-value-mapper a) (serialized b)))
(defmethod
  group-by
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.groupBy this (fn-wrap/key-value-mapper a)))
(defmethod
  select-key
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.selectKey this (fn-wrap/key-value-mapper a)))
(comment
  skipped
  addGlobalStore
  "(this nil b (noah.core/consumed c) nil)\n <- [[org.apache.kafka.streams.StreamsBuilder this] [org.apache.kafka.streams.state.StoreBuilder a] [java.lang.String b] [org.apache.kafka.streams.kstream.Consumed c] [org.apache.kafka.streams.processor.ProcessorSupplier d]]")
(comment
  skipped
  addGlobalStore
  "(this nil b c (noah.core/consumed d) e nil)\n <- [[org.apache.kafka.streams.StreamsBuilder this] [org.apache.kafka.streams.state.StoreBuilder a] [java.lang.String b] [java.lang.String c] [org.apache.kafka.streams.kstream.Consumed d] [java.lang.String e] [org.apache.kafka.streams.processor.ProcessorSupplier f]]")
