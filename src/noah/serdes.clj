(ns noah.serdes
  (:require
   [camel-snake-kebab.core :refer [->kebab-case]]
   [franzy.serialization.nippy.serializers :refer [nippy-serializer]]
   [franzy.serialization.nippy.deserializers :refer [nippy-deserializer]])
  (:import
   [org.apache.kafka.common.serialization Serdes Serde]
   [org.apache.kafka.streams.kstream Consumed Produced Serialized]))

(def ^:private edn-serializer (nippy-serializer))
(def ^:private edn-deserializer (nippy-deserializer))

(deftype EdnSerde []
  Serde
  (configure [this map b])
  (close [this])
  (serializer [this] edn-serializer)
  (deserializer [this] edn-deserializer))

;; `serdes` is a multimethod in order to be open to extension
(defmulti serdes identity)
(defmethod serdes :default [o] o) ;; allow using a plain Serdes instance instead of a keyword
(defmethod serdes :string       [_] (Serdes/String))
(defmethod serdes :long         [_] (Serdes/Long))
(defmethod serdes :bytes        [_] (Serdes/Bytes))
(defmethod serdes :byte-buffer  [_] (Serdes/ByteBuffer))
(defmethod serdes :byte-array   [_] (Serdes/ByteArray))
(defmethod serdes :short        [_] (Serdes/Short))
(defmethod serdes :int          [_] (Serdes/Integer))
(defmethod serdes :double       [_] (Serdes/Double))
(defmethod serdes :float        [_] (Serdes/Float))
(defmethod serdes :edn          [_] (EdnSerde.))

(defmacro defserdeswrapper [class-sym wrap-method]
  (let [fn-name (->kebab-case class-sym)]
    `(defn ~fn-name [~'o]
       (cond (instance? ~class-sym ~'o) ~'o
             (map? ~'o) (let [{:keys [serdes/key serdes/value serdes/k serdes/v]} ~'o]
                          (.. ~class-sym (~wrap-method
                                          (serdes (or ~'k ~'key))
                                          (serdes (or ~'v ~'value)))))
             true (throw (ex-info ~(str "Need a serdes map or an instance of " (.getName (ns-resolve *ns* class-sym))) {:got ~'o})))) ))

;; several trivially different Java types for passing Serdes around...
(defserdeswrapper Consumed with)
(defserdeswrapper Produced with)
(defserdeswrapper Serialized with)
