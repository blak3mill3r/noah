(ns noah.map-wrap
  (:require
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

(defn serdes [o]
  (get {:string (Serdes/String)
        :long   (Serdes/Long)
        :bytes  (Serdes/Bytes)
        :short  (Serdes/Short)
        :int    (Serdes/Integer)
        :double (Serdes/Double)
        :float  (Serdes/Float)
        :edn    (EdnSerde.)}
       o o))

;; TODO macro these up
;; too much repetition

(defn consumed [o]
  (cond
    (instance? Consumed o) o

    (map? o)
    (let [{:keys [consumed/key consumed/value consumed/k consumed/v]} o]
      (Consumed/with (serdes (or k key))
                     (serdes (or v value))))
    
    true
    (throw (ex-info ~(str "Need a map or an instance of Consumed") {:got o}))))

(defn produced [o]
  (cond
    (instance? Produced o) o

    (map? o)
    (let [{:keys [produced/key produced/value produced/k produced/v]} o]
      (Produced/with (serdes (or k key))
                     (serdes (or v value))))
    
    true
    (throw (ex-info ~(str "Need a map or an instance of Produced") {:got o}))))

(defn serialized [o]
  (cond
    (instance? Serialized o) o

    (map? o)
    (let [{:keys [serialized/key serialized/value serialized/k serialized/v]} o]
      (Serialized/with (serdes (or k key))
                       (serdes (or v value))))

    true
    (throw (ex-info ~(str "Need a map or an instance of Serialized") {:got o}))))
