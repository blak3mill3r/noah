(ns noah.examples.word-count
  (:require [noah.core :as n]
            [clojure.string :as str])
  (:import
   [org.apache.kafka.common.serialization Serdes]
   [org.apache.kafka.streams.kstream Consumed Produced]))

(defn kafka-streams []
  (n/with-builder
    (-> n/*builder*
        (n/stream "noah-plaintext-input" (Consumed/with (Serdes/String) (Serdes/String)))
        (n/flat-map-values (fn [string] (str/split string #"\s+")))
        (n/group-by (fn [_ word] word))
        n/count
        n/to-stream
        (n/to "noah-wordcount-output" (Produced/with (Serdes/String) (Serdes/Long))))
    (n/kafka-streams
     n/*builder*
     {"application.id"      "noah-word-count-example"
      "bootstrap.servers"   "localhost:9092"
      "default.key.serde"   org.apache.kafka.common.serialization.Serdes$StringSerde
      "default.value.serde" org.apache.kafka.common.serialization.Serdes$StringSerde
      "commit.interval.ms"  "5000"
      ;; "state.dir"           "somewherez passed on cmd-line"
      })))

(defonce my-streams (atom (kafka-streams)))
#_(reset! my-streams (kafka-streams))

#_(.start @my-streams)
#_(.close @my-streams)

;; in two terminals:

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; kafka-console-producer --topic noah-plaintext-input --broker-list localhost:9092 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; kafka-console-consumer --topic noah-wordcount-output     \
;;   --from-beginning                                       \
;;   --bootstrap-server localhost:9092                      \
;;   --property print.key=true                              \
;;   --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
