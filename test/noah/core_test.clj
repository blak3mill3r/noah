(ns noah.core-test
  (:require
   [clojure.test :as t :refer [deftest is]]
   [noah.core :as sut]
   [noah.test-utils :as tu :refer [topology-test-driver record-factory *driver* *produce* output-topic-seq]]
   [clojure.string :as str])
  (:import
   [org.apache.kafka.common.serialization Serdes]))

(defn topology []
  (let [b (sut/streams-builder)
        text (-> b (sut/stream "text" #:serdes{:k :string :v :string}))]
    (-> text
        (sut/flat-map-values #(str/split % #"\s+"))
        (sut/group-by #(-> %2))
        sut/count
        sut/to-stream
        (sut/to "word-counts" #:serdes{:k :string :v :long}))
    (sut/build b)))

(t/use-fixtures :once
  (tu/topology-fixture
   (topology)
   {"bootstrap.servers"   "localhost:9091"
    "application.id"      "noah-test"
    "default.key.serde"   (.getName (.getClass (sut/serdes :string)))
    "default.value.serde" (.getName (.getClass (sut/serdes :string)))}))

(deftest can-count
  (*produce* "text" :string :string nil "some word some more word")
  (doseq [[a b] (map vector
                     (output-topic-seq "word-counts" :string :long)
                     [["some" 1] ["word" 1] ["some" 2] ["more" 1] ["word" 2]])]
    (is (= a b))))
