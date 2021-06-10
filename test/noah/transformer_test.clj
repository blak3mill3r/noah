(ns noah.transformer-test
  (:require [noah.transformer :as sut]
            [clojure.test :as t :refer [deftest is]]
            [noah.core :as n]
            [noah.serdes :as serdes]
            [noah.test-utils :as tu :refer [topology-test-driver record-factory *driver* *topic* advance-time output-topic-seq]]
            [clojure.string :as str]
            [noah.fn-wrap :as fn-wrap])
  (:import [org.apache.kafka.streams.state Stores]))

;; A transformer which expires records not seen for 20 min
;; it tracks latest timestamps for each key,
;; and every 20 minutes, it checks all keys in the state store and emits tombstones for expired ones
(sut/deftransformer expiring-transformer
  #_[mystore #::serdes{:k :string :v :long}] ;; <-- would be good perhaps to put the serdes in this record
  [mystore]
  :schedule (java.time.Duration/ofMinutes 20) ::n/stream-time
  (fn expiration-scan [ts]
    (let [cutoff (- ts (* 1000 60 20))]
      (doseq [r (iterator-seq (.all mystore))]
        (when (and (.value r) (< (.value r) cutoff))
          (.forward (sut/context) (.key r) nil)))))
  ;;:init  (fn my-init [ctx] )
  ;;:close (fn my-close [ctx stores] )
  [k v]
  (if (nil? v)
    (.delete mystore k)
    (do (.put mystore k (.timestamp (sut/context)))
        (.forward (sut/context) k v))))

(defn topology []
  (let [b (n/streams-builder)
        sb (Stores/keyValueStoreBuilder ;; ^ instead of having to declare it when building the topology like this
            (Stores/persistentKeyValueStore "mystore")
            (n/serdes :string)
            (n/serdes :long))
        _ (n/add-state-store b sb)
        text (-> b (n/stream "text"))]
    (-> text
        (n/flat-map-values #(str/split % #"\s+"))
        (n/group-by #(-> %2))
        n/count
        n/to-stream
        (n/transform expiring-transformer ["mystore"])
        (n/to "word-counts" {::n/value-serde :long}))
    (n/build b)))

(t/use-fixtures :once
  (tu/topology-fixture
   (topology)
   {"bootstrap.servers"   "localhost:9091"
    "application.id"      "noah-test"
    "default.key.serde"   (.getName (.getClass (n/serdes :string)))
    "default.value.serde" (.getName (.getClass (n/serdes :string)))}))

(deftest expires-records-older-than-twenty-minutes
  (let [->text (*topic* "text" :string :string)]
    (->text nil "some") (advance-time ->text (* 1000 60 21))
    (->text nil "more")
    (let [output (map vector
                      (output-topic-seq "word-counts" :string :long)
                      [["some" 1] ["more" 1]
                       ;; then a tombstone for `some` which expired
                       ["some" nil]
                       ;; and no more tombstones because `more` did not expire
                       nil])]
      (doseq [[result expected] output]
        (when expected (is (= expected result)))))))
