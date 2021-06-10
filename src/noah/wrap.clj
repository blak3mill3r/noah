(ns noah.wrap
  "Hand-written wrappers."
  (:refer-clojure :exclude [transduce])
  (:require [noah.transduce])
  (:import
   [org.apache.kafka.streams KafkaStreams StreamsBuilder StreamsConfig KeyValue]
   [org.apache.kafka.streams.kstream Materialized]))

(defn kafka-streams
  [topology config]
  (KafkaStreams. topology (StreamsConfig. config)))

(defn streams-builder
  []
  (StreamsBuilder.))

(defn kv
  [k v]
  (KeyValue/pair k v))

;; working around cyclic dependency
;; I want this to live here but use transform from core, which is generated
(defn transduce
  ([kstream xform]
   (@(ns-resolve 'noah.core 'transform)  kstream (noah.transduce/transducing-transformer xform) []))
  ([kstream xform state-store-name]
   (@(ns-resolve 'noah.core 'transform)  kstream (noah.transduce/transducing-transformer xform state-store-name) [(name state-store-name)])))
