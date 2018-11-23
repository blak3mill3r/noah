(ns noah.fn-wrap
  "There are many types in Kafka Streams which are basically just function application from a Clojure perspective.

  This namespace defines unary helper fns for each of those types, which take a fn and wrap it as an instance of that type.

  Also, these helpers will pass through an instance of the type unchanged; this means you can use noah with Kafka Streams Java code."
  (:require [camel-snake-kebab.core :refer [->kebab-case]]
            [clojure.reflect :as ref])
  (:import [org.apache.kafka.streams.kstream Merger ValueJoiner KeyValueMapper ValueMapper Predicate Reducer Initializer Aggregator ValueMapperWithKey]
           [org.apache.kafka.streams.processor TopicNameExtractor]))

(defmacro defconverter
  "Expands into the definition of a converter fn which will return an instance of `type` (a symbol representing a java class).
  This type should be one which has an interface that looks like function application (of a specific arity, being a java interface).
  If passed a clojure fn, it wraps it in this function container type using `proxy`. Instances of the type will be passed through unchanged."
  [type n-args & [ apply-fn return-fn ]]
  (let [arg-syms (map (comp symbol str) (take n-args "abcdefg"))
        apply-fn (or apply-fn 'apply)]
    `(defn ~(->kebab-case type) [~'f]
       (cond
         (fn? ~'f) (proxy [~type] [] (~apply-fn [~@arg-syms]
                                      ~(if return-fn
                                         `(~return-fn (~'f ~@arg-syms))
                                         `(~'f ~@arg-syms))))
         (isa? (type ~'f) ~type) ~'f
         true (throw (ex-info ~(str "Need a function or an instance of "(.getName (resolve type))) {:got ~'f}))))))

(defconverter ValueJoiner 2)
(defconverter ValueMapper 1)
(defconverter KeyValueMapper 2)
(defconverter ValueMapperWithKey 2)
(defconverter Initializer 0)
(defconverter Aggregator 3)
(defconverter Reducer 2)
(defconverter Merger 3)
(defconverter Predicate 2 test boolean)
(defconverter TopicNameExtractor 3 extract str)

;; https://stackoverflow.com/questions/1696693/clojure-how-to-find-out-the-arity-of-function-at-runtime
(defn arity
  "Returns the maximum arity of:
    - anonymous functions like `#()` and `(fn [])`.
    - defined functions like `map` or `+`.
    - macros, by passing a var like `#'->`.

  Returns `:variadic` if the function/macro is variadic."
  [f]
  (let [func (if (var? f) @f f)
        methods (->> func class .getDeclaredMethods
                     (map #(vector (.getName %)
                                   (count (.getParameterTypes %)))))
        var-args? (some #(-> % first #{"getRequiredArity"})
                        methods)]
    (if var-args?
      :variadic
      (let [max-arity (->> methods
                           (filter (comp #{"invoke"} first))
                           (sort-by second)
                           last
                           second)]
        (if (and (var? f) (-> f meta :macro))
          (- max-arity 2) ;; substract implicit &form and &env arguments
          max-arity)))))

(comment
  (ref/reflect org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier)
  (ref/reflect org.apache.kafka.streams.kstream.ValueTransformerWithKey)
  (ref/reflect org.apache.kafka.streams.kstream.ValueTransformer)
  (ref/reflect org.apache.kafka.streams.kstream.Transformer)

  (defn transformer
    [f]
    (reify
      TransformerSupplier
      (get [_] (transformer xform)))
    

    )
  )
