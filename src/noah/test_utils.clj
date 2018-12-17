(ns noah.test-utils
  (:require [noah.core :refer [ serdes ]])
  (:import
   [org.apache.kafka.streams TopologyTestDriver StreamsConfig ]
   [org.apache.kafka.streams.test ConsumerRecordFactory]
   [org.apache.kafka.common.serialization Serdes]
   ))

(def ^:dynamic *driver* nil)
(def ^:dynamic *produce* nil)

;; FIXME make it like    :string :edn :long
(defn record-factory [key-ser val-ser] (ConsumerRecordFactory. key-ser val-ser))

(defn topology-test-driver [topology props]
  (TopologyTestDriver. topology
                       (-> (fn [p [k v]] (do (.setProperty p k v) p))
                           (reduce (java.util.Properties.) props))))

(defn output-topic-seq
  [topic k-serde v-serde]
  (lazy-seq (cons (when-let [record (.readOutput *driver* topic
                                                 (.deserializer (serdes k-serde))
                                                 (.deserializer (serdes v-serde)))]
                    ((juxt (memfn key) (memfn value)) record))
                  (output-topic-seq topic k-serde v-serde))))

(defn topology-fixture
  [topology properties]
  (fn [test-fn]
    (let [driver (topology-test-driver topology properties)
          rf (record-factory (.. Serdes String serializer) (.. Serdes String serializer))]
      (binding [*driver* driver *produce* (fn [topic k v] (->> (.create rf topic k v) (.pipeInput driver)))]
        (try
          (test-fn)
          ;; when completed, make sure your tests close() the driver to release all resources and processors.
          (catch Throwable e (println "Caught: " e " in test, closing driver"))
          (finally (.close driver)))))))
