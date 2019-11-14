(ns noah.transduce-test
  (:require [noah.transduce :as sut]
            [net.cgrand.xforms :as x]
            [clojure.test :as t :refer [deftest is]]
            [noah.core :as n]
            [noah.test-utils :as tu :refer [topology-test-driver record-factory *driver* *topic* advance-time output-topic-seq]]
            [clojure.core.async :as a]
            [coddled-super-centaurs.core :as csc])
  (:import [org.apache.kafka.streams.state Stores]
           [org.apache.kafka.streams.processor ProcessorContext]))

;;; Generalizing this idea would be good
;; generate transducers as random code with certain predicates and modifier functions, say on ints
;; generate them in pairs, one noah and one plain clojure
;; make a channel per key for a set of keys
;; each key channel gets the same random transducer
;; generate kvs and feed them to the TopologyTestDriver (or to a real cluster w monkey pulling plugs), and to the core.async channels

;; a suitably shwifty vanilla clojure stateful transducer
;; what we really ought to do is generate these, and test that the noah version matches the in-memory one
(def plain-xform
  (comp (map inc)
        (partition-by #(or (zero? (mod % 11))
                           (zero? (mod % 7))))
        (filter #(not= 1 (count %)))
        (map #(apply + %))
        (partition-all 4)
        (interpose :blah)))

;; the same, as a noah stateful transducer
(def noah-xform
  (comp (map inc)
        (csc/partition-by #(or (zero? (mod % 11))
                               (zero? (mod % 7))))
        (filter #(not= 1 (count %)))
        (map #(apply + %))
        (csc/partition-all 4)
        (csc/interpose :blah)))

;; since core.async will not call arity-1 of the channel transducer if the channel remains open
;; they come in handy for this test suite, in which one goal is to confirm that the noah transducers
;; agree with their in-memory clojure analogues...
;; this helper exploits that fact; if you just used (into [] xform coll) to compare against,
;; then when coll runs out of elements, `into` will produce completion values using arity-1
;; whereas the noah transducers will never do that, on accounta KStreams being naturally unbounded
(defn- into-nc
  "more or less a version of clojure.core/into but never calling the arity-1 of xform"
  [to xform from]
  (let [c (a/chan 8 xform)]
    (a/<!! (a/onto-chan c from false))
    (into [] (take-while identity (repeatedly #(a/poll! c))))))

(defn topology []
  (let [b (n/streams-builder)
        _ (n/add-state-store b (Stores/keyValueStoreBuilder
                                (Stores/persistentKeyValueStore "somestate")
                                (n/serdes :edn)
                                (n/serdes :edn)))
        nums (-> b (n/stream "nums"))]
    (-> nums
        (n/transduce noah-xform "somestate")
        (n/to "transduced-nums"))
    (n/build b)))

(t/use-fixtures :once
  (tu/topology-fixture
   (topology)
   {"bootstrap.servers"   "localhost:9091"
    "application.id"      "noah-test"
    "default.key.serde"   (.getName (.getClass (n/serdes :edn)))
    "default.value.serde" (.getName (.getClass (n/serdes :edn)))}))

(deftest transduces-a-kstream-of-numbers
  (let [->nums (*topic* "nums" :edn :edn)]
    (let [foo-in (range 1 63)
          bar-in (range 64 127)
          expect-foo (into-nc [] plain-xform (range 1 63))
          expect-bar (into-nc [] plain-xform bar-in)]
      (doseq [n foo-in] (->nums "foo" n))
      (doseq [n bar-in] (->nums "bar" n))
      (let [output (x/into {}
                           (comp (take 20)
                                 (x/by-key first last (x/into [])))
                           (output-topic-seq "transduced-nums" :edn :edn))]
        (is (= expect-foo (get output "foo")))
        (is (= expect-bar (get output "bar")))))))
