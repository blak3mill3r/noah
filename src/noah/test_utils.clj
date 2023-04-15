(ns noah.test-utils
  (:require [noah.core :refer [ serdes ]])
  (:import
   [org.apache.kafka.streams TopologyTestDriver StreamsConfig ]
   [org.apache.kafka.common.serialization Serdes]
   [java.time Duration]
   ))

(def ^:dynamic *driver* nil)
(def ^:dynamic *produce* nil)
(def ^:dynamic *topic* nil)
(def ^:dynamic *store* nil)

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

(deftype MockTopic [driver topic k-ser v-ser test-input-topic]
  clojure.lang.IFn
  (invoke [this k v]
    (.pipeInput test-input-topic k v)))

(defn advance-wall-clock-time
  [ms]
  (when-not *driver* (throw (ex-info "Must be called inside a topology test." {})))
  (.advanceWallClockTime *driver* (Duration/ofMillis ms)))

(defn mock-topic [driver topic k-ser v-ser]
  (->MockTopic driver topic k-ser v-ser
               (.createInputTopic driver topic
                                  (.serializer (serdes k-ser))
                                  (.serializer (serdes v-ser)))))

(defn topology-fixture
  [topology properties]
  (fn [test-fn]
    (let [driver (topology-test-driver topology properties)]
      (binding [*driver* driver
                *store* (fn [name] (.getStateStore driver name))
                *topic* (fn [topic k-ser v-ser] (mock-topic driver topic k-ser v-ser))]
        (try
          (test-fn)
          ;; when completed, make sure your tests close() the driver to release all resources and processors.
          (catch Throwable e (println "Caught: " e " in test, closing driver"))
          (finally (.close driver)))))))

;; https://github.com/ztellman/potemkin#def-map-type
;; should be possible to def-map-type KeyValueStore
;; except for the range() thing...
;; and, what about implementing keys for def-map-type?

;; KeyValueStore store = testDriver.getKeyValueStore("store-name");
;; assertEquals("some value", store.get("some key"));
