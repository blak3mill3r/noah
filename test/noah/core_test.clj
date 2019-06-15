(ns noah.core-test
  "The obligatory word-count"
  (:require
   [clojure.test :as t :refer [deftest is]]
   [noah.core :as n]
   [noah.test-utils :as tu :refer [topology-test-driver record-factory *topic* output-topic-seq]]
   [clojure.string :as str]
   [noah.fn-wrap :as fn-wrap]))

(defn topology []
  (let [b (n/streams-builder)
        text (-> b (n/stream "text"))]
    (-> text
        (n/flat-map-values #(str/split % #"\s+"))
        (n/group-by #(-> %2))
        n/count
        n/to-stream
        (n/to "word-counts" {::n/value-serde :long}))
    (n/build b)))

(t/use-fixtures :once
  (tu/topology-fixture
   (topology)
   {"bootstrap.servers"   "localhost:9091"
    "application.id"      "noah-test"
    "default.key.serde"   (.getName (.getClass (n/serdes :string)))
    "default.value.serde" (.getName (.getClass (n/serdes :string)))}))

(deftest can-count
  (let [->text (*topic* "text" :string :string)]
    (->text nil "it's like going to like count these like words")
    (doseq [[a b] (map vector
                       (output-topic-seq "word-counts" :string :long)
                       [["it's"  1]
                        ["like"  1]
                        ["going" 1]
                        ["to"    1]
                        ["like"  2]
                        ["count" 1]
                        ["these" 1]
                        ["like"  3]
                        ["words" 1]])]
      (is (= a b)))))
