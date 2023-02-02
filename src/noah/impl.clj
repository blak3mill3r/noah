(ns noah.impl
  "Gross namespace with funkadelic macro to make wrappers."
  (:require
   [camel-snake-kebab.core :refer [->kebab-case]]
   [noah.fn-wrap :as fn-wrap]
   [noah.transformer]
   [noah.javanation :refer [conversion-fn]]
   [clojure.reflect :as ref]
   [clojure.string :as str])
  (:import
   [org.apache.kafka.clients.consumer ConsumerConfig]
   [org.apache.kafka.common.serialization Serdes Serde]
   [org.apache.kafka.common.utils Bytes]
   [org.apache.kafka.streams KafkaStreams StreamsBuilder StreamsConfig KeyValue]
   [org.apache.kafka.streams.kstream Aggregator Consumed GlobalKTable Initializer Joined JoinWindows KeyValueMapper ValueMapperWithKey KGroupedStream KGroupedTable KStream KTable Materialized Merger Predicate Produced Reducer Grouped SessionWindowedKStream SessionWindows ValueJoiner ValueJoinerWithKey ValueMapper Windows TimeWindowedKStream TransformerSupplier Transformer ValueTransformerWithKeySupplier ValueTransformerWithKey ValueTransformerSupplier ValueTransformer ForeachAction]
   [org.apache.kafka.streams.kstream.internals KTableImpl KStreamImpl KGroupedStreamImpl]
   [org.apache.kafka.streams.state KeyValueStore StoreBuilder]
   [org.apache.kafka.streams.processor TopicNameExtractor]
   [java.util Collections Map Properties]
   [noah.transformer NoahTransformer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; * Implementation of `(defwrappers)` macro
;; first we define a hierarchy for the wrapper multimethod dispatch
;; this means you can pass either a ValueJoiner or a Clojure function
;; multimethod dispatch will take care of finding the matching signature on the
;; right target class

(derive clojure.lang.IPersistentMap :noah.core/consumed)   (derive Consumed :noah.core/consumed)
(derive clojure.lang.IPersistentMap :noah.core/produced)   (derive Produced :noah.core/produced)
(derive clojure.lang.IPersistentMap :noah.core/grouped)    (derive Grouped  :noah.core/grouped)
(derive Initializer            :noah.core/fn-0)
(derive ValueMapper            :noah.core/fn-1)
(derive ValueMapperWithKey     :noah.core/fn-2)
(derive KeyValueMapper         :noah.core/fn-2)
(derive ValueJoiner            :noah.core/fn-2)
(derive ValueJoinerWithKey     :noah.core/fn-3)
(derive Reducer                :noah.core/fn-2)
(derive Predicate              :noah.core/fn-2)
(derive ForeachAction          :noah.core/fn-2)
(derive Aggregator             :noah.core/fn-3)
(derive Merger                 :noah.core/fn-3)
(derive TopicNameExtractor     :noah.core/fn-3)
(derive KStream                :noah.core/stream)
(derive KStreamImpl            :noah.core/stream)
(derive KGroupedStream         :noah.core/stream)
(derive SessionWindowedKStream :noah.core/stream)
(derive TimeWindowedKStream    :noah.core/stream)
(derive KTable                 :noah.core/table)
(derive KTableImpl             :noah.core/table) ;; can automate using java reflection? ... why was this even necessary?
(derive KGroupedTable          :noah.core/table)
(derive GlobalKTable           :noah.core/global-table)
(derive JoinWindows            :noah.core/join-windows)
(derive Windows                :noah.core/windows)
(derive NoahTransformer        :noah.core/transformer)
(derive Transformer            :noah.core/transformer)

;;; Implementation details

(defn type-with-fn-arity [o]
  (if (fn? o)
    (or (try (nth [:noah.core/fn-0 :noah.core/fn-1 :noah.core/fn-2 :noah.core/fn-3] (fn-wrap/arity o)) (catch Exception _ nil))
        (throw (IllegalArgumentException. "You can only pass noah functions of arity 0,1,2,3.")))
    (type o)))

;; this is the multimethod dispatch fn: a vector of the types of the arguments
(defn types-vector [& args] (into [] (map type-with-fn-arity) args))

;; for Java varargs methods we need to get the type from inside the collection (the last clojure param) and dispatch based on that
(defn types-vector-varargs [& args] (into []
                                          (map type-with-fn-arity)
                                          (concat (drop-last args)
                                                  (list (first (last args))))))

(defn- remove-varargs [type-sym]
  (cond-> type-sym
    (str/ends-with? (str type-sym) "<>")
    (-> str (str/replace-first "<>" "") symbol)))

(defn- try-resolve [type]
  (->> type remove-varargs (ns-resolve 'noah.core)))

(defn- noah-parent [type]
  #_(when (nil? (first (for [t (ancestors type) :when (and (keyword? t) (= "noah.core" (namespace t)))] t)))
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
         (str "[" (str/join " " (into [(docstring-part declaring-class)] (map docstring-part) parameter-types)) "]"))
       set sort (str/join "\n")))

(defn- defwrapper-defmulti [name sigs]
  `(~'defmulti ~(->kebab-case name)
    ~(docstring-wrapper name sigs)
    ~(if (-> sigs first :flags :varargs)
       `noah.core/types-vector-varargs
       `noah.core/types-vector)))

(def ^:private arg-symbols (into [] (map (comp symbol str)) "abcdefg"))

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
      nil
      #_(println "Warn: no conversion for " type))))

(defn- defwrapper-defmethod [{:keys [name parameter-types flags return-type declaring-class] :as member-fn}]
  (let [arg-types-and-syms (into [[declaring-class 'this]] (map vector parameter-types arg-symbols))
        convert-params-code (generate-convert-params-code arg-types-and-syms)]

    ;; need a conversion-fn for every parameter
    (if (some nil? convert-params-code) `(comment skipped ~name ~(str (prn-str convert-params-code) " <- " arg-types-and-syms))
        `(~'defmethod
          ;; method name
          ~(->kebab-case name)
          ;; dispatch value
          [~@(for [[t _] arg-types-and-syms] (-> t try-resolve noah-parent (or t)))]
          ;; argument vector
          [~@(map second arg-types-and-syms)]
          ;; function body: a call to the Java method, forwarding converted Clojure args
          (~(symbol (str "." name)) ~@convert-params-code)))))

(defn- defwrapper-varargs [{:keys [name parameter-types flags return-type declaring-class] :as member-fn}]
  (let [vararg-type (-> parameter-types last remove-varargs)
        arg-types-and-syms-and-varargs
        (into [[declaring-class 'this nil]]
              (map vector
                   (concat (drop-last parameter-types)
                           [vararg-type])
                   arg-symbols
                   (concat (-> parameter-types count dec (repeat nil))
                           (list vararg-type))))
        convert-params-code (generate-convert-params-code arg-types-and-syms-and-varargs)]

    ;; need a conversion-fn for every parameter
    (if (some nil? convert-params-code) `(comment skipped ~name ~(str (prn-str convert-params-code) " <- " arg-types-and-syms-and-varargs))
        `(~'defmethod
          ;; method name
          ~(->kebab-case name)
          ;; dispatch value
          [~@(for [[t _] arg-types-and-syms-and-varargs] (-> t try-resolve noah-parent (or t)))]
          ;; argument vector
          [~@(map second (drop-last arg-types-and-syms-and-varargs))
           ~'vararg]
          ;; function body: a call to the Java method, forwarding converted Clojure args
          (~(symbol (str "." name)) ~@convert-params-code)))))

(defn- reflected-methods-by-name
  []
  (->> (for [klass [KStream KGroupedStream KTable KGroupedTable SessionWindowedKStream TimeWindowedKStream StreamsBuilder]
             m (:members (ref/reflect klass))
             :when (and (-> m :flags :public)
                        (not (instance? clojure.reflect.Constructor m)))] m)
       (group-by :name)))

(defn is-varargs? [type-symbol]
  (-> type-symbol name
      (str/ends-with? "<>")))

(defmacro defwrappers
  []
  (let [wrapper-input (reflected-methods-by-name)]
    `(do ~@(for [[name sigs] wrapper-input]        (defwrapper-defmulti name sigs))
         ~@(for [[name sigs] wrapper-input s sigs] (if-not (:varargs (:flags s))
                                                     (defwrapper-defmethod s)
                                                     (defwrapper-varargs s))))))

(comment
  (-> (->> (for [klass [KStream]
                 m (:members (ref/reflect klass))
                 :when (and (-> m :flags :public)
                            (not (instance? clojure.reflect.Constructor m))
                            ;; TODO support varargs
                            (-> m :flags :varargs))] m)
           (group-by :name))
      #_(get 'transform)
      #_(->> (into {}))
      #_(defwrapper-varargs))

  (defn- wrapper-kind [[name sigs]] (if (= 1 (count sigs)) :wrapper-function :wrapper-multimethod))

  (-> (reflected-methods-by-name)
      (get 'aggregate)
      (->> (map (juxt :declaring-class :parameter-types))))

  (noah-parent Initializer)         ;; => :noah.core/fn-0
  (noah-parent TimeWindowedKStream) ;; => :noah.core/stream
  (noah-parent KStream)             ;; => :noah.core/stream
  (noah-parent ValueJoiner)         ;; => :noah.core/fn-2
  (noah-parent Aggregator)          ;; => :noah.core/fn-3
  (noah-parent Produced)            ;; => :noah.core/produced
  (noah-parent Consumed)            ;; => :noah.core/consumed
  (noah-parent clojure.lang.IPersistentMap) ;; => :noah.core/consumed ;; hmm... assumption violated?

  (noah-parent org.apache.kafka.streams.kstream.internals.KTableImpl) ;; => :noah.core/table

  ;; useless?
  (def ^:dynamic *builder* nil)
  (defmacro with-builder [& body]
    `(let [b# (streams-builder)]
       (binding [*builder* b#]
         ~@body))))

