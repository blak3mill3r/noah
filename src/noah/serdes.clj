(ns noah.serdes
  "There's a lot of room for improvement here. They don't take options, and I haven't thought it all through.
  You can always build a Serde yourself. `serdes` is a multimethod so it is open to extension."
  (:require
   [taoensso.nippy :as nippy]
   [clojure.data.json :as json])
  (:import
   [java.nio.charset StandardCharsets]
   [org.apache.kafka.common.serialization Serdes Serde Deserializer Serializer]))

(deftype NippyDeserializer [opts]
  Deserializer
  (configure [_ _ _])
  (deserialize [_ _ data] (nippy/thaw data opts))
  (close [_]))

(deftype NippySerializer [opts]
  Serializer
  (configure [_ _ _])
  (serialize [_ _ data] (nippy/freeze data opts))
  (close [_]))

(def the-nippy-serializer (NippySerializer. {:incl-metadata? false})) ;; TODO:(Blake) allow the user to configure this
(def the-nippy-deserializer (NippyDeserializer. {}))

(deftype NippySerde []
  Serde
  (configure [this map b])
  (close [this])
  (serializer [this] the-nippy-serializer)
  (deserializer [this] the-nippy-deserializer))

(deftype EdnSerializer []
  Serializer
  (close [o])
  (configure [this configs key?])
  (serialize [this topic data]
    (when data (-> (binding [*print-length* false
                             *print-level* false]
                     (prn-str data))
                   (.getBytes StandardCharsets/UTF_8)))))

(defrecord EdnDeserializer [opts]
  Deserializer
  (close [this])
  (configure [this configs key?])
  (deserialize [this topic data]
    (when data
      (->> (String. data StandardCharsets/UTF_8)
           (clojure.edn/read-string opts)))))

(def the-edn-serializer (EdnSerializer.))
(def the-edn-deserializer (EdnDeserializer. {}))

(deftype EdnSerde []
  Serde
  (configure [this map b])
  (close [this])
  (serializer [this] the-edn-serializer)
  (deserializer [this] the-edn-deserializer))

(deftype JsonSerializer []
  Serializer
  (close [o])
  (configure [this configs key?])
  (serialize [this topic data]
    (when data (-> (json/write-str data)
                   (.getBytes StandardCharsets/UTF_8)))))

(defrecord JsonDeserializer [opts]
  Deserializer
  (close [this])
  (configure [this configs key?])
  (deserialize [this topic data]
    (when data
      (-> (String. data StandardCharsets/UTF_8)
          (json/read-str opts)))))

(def the-json-serializer (JsonSerializer.))
(def the-json-deserializer (JsonDeserializer. {}))

(deftype JsonSerde []
  Serde
  (configure [this map b])
  (close [this])
  (serializer [this] the-json-serializer)
  (deserializer [this] the-json-deserializer))

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
(defmethod serdes :nippy        [_] (NippySerde.))
(defmethod serdes :edn          [_] (EdnSerde.))
(defmethod serdes :json         [_] (JsonSerde.))
