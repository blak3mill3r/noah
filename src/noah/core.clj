(ns noah.core
  "Provides an interface to the high-level Streams API. The vast majority of functions here are generated via reflection."
  (:refer-clojure
   :rename {filter    core-filter
            map       core-map
            reduce    core-reduce
            count     core-count
            group-by  core-group-by
            peek      core-peek
            print     core-print
            merge     core-merge
            transduce core-transduce})
  (:require [noah.impl :refer [defwrappers]]
            [noah.wrap]
            [noah.serdes]
            [noah.transformer]
            [noah.map-wrap :as map-wrap]
            [noah.fn-wrap :as fn-wrap]
            [potemkin])
  (:import [org.apache.kafka.clients.consumer ConsumerConfig]
           [org.apache.kafka.common.serialization Serdes Serde]
           [org.apache.kafka.common.utils Bytes]
           [org.apache.kafka.streams KafkaStreams StreamsBuilder StreamsConfig KeyValue]
           [org.apache.kafka.streams.kstream Aggregator Consumed GlobalKTable Initializer Joined JoinWindows KeyValueMapper ValueMapperWithKey KGroupedStream KGroupedTable KStream KTable Materialized Merger Predicate Produced Reducer Grouped SessionWindowedKStream SessionWindows ValueJoiner ValueJoinerWithKey ValueMapper Windows TimeWindowedKStream TransformerSupplier Transformer ValueTransformerWithKeySupplier ValueTransformerWithKey ValueTransformerSupplier ValueTransformer Named]
           [org.apache.kafka.streams.kstream.internals KTableImpl KStreamImpl KGroupedStreamImpl]
           [org.apache.kafka.streams.state KeyValueStore]
           [org.apache.kafka.streams.processor TopicNameExtractor]
           [java.util Collections Map Properties]))

(potemkin/import-vars [noah.serdes serdes])
(potemkin/import-vars [noah.map-wrap consumed produced grouped materialized map->properties])
(potemkin/import-vars [noah.wrap kafka-streams streams-builder kv transduce])
(potemkin/import-vars [noah.impl types-vector types-vector-varargs])
(potemkin/import-vars [noah.transformer context])

;; the macro expansion of this produces the remainder of this source file
;; which is included inline for developer convenience
#_(defwrappers)
#_(macroexpand-1 '(defwrappers))

(defmulti
  reduce
  "[stream fn-2 Materialized]
[stream fn-2 Named Materialized]
[stream fn-2 Named]
[stream fn-2]
[table fn-2 fn-2 Materialized]
[table fn-2 fn-2 Named Materialized]
[table fn-2 fn-2]"
  noah.core/types-vector)
(defmulti
  windowed-by
  "[stream SessionWindows]
[stream SlidingWindows]
[stream windows]"
  noah.core/types-vector)
(defmulti
  aggregate
  "[stream fn-0 fn-3 Materialized]
[stream fn-0 fn-3 Named Materialized]
[stream fn-0 fn-3 Named]
[stream fn-0 fn-3 fn-3 Materialized]
[stream fn-0 fn-3 fn-3 Named Materialized]
[stream fn-0 fn-3 fn-3 Named]
[stream fn-0 fn-3 fn-3]
[stream fn-0 fn-3]
[table fn-0 fn-3 fn-3 Materialized]
[table fn-0 fn-3 fn-3 Named Materialized]
[table fn-0 fn-3 fn-3 Named]
[table fn-0 fn-3 fn-3]"
  noah.core/types-vector)
(defmulti
  peek
  "[stream fn-2 Named]
[stream fn-2]"
  noah.core/types-vector)
(defmulti
  branch
  "[stream Named fn-2]
[stream fn-2]"
  noah.core/types-vector-varargs)
(defmulti
  repartition
  "[stream Repartitioned]
[stream]"
  noah.core/types-vector)
(defmulti
  map
  "[stream fn-2 Named]
[stream fn-2]"
  noah.core/types-vector)
(defmulti
  join
  "[stream global-table fn-2 fn-2 Named]
[stream global-table fn-2 fn-2]
[stream stream fn-2 join-windows Joined]
[stream stream fn-2 join-windows StreamJoined]
[stream stream fn-2 join-windows]
[stream table fn-2 Joined]
[stream table fn-2]
[table table Function fn-2 Materialized]
[table table Function fn-2 Named Materialized]
[table table Function fn-2 Named]
[table table Function fn-2]
[table table fn-2 Materialized]
[table table fn-2 Named Materialized]
[table table fn-2 Named]
[table table fn-2]"
  noah.core/types-vector)
(defmulti
  flat-map-values
  "[stream fn-1 Named]
[stream fn-1]
[stream fn-2 Named]
[stream fn-2]"
  noah.core/types-vector)
(defmulti
  to
  "[stream String produced]
[stream String]
[stream fn-3 produced]
[stream fn-3]"
  noah.core/types-vector)
(defmulti
  suppress
  "[table Suppressed]"
  noah.core/types-vector)
(defmulti
  queryable-store-name
  "[table]"
  noah.core/types-vector)
(defmulti
  group-by-key
  "[stream Grouped]
[stream serialized]
[stream]"
  noah.core/types-vector)
(defmulti
  transform
  "[stream TransformerSupplier Named String]
[stream TransformerSupplier String]"
  noah.core/types-vector-varargs)
(defmulti
  table
  "[StreamsBuilder String Materialized]
[StreamsBuilder String consumed Materialized]
[StreamsBuilder String consumed]
[StreamsBuilder String]"
  noah.core/types-vector)
(defmulti
  add-state-store
  "[StreamsBuilder StoreBuilder]"
  noah.core/types-vector)
(defmulti
  left-join
  "[stream global-table fn-2 fn-2 Named]
[stream global-table fn-2 fn-2]
[stream stream fn-2 join-windows Joined]
[stream stream fn-2 join-windows StreamJoined]
[stream stream fn-2 join-windows]
[stream table fn-2 Joined]
[stream table fn-2]
[table table Function fn-2 Materialized]
[table table Function fn-2 Named Materialized]
[table table Function fn-2 Named]
[table table Function fn-2]
[table table fn-2 Materialized]
[table table fn-2 Named Materialized]
[table table fn-2 Named]
[table table fn-2]"
  noah.core/types-vector)
(defmulti
  filter-not
  "[stream fn-2 Named]
[stream fn-2]
[table fn-2 Materialized]
[table fn-2 Named Materialized]
[table fn-2 Named]
[table fn-2]"
  noah.core/types-vector)
(defmulti
  map-values
  "[stream fn-1 Named]
[stream fn-1]
[stream fn-2 Named]
[stream fn-2]
[table fn-1 Materialized]
[table fn-1 Named Materialized]
[table fn-1 Named]
[table fn-1]
[table fn-2 Materialized]
[table fn-2 Named Materialized]
[table fn-2 Named]
[table fn-2]"
  noah.core/types-vector)
(defmulti
  through
  "[stream String produced]
[stream String]"
  noah.core/types-vector)
(defmulti
  process
  "[stream ProcessorSupplier Named String]
[stream ProcessorSupplier String]"
  noah.core/types-vector-varargs)
(defmulti
  to-stream
  "[table Named]
[table fn-2 Named]
[table fn-2]
[table]"
  noah.core/types-vector)
(defmulti
  flat-transform
  "[stream TransformerSupplier Named String]
[stream TransformerSupplier String]"
  noah.core/types-vector-varargs)
(defmulti
  print
  "[stream Printed]"
  noah.core/types-vector)
(defmulti
  outer-join
  "[stream stream fn-2 join-windows Joined]
[stream stream fn-2 join-windows StreamJoined]
[stream stream fn-2 join-windows]
[table table fn-2 Materialized]
[table table fn-2 Named Materialized]
[table table fn-2 Named]
[table table fn-2]"
  noah.core/types-vector)
(defmulti
  merge
  "[stream stream Named]
[stream stream]"
  noah.core/types-vector)
(defmulti
  stream
  "[StreamsBuilder Collection consumed]
[StreamsBuilder Collection]
[StreamsBuilder Pattern consumed]
[StreamsBuilder Pattern]
[StreamsBuilder String consumed]
[StreamsBuilder String]"
  noah.core/types-vector)
(defmulti
  flat-map
  "[stream fn-2 Named]
[stream fn-2]"
  noah.core/types-vector)
(defmulti
  global-table
  "[StreamsBuilder String Materialized]
[StreamsBuilder String consumed Materialized]
[StreamsBuilder String consumed]
[StreamsBuilder String]"
  noah.core/types-vector)
(defmulti
  cogroup
  "[stream fn-3]"
  noah.core/types-vector)
(defmulti
  transform-values
  "[stream ValueTransformerSupplier Named String]
[stream ValueTransformerSupplier String]
[stream ValueTransformerWithKeySupplier Named String]
[stream ValueTransformerWithKeySupplier String]
[table ValueTransformerWithKeySupplier Materialized Named String]
[table ValueTransformerWithKeySupplier Materialized String]
[table ValueTransformerWithKeySupplier Named String]
[table ValueTransformerWithKeySupplier String]"
  noah.core/types-vector-varargs)
(defmulti
  split
  "[stream Named]\n[stream]"
  noah.core/types-vector)
(defmulti
  build
  "[StreamsBuilder Properties]
[StreamsBuilder]"
  noah.core/types-vector)
(defmulti
  filter
  "[stream fn-2 Named]
[stream fn-2]
[table fn-2 Materialized]
[table fn-2 Named Materialized]
[table fn-2 Named]
[table fn-2]"
  noah.core/types-vector)
(defmulti
  foreach
  "[stream fn-2 Named]
[stream fn-2]"
  noah.core/types-vector)
(defmulti
  flat-transform-values
  "[stream ValueTransformerSupplier Named String]
[stream ValueTransformerSupplier String]
[stream ValueTransformerWithKeySupplier Named String]
[stream ValueTransformerWithKeySupplier String]"
  noah.core/types-vector-varargs)
(defmulti
  to-table
  "[stream Materialized]
[stream Named Materialized]
[stream Named]
[stream]"
  noah.core/types-vector)
(defmulti
  count
  "[stream Materialized]
[stream Named Materialized]
[stream Named]
[stream]
[table Materialized]
[table Named Materialized]
[table Named]
[table]"
  noah.core/types-vector)
(defmulti
  group-by
  "[stream fn-2 Grouped]
[stream fn-2 serialized]
[stream fn-2]
[table fn-2 Grouped]
[table fn-2 serialized]
[table fn-2]"
  noah.core/types-vector)
(defmulti
  select-key
  "[stream fn-2 Named]
[stream fn-2]"
  noah.core/types-vector)
(defmulti
  add-global-store
  "[StreamsBuilder StoreBuilder String String consumed String ProcessorSupplier]
[StreamsBuilder StoreBuilder String consumed ProcessorSupplier]"
  noah.core/types-vector)
(defmethod
  reduce
  [:noah.core/stream :noah.core/fn-2 org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.reduce
   this
   (noah.fn-wrap/reducer a)
   b))
(defmethod
  reduce
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.reduce
   this
   (noah.fn-wrap/reducer a)))
(clojure.core/comment
  noah.impl/skipped
  reduce
  "(this (noah.fn-wrap/reducer a) nil c)
 <- [[org.apache.kafka.streams.kstream.KGroupedStream this] [org.apache.kafka.streams.kstream.Reducer a] [org.apache.kafka.streams.kstream.Named b] [org.apache.kafka.streams.kstream.Materialized c]]")
(defmethod
  reduce
  [:noah.core/table :noah.core/fn-2 :noah.core/fn-2 org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.reduce
   this
   (noah.fn-wrap/reducer a)
   (noah.fn-wrap/reducer b)
   c))
(defmethod
  reduce
  [:noah.core/table :noah.core/fn-2 :noah.core/fn-2]
  [this a b]
  (.reduce
   this
   (noah.fn-wrap/reducer a)
   (noah.fn-wrap/reducer b)))
(clojure.core/comment
  noah.impl/skipped
  reduce
  "(this (noah.fn-wrap/reducer a) (noah.fn-wrap/reducer b) nil d)
 <- [[org.apache.kafka.streams.kstream.KGroupedTable this] [org.apache.kafka.streams.kstream.Reducer a] [org.apache.kafka.streams.kstream.Reducer b] [org.apache.kafka.streams.kstream.Named c] [org.apache.kafka.streams.kstream.Materialized d]]")
(defmethod
  reduce
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.reduce
   this
   (noah.fn-wrap/reducer a)))
(clojure.core/comment
  noah.impl/skipped
  reduce
  "(this (noah.fn-wrap/reducer a) nil c)
 <- [[org.apache.kafka.streams.kstream.SessionWindowedKStream this] [org.apache.kafka.streams.kstream.Reducer a] [org.apache.kafka.streams.kstream.Named b] [org.apache.kafka.streams.kstream.Materialized c]]")
(clojure.core/comment
  noah.impl/skipped
  reduce
  "(this (noah.fn-wrap/reducer a) nil)
 <- [[org.apache.kafka.streams.kstream.SessionWindowedKStream this] [org.apache.kafka.streams.kstream.Reducer a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  reduce
  [:noah.core/stream :noah.core/fn-2 org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.reduce
   this
   (noah.fn-wrap/reducer a)
   b))
(defmethod
  reduce
  [:noah.core/stream :noah.core/fn-2 org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.reduce
   this
   (noah.fn-wrap/reducer a)
   b))
(clojure.core/comment
  noah.impl/skipped
  reduce
  "(this (noah.fn-wrap/reducer a) nil)
 <- [[org.apache.kafka.streams.kstream.TimeWindowedKStream this] [org.apache.kafka.streams.kstream.Reducer a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  reduce
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.reduce
   this
   (noah.fn-wrap/reducer a)))
(clojure.core/comment
  noah.impl/skipped
  reduce
  "(this (noah.fn-wrap/reducer a) nil c)
 <- [[org.apache.kafka.streams.kstream.TimeWindowedKStream this] [org.apache.kafka.streams.kstream.Reducer a] [org.apache.kafka.streams.kstream.Named b] [org.apache.kafka.streams.kstream.Materialized c]]")
(defmethod
  windowed-by
  [:noah.core/stream org.apache.kafka.streams.kstream.SessionWindows]
  [this a]
  (.windowedBy this a))
(defmethod
  windowed-by
  [:noah.core/stream :noah.core/windows]
  [this a]
  (.windowedBy this a))
(clojure.core/comment
  noah.impl/skipped
  windowedBy
  "(this nil)
 <- [[org.apache.kafka.streams.kstream.KGroupedStream this] [org.apache.kafka.streams.kstream.SlidingWindows a]]")
(defmethod
  aggregate
  [:noah.core/stream :noah.core/fn-0 :noah.core/fn-3 org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)
   c))
(clojure.core/comment
  noah.impl/skipped
  aggregate
  "(this (noah.fn-wrap/initializer a) (noah.fn-wrap/aggregator b) nil d)
 <- [[org.apache.kafka.streams.kstream.KGroupedStream this] [org.apache.kafka.streams.kstream.Initializer a] [org.apache.kafka.streams.kstream.Aggregator b] [org.apache.kafka.streams.kstream.Named c] [org.apache.kafka.streams.kstream.Materialized d]]")
(defmethod
  aggregate
  [:noah.core/stream :noah.core/fn-0 :noah.core/fn-3]
  [this a b]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)))
(defmethod
  aggregate
  [:noah.core/table :noah.core/fn-0 :noah.core/fn-3 :noah.core/fn-3 org.apache.kafka.streams.kstream.Materialized]
  [this a b c d]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)
   (noah.fn-wrap/aggregator c)
   d))
(clojure.core/comment
  noah.impl/skipped
  aggregate
  "(this (noah.fn-wrap/initializer a) (noah.fn-wrap/aggregator b) (noah.fn-wrap/aggregator c) nil e)
 <- [[org.apache.kafka.streams.kstream.KGroupedTable this] [org.apache.kafka.streams.kstream.Initializer a] [org.apache.kafka.streams.kstream.Aggregator b] [org.apache.kafka.streams.kstream.Aggregator c] [org.apache.kafka.streams.kstream.Named d] [org.apache.kafka.streams.kstream.Materialized e]]")
(defmethod
  aggregate
  [:noah.core/table :noah.core/fn-0 :noah.core/fn-3 :noah.core/fn-3]
  [this a b c]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)
   (noah.fn-wrap/aggregator c)))
(clojure.core/comment
  noah.impl/skipped
  aggregate
  "(this (noah.fn-wrap/initializer a) (noah.fn-wrap/aggregator b) (noah.fn-wrap/aggregator c) nil)
 <- [[org.apache.kafka.streams.kstream.KGroupedTable this] [org.apache.kafka.streams.kstream.Initializer a] [org.apache.kafka.streams.kstream.Aggregator b] [org.apache.kafka.streams.kstream.Aggregator c] [org.apache.kafka.streams.kstream.Named d]]")
(clojure.core/comment
  noah.impl/skipped
  aggregate
  "(this (noah.fn-wrap/initializer a) (noah.fn-wrap/aggregator b) (noah.fn-wrap/merger c) nil e)
 <- [[org.apache.kafka.streams.kstream.SessionWindowedKStream this] [org.apache.kafka.streams.kstream.Initializer a] [org.apache.kafka.streams.kstream.Aggregator b] [org.apache.kafka.streams.kstream.Merger c] [org.apache.kafka.streams.kstream.Named d] [org.apache.kafka.streams.kstream.Materialized e]]")
(clojure.core/comment
  noah.impl/skipped
  aggregate
  "(this (noah.fn-wrap/initializer a) (noah.fn-wrap/aggregator b) (noah.fn-wrap/merger c) nil)
 <- [[org.apache.kafka.streams.kstream.SessionWindowedKStream this] [org.apache.kafka.streams.kstream.Initializer a] [org.apache.kafka.streams.kstream.Aggregator b] [org.apache.kafka.streams.kstream.Merger c] [org.apache.kafka.streams.kstream.Named d]]")
(defmethod
  aggregate
  [:noah.core/stream :noah.core/fn-0 :noah.core/fn-3 :noah.core/fn-3 org.apache.kafka.streams.kstream.Materialized]
  [this a b c d]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)
   (noah.fn-wrap/merger c)
   d))
(defmethod
  aggregate
  [:noah.core/stream :noah.core/fn-0 :noah.core/fn-3 :noah.core/fn-3]
  [this a b c]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)
   (noah.fn-wrap/merger c)))
(defmethod
  aggregate
  [:noah.core/stream :noah.core/fn-0 :noah.core/fn-3 org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)
   c))
(clojure.core/comment
  noah.impl/skipped
  aggregate
  "(this (noah.fn-wrap/initializer a) (noah.fn-wrap/aggregator b) nil d)
 <- [[org.apache.kafka.streams.kstream.TimeWindowedKStream this] [org.apache.kafka.streams.kstream.Initializer a] [org.apache.kafka.streams.kstream.Aggregator b] [org.apache.kafka.streams.kstream.Named c] [org.apache.kafka.streams.kstream.Materialized d]]")
(defmethod
  aggregate
  [:noah.core/stream :noah.core/fn-0 :noah.core/fn-3]
  [this a b]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)))
(clojure.core/comment
  noah.impl/skipped
  aggregate
  "(this (noah.fn-wrap/initializer a) (noah.fn-wrap/aggregator b) nil)
 <- [[org.apache.kafka.streams.kstream.TimeWindowedKStream this] [org.apache.kafka.streams.kstream.Initializer a] [org.apache.kafka.streams.kstream.Aggregator b] [org.apache.kafka.streams.kstream.Named c]]")
(defmethod
  peek
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.peek
   this
   (noah.fn-wrap/foreach-action a)))
(clojure.core/comment
  noah.impl/skipped
  peek
  "(this (noah.fn-wrap/foreach-action a) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.ForeachAction a] [org.apache.kafka.streams.kstream.Named b]]")
(clojure.core/comment
  noah.impl/skipped
  branch
  "(this nil (clojure.core/into-array org.apache.kafka.streams.kstream.Predicate (clojure.core/map noah.fn-wrap/predicate vararg)))
 <- [[org.apache.kafka.streams.kstream.KStream this nil] [org.apache.kafka.streams.kstream.Named a nil] [org.apache.kafka.streams.kstream.Predicate b org.apache.kafka.streams.kstream.Predicate]]")
(defmethod
  branch
  [:noah.core/stream :noah.core/fn-2]
  [this vararg]
  (.branch
   this
   (clojure.core/into-array
    org.apache.kafka.streams.kstream.Predicate
    (clojure.core/map
     noah.fn-wrap/predicate
     vararg))))
(clojure.core/comment
  noah.impl/skipped
  repartition
  "(this nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.Repartitioned a]]")
(defmethod
  repartition
  [:noah.core/stream]
  [this]
  (.repartition this))
(defmethod
  map
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.map
   this
   (noah.fn-wrap/key-value-mapper
    a)))
(clojure.core/comment
  noah.impl/skipped
  map
  "(this (noah.fn-wrap/key-value-mapper a) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.KeyValueMapper a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  join
  [:noah.core/stream :noah.core/table :noah.core/fn-2 org.apache.kafka.streams.kstream.Joined]
  [this a b c]
  (.join
   this
   a
   (noah.fn-wrap/value-joiner b)
   c))
(defmethod
  join
  [:noah.core/stream :noah.core/table :noah.core/fn-2]
  [this a b]
  (.join
   this
   a
   (noah.fn-wrap/value-joiner b)))
(clojure.core/comment
  noah.impl/skipped
  join
  "(this a (noah.fn-wrap/value-joiner b) c nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.KStream a] [org.apache.kafka.streams.kstream.ValueJoiner b] [org.apache.kafka.streams.kstream.JoinWindows c] [org.apache.kafka.streams.kstream.StreamJoined d]]")
(defmethod
  join
  [:noah.core/stream :noah.core/stream :noah.core/fn-2 :noah.core/join-windows org.apache.kafka.streams.kstream.Joined]
  [this a b c d]
  (.join
   this
   a
   (noah.fn-wrap/value-joiner b)
   c
   d))
(defmethod
  join
  [:noah.core/stream :noah.core/stream :noah.core/fn-2 :noah.core/join-windows]
  [this a b c]
  (.join
   this
   a
   (noah.fn-wrap/value-joiner b)
   c))
(defmethod
  join
  [:noah.core/stream :noah.core/global-table :noah.core/fn-2 :noah.core/fn-2]
  [this a b c]
  (.join
   this
   a
   (noah.fn-wrap/key-value-mapper
    b)
   (noah.fn-wrap/value-joiner c)))
(clojure.core/comment
  noah.impl/skipped
  join
  "(this a (noah.fn-wrap/key-value-mapper b) (noah.fn-wrap/value-joiner c) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.GlobalKTable a] [org.apache.kafka.streams.kstream.KeyValueMapper b] [org.apache.kafka.streams.kstream.ValueJoiner c] [org.apache.kafka.streams.kstream.Named d]]")
(clojure.core/comment
  noah.impl/skipped
  join
  "(this a nil (noah.fn-wrap/value-joiner c))
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [java.util.function.Function b] [org.apache.kafka.streams.kstream.ValueJoiner c]]")
(clojure.core/comment
  noah.impl/skipped
  join
  "(this a (noah.fn-wrap/value-joiner b) nil)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [org.apache.kafka.streams.kstream.ValueJoiner b] [org.apache.kafka.streams.kstream.Named c]]")
(clojure.core/comment
  noah.impl/skipped
  join
  "(this a (noah.fn-wrap/value-joiner b) nil d)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [org.apache.kafka.streams.kstream.ValueJoiner b] [org.apache.kafka.streams.kstream.Named c] [org.apache.kafka.streams.kstream.Materialized d]]")
(defmethod
  join
  [:noah.core/table :noah.core/table :noah.core/fn-2]
  [this a b]
  (.join
   this
   a
   (noah.fn-wrap/value-joiner b)))
(defmethod
  join
  [:noah.core/table :noah.core/table :noah.core/fn-2 org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.join
   this
   a
   (noah.fn-wrap/value-joiner b)
   c))
(clojure.core/comment
  noah.impl/skipped
  join
  "(this a nil (noah.fn-wrap/value-joiner c) nil)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [java.util.function.Function b] [org.apache.kafka.streams.kstream.ValueJoiner c] [org.apache.kafka.streams.kstream.Named d]]")
(clojure.core/comment
  noah.impl/skipped
  join
  "(this a nil (noah.fn-wrap/value-joiner c) d)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [java.util.function.Function b] [org.apache.kafka.streams.kstream.ValueJoiner c] [org.apache.kafka.streams.kstream.Materialized d]]")
(clojure.core/comment
  noah.impl/skipped
  join
  "(this a nil (noah.fn-wrap/value-joiner c) nil e)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [java.util.function.Function b] [org.apache.kafka.streams.kstream.ValueJoiner c] [org.apache.kafka.streams.kstream.Named d] [org.apache.kafka.streams.kstream.Materialized e]]")
(clojure.core/comment
  noah.impl/skipped
  flatMapValues
  "(this (noah.fn-wrap/value-mapper a) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.ValueMapper a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  flat-map-values
  [:noah.core/stream :noah.core/fn-1]
  [this a]
  (.flatMapValues
   this
   (noah.fn-wrap/value-mapper a)))
(clojure.core/comment
  noah.impl/skipped
  flatMapValues
  "(this (noah.fn-wrap/value-mapper-with-key a) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.ValueMapperWithKey a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  flat-map-values
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.flatMapValues
   this
   (noah.fn-wrap/value-mapper-with-key
    a)))
(defmethod
  to
  [:noah.core/stream java.lang.String]
  [this a]
  (.to this a))
(defmethod
  to
  [:noah.core/stream :noah.core/fn-3]
  [this a]
  (.to
   this
   (noah.fn-wrap/topic-name-extractor
    a)))
(defmethod
  to
  [:noah.core/stream :noah.core/fn-3 :noah.core/produced]
  [this a b]
  (.to
   this
   (noah.fn-wrap/topic-name-extractor
    a)
   (noah.core/produced b)))
(defmethod
  to
  [:noah.core/stream java.lang.String
   :noah.core/produced]
  [this a b]
  (.to
   this
   a
   (noah.core/produced b)))
(defmethod
  suppress
  [:noah.core/table org.apache.kafka.streams.kstream.Suppressed]
  [this a]
  (.suppress this a))
(defmethod
  queryable-store-name
  [:noah.core/table]
  [this]
  (.queryableStoreName this))
(defmethod
  group-by-key
  [:noah.core/stream :noah.core/serialized]
  [this a]
  (.groupByKey
   this
   (noah.core/serialized a)))
(defmethod
  group-by-key
  [:noah.core/stream org.apache.kafka.streams.kstream.Grouped]
  [this a]
  (.groupByKey this a))
(defmethod
  group-by-key
  [:noah.core/stream]
  [this]
  (.groupByKey this))
(clojure.core/comment
  noah.impl/skipped
  transform
  "(this a nil (clojure.core/into-array java.lang.String (clojure.core/map clojure.core/identity vararg)))
 <- [[org.apache.kafka.streams.kstream.KStream this nil] [org.apache.kafka.streams.kstream.TransformerSupplier a nil] [org.apache.kafka.streams.kstream.Named b nil] [java.lang.String c java.lang.String]]")
(defmethod
  transform
  [:noah.core/stream org.apache.kafka.streams.kstream.TransformerSupplier
   java.lang.String]
  [this a vararg]
  (.transform
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map
     clojure.core/identity
     vararg))))
(defmethod
  table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.table
   this
   a
   (noah.core/consumed b)
   c))
(defmethod
  table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String]
  [this a]
  (.table this a))
(defmethod
  table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.table this a b))
(defmethod
  table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed]
  [this a b]
  (.table
   this
   a
   (noah.core/consumed b)))
(defmethod
  add-state-store
  [org.apache.kafka.streams.StreamsBuilder
   org.apache.kafka.streams.state.StoreBuilder]
  [this a]
  (.addStateStore this a))
(defmethod
  left-join
  [:noah.core/stream :noah.core/stream :noah.core/fn-2 :noah.core/join-windows org.apache.kafka.streams.kstream.Joined]
  [this a b c d]
  (.leftJoin
   this
   a
   (noah.fn-wrap/value-joiner b)
   c
   d))
(clojure.core/comment
  noah.impl/skipped
  leftJoin
  "(this a (noah.fn-wrap/value-joiner b) c nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.KStream a] [org.apache.kafka.streams.kstream.ValueJoiner b] [org.apache.kafka.streams.kstream.JoinWindows c] [org.apache.kafka.streams.kstream.StreamJoined d]]")
(defmethod
  left-join
  [:noah.core/stream :noah.core/table :noah.core/fn-2]
  [this a b]
  (.leftJoin
   this
   a
   (noah.fn-wrap/value-joiner b)))
(clojure.core/comment
  noah.impl/skipped
  leftJoin
  "(this a (noah.fn-wrap/key-value-mapper b) (noah.fn-wrap/value-joiner c) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.GlobalKTable a] [org.apache.kafka.streams.kstream.KeyValueMapper b] [org.apache.kafka.streams.kstream.ValueJoiner c] [org.apache.kafka.streams.kstream.Named d]]")
(defmethod
  left-join
  [:noah.core/stream :noah.core/stream :noah.core/fn-2 :noah.core/join-windows]
  [this a b c]
  (.leftJoin
   this
   a
   (noah.fn-wrap/value-joiner b)
   c))
(defmethod
  left-join
  [:noah.core/stream :noah.core/global-table :noah.core/fn-2 :noah.core/fn-2]
  [this a b c]
  (.leftJoin
   this
   a
   (noah.fn-wrap/key-value-mapper
    b)
   (noah.fn-wrap/value-joiner c)))
(defmethod
  left-join
  [:noah.core/stream :noah.core/table :noah.core/fn-2 org.apache.kafka.streams.kstream.Joined]
  [this a b c]
  (.leftJoin
   this
   a
   (noah.fn-wrap/value-joiner b)
   c))
(clojure.core/comment
  noah.impl/skipped
  leftJoin
  "(this a nil (noah.fn-wrap/value-joiner c) nil e)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [java.util.function.Function b] [org.apache.kafka.streams.kstream.ValueJoiner c] [org.apache.kafka.streams.kstream.Named d] [org.apache.kafka.streams.kstream.Materialized e]]")
(clojure.core/comment
  noah.impl/skipped
  leftJoin
  "(this a (noah.fn-wrap/value-joiner b) nil d)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [org.apache.kafka.streams.kstream.ValueJoiner b] [org.apache.kafka.streams.kstream.Named c] [org.apache.kafka.streams.kstream.Materialized d]]")
(clojure.core/comment
  noah.impl/skipped
  leftJoin
  "(this a nil (noah.fn-wrap/value-joiner c))
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [java.util.function.Function b] [org.apache.kafka.streams.kstream.ValueJoiner c]]")
(clojure.core/comment
  noah.impl/skipped
  leftJoin
  "(this a nil (noah.fn-wrap/value-joiner c) d)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [java.util.function.Function b] [org.apache.kafka.streams.kstream.ValueJoiner c] [org.apache.kafka.streams.kstream.Materialized d]]")
(clojure.core/comment
  noah.impl/skipped
  leftJoin
  "(this a (noah.fn-wrap/value-joiner b) nil)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [org.apache.kafka.streams.kstream.ValueJoiner b] [org.apache.kafka.streams.kstream.Named c]]")
(clojure.core/comment
  noah.impl/skipped
  leftJoin
  "(this a nil (noah.fn-wrap/value-joiner c) nil)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [java.util.function.Function b] [org.apache.kafka.streams.kstream.ValueJoiner c] [org.apache.kafka.streams.kstream.Named d]]")
(defmethod
  left-join
  [:noah.core/table :noah.core/table :noah.core/fn-2 org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.leftJoin
   this
   a
   (noah.fn-wrap/value-joiner b)
   c))
(defmethod
  left-join
  [:noah.core/table :noah.core/table :noah.core/fn-2]
  [this a b]
  (.leftJoin
   this
   a
   (noah.fn-wrap/value-joiner b)))
(defmethod
  filter-not
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.filterNot
   this
   (noah.fn-wrap/predicate a)))
(clojure.core/comment
  noah.impl/skipped
  filterNot
  "(this (noah.fn-wrap/predicate a) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.Predicate a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  filter-not
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.filterNot
   this
   (noah.fn-wrap/predicate a)))
(clojure.core/comment
  noah.impl/skipped
  filterNot
  "(this (noah.fn-wrap/predicate a) nil c)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.Predicate a] [org.apache.kafka.streams.kstream.Named b] [org.apache.kafka.streams.kstream.Materialized c]]")
(defmethod
  filter-not
  [:noah.core/table :noah.core/fn-2 org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.filterNot
   this
   (noah.fn-wrap/predicate a)
   b))
(clojure.core/comment
  noah.impl/skipped
  filterNot
  "(this (noah.fn-wrap/predicate a) nil)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.Predicate a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  map-values
  [:noah.core/stream :noah.core/fn-1]
  [this a]
  (.mapValues
   this
   (noah.fn-wrap/value-mapper a)))
(clojure.core/comment
  noah.impl/skipped
  mapValues
  "(this (noah.fn-wrap/value-mapper a) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.ValueMapper a] [org.apache.kafka.streams.kstream.Named b]]")
(clojure.core/comment
  noah.impl/skipped
  mapValues
  "(this (noah.fn-wrap/value-mapper-with-key a) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.ValueMapperWithKey a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  map-values
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.mapValues
   this
   (noah.fn-wrap/value-mapper-with-key
    a)))
(clojure.core/comment
  noah.impl/skipped
  mapValues
  "(this (noah.fn-wrap/value-mapper-with-key a) nil)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.ValueMapperWithKey a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  map-values
  [:noah.core/table :noah.core/fn-2 org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.mapValues
   this
   (noah.fn-wrap/value-mapper-with-key
    a)
   b))
(defmethod
  map-values
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.mapValues
   this
   (noah.fn-wrap/value-mapper-with-key
    a)))
(defmethod
  map-values
  [:noah.core/table :noah.core/fn-1 org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.mapValues
   this
   (noah.fn-wrap/value-mapper a)
   b))
(clojure.core/comment
  noah.impl/skipped
  mapValues
  "(this (noah.fn-wrap/value-mapper a) nil)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.ValueMapper a] [org.apache.kafka.streams.kstream.Named b]]")
(clojure.core/comment
  noah.impl/skipped
  mapValues
  "(this (noah.fn-wrap/value-mapper a) nil c)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.ValueMapper a] [org.apache.kafka.streams.kstream.Named b] [org.apache.kafka.streams.kstream.Materialized c]]")
(defmethod
  map-values
  [:noah.core/table :noah.core/fn-1]
  [this a]
  (.mapValues
   this
   (noah.fn-wrap/value-mapper a)))
(clojure.core/comment
  noah.impl/skipped
  mapValues
  "(this (noah.fn-wrap/value-mapper-with-key a) nil c)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.ValueMapperWithKey a] [org.apache.kafka.streams.kstream.Named b] [org.apache.kafka.streams.kstream.Materialized c]]")
(defmethod
  through
  [:noah.core/stream java.lang.String]
  [this a]
  (.through this a))
(defmethod
  through
  [:noah.core/stream java.lang.String
   :noah.core/produced]
  [this a b]
  (.through
   this
   a
   (noah.core/produced b)))
(defmethod
  process
  [:noah.core/stream org.apache.kafka.streams.processor.ProcessorSupplier
   java.lang.String]
  [this a vararg]
  (.process
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map
     clojure.core/identity
     vararg))))
(clojure.core/comment
  noah.impl/skipped
  process
  "(this a nil (clojure.core/into-array java.lang.String (clojure.core/map clojure.core/identity vararg)))
 <- [[org.apache.kafka.streams.kstream.KStream this nil] [org.apache.kafka.streams.processor.ProcessorSupplier a nil] [org.apache.kafka.streams.kstream.Named b nil] [java.lang.String c java.lang.String]]")
(clojure.core/comment
  noah.impl/skipped
  toStream
  "(this nil)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.Named a]]")
(clojure.core/comment
  noah.impl/skipped
  toStream
  "(this (noah.fn-wrap/key-value-mapper a) nil)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KeyValueMapper a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  to-stream
  [:noah.core/table]
  [this]
  (.toStream this))
(defmethod
  to-stream
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.toStream
   this
   (noah.fn-wrap/key-value-mapper
    a)))
(clojure.core/comment
  noah.impl/skipped
  flatTransform
  "(this a nil (clojure.core/into-array java.lang.String (clojure.core/map clojure.core/identity vararg)))
 <- [[org.apache.kafka.streams.kstream.KStream this nil] [org.apache.kafka.streams.kstream.TransformerSupplier a nil] [org.apache.kafka.streams.kstream.Named b nil] [java.lang.String c java.lang.String]]")
(defmethod
  flat-transform
  [:noah.core/stream org.apache.kafka.streams.kstream.TransformerSupplier
   java.lang.String]
  [this a vararg]
  (.flatTransform
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map
     clojure.core/identity
     vararg))))
(defmethod
  print
  [:noah.core/stream org.apache.kafka.streams.kstream.Printed]
  [this a]
  (.print this a))
(defmethod
  outer-join
  [:noah.core/stream :noah.core/stream :noah.core/fn-2 :noah.core/join-windows]
  [this a b c]
  (.outerJoin
   this
   a
   (noah.fn-wrap/value-joiner b)
   c))
(clojure.core/comment
  noah.impl/skipped
  outerJoin
  "(this a (noah.fn-wrap/value-joiner b) c nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.KStream a] [org.apache.kafka.streams.kstream.ValueJoiner b] [org.apache.kafka.streams.kstream.JoinWindows c] [org.apache.kafka.streams.kstream.StreamJoined d]]")
(defmethod
  outer-join
  [:noah.core/stream :noah.core/stream :noah.core/fn-2 :noah.core/join-windows org.apache.kafka.streams.kstream.Joined]
  [this a b c d]
  (.outerJoin
   this
   a
   (noah.fn-wrap/value-joiner b)
   c
   d))
(clojure.core/comment
  noah.impl/skipped
  outerJoin
  "(this a (noah.fn-wrap/value-joiner b) nil)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [org.apache.kafka.streams.kstream.ValueJoiner b] [org.apache.kafka.streams.kstream.Named c]]")
(defmethod
  outer-join
  [:noah.core/table :noah.core/table :noah.core/fn-2]
  [this a b]
  (.outerJoin
   this
   a
   (noah.fn-wrap/value-joiner b)))
(defmethod
  outer-join
  [:noah.core/table :noah.core/table :noah.core/fn-2 org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.outerJoin
   this
   a
   (noah.fn-wrap/value-joiner b)
   c))
(clojure.core/comment
  noah.impl/skipped
  outerJoin
  "(this a (noah.fn-wrap/value-joiner b) nil d)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.KTable a] [org.apache.kafka.streams.kstream.ValueJoiner b] [org.apache.kafka.streams.kstream.Named c] [org.apache.kafka.streams.kstream.Materialized d]]")
(clojure.core/comment
  noah.impl/skipped
  merge
  "(this a nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.KStream a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  merge
  [:noah.core/stream :noah.core/stream]
  [this a]
  (.merge this a))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder
   java.util.regex.Pattern]
  [this a]
  (.stream this a))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder
   java.util.Collection
   :noah.core/consumed]
  [this a b]
  (.stream
   this
   a
   (noah.core/consumed b)))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String]
  [this a]
  (.stream this a))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder
   java.util.Collection]
  [this a]
  (.stream this a))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed]
  [this a b]
  (.stream
   this
   a
   (noah.core/consumed b)))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder
   java.util.regex.Pattern
   :noah.core/consumed]
  [this a b]
  (.stream
   this
   a
   (noah.core/consumed b)))
(defmethod
  flat-map
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.flatMap
   this
   (noah.fn-wrap/key-value-mapper
    a)))
(clojure.core/comment
  noah.impl/skipped
  flatMap
  "(this (noah.fn-wrap/key-value-mapper a) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.KeyValueMapper a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  global-table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed]
  [this a b]
  (.globalTable
   this
   a
   (noah.core/consumed b)))
(defmethod
  global-table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.globalTable
   this
   a
   (noah.core/consumed b)
   c))
(defmethod
  global-table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.globalTable this a b))
(defmethod
  global-table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String]
  [this a]
  (.globalTable this a))
(defmethod
  cogroup
  [:noah.core/stream :noah.core/fn-3]
  [this a]
  (.cogroup
   this
   (noah.fn-wrap/aggregator a)))
(defmethod
  transform-values
  [:noah.core/stream org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
   java.lang.String]
  [this a vararg]
  (.transformValues
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map
     clojure.core/identity
     vararg))))
(defmethod
  transform-values
  [:noah.core/stream org.apache.kafka.streams.kstream.ValueTransformerSupplier
   java.lang.String]
  [this a vararg]
  (.transformValues
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map
     clojure.core/identity
     vararg))))
(clojure.core/comment
  noah.impl/skipped
  transformValues
  "(this a nil (clojure.core/into-array java.lang.String (clojure.core/map clojure.core/identity vararg)))
 <- [[org.apache.kafka.streams.kstream.KStream this nil] [org.apache.kafka.streams.kstream.ValueTransformerSupplier a nil] [org.apache.kafka.streams.kstream.Named b nil] [java.lang.String c java.lang.String]]")
(clojure.core/comment
  noah.impl/skipped
  transformValues
  "(this a nil (clojure.core/into-array java.lang.String (clojure.core/map clojure.core/identity vararg)))
 <- [[org.apache.kafka.streams.kstream.KStream this nil] [org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier a nil] [org.apache.kafka.streams.kstream.Named b nil] [java.lang.String c java.lang.String]]")
(defmethod
  transform-values
  [:noah.core/table org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
   java.lang.String]
  [this a vararg]
  (.transformValues
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map
     clojure.core/identity
     vararg))))
(defmethod
  transform-values
  [:noah.core/table org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
   org.apache.kafka.streams.kstream.Materialized
   java.lang.String]
  [this a b vararg]
  (.transformValues
   this
   a
   b
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map
     clojure.core/identity
     vararg))))
(clojure.core/comment
  noah.impl/skipped
  transformValues
  "(this a nil (clojure.core/into-array java.lang.String (clojure.core/map clojure.core/identity vararg)))
 <- [[org.apache.kafka.streams.kstream.KTable this nil] [org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier a nil] [org.apache.kafka.streams.kstream.Named b nil] [java.lang.String c java.lang.String]]")
(clojure.core/comment
  noah.impl/skipped
  transformValues
  "(this a b nil (clojure.core/into-array java.lang.String (clojure.core/map clojure.core/identity vararg)))
 <- [[org.apache.kafka.streams.kstream.KTable this nil] [org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier a nil] [org.apache.kafka.streams.kstream.Materialized b nil] [org.apache.kafka.streams.kstream.Named c nil] [java.lang.String d java.lang.String]]")
(clojure.core/comment
  noah.impl/skipped
  split
  "(this nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.Named a]]")
(defmethod
  split
  [:noah.core/stream]
  [this]
  (.split this))
(defmethod
  build
  [org.apache.kafka.streams.StreamsBuilder]
  [this]
  (.build this))
(defmethod
  build
  [org.apache.kafka.streams.StreamsBuilder
   java.util.Properties]
  [this a]
  (.build
   this
   (noah.core/map->properties a)))
(defmethod
  filter
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.filter
   this
   (noah.fn-wrap/predicate a)))
(clojure.core/comment
  noah.impl/skipped
  filter
  "(this (noah.fn-wrap/predicate a) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.Predicate a] [org.apache.kafka.streams.kstream.Named b]]")
(clojure.core/comment
  noah.impl/skipped
  filter
  "(this (noah.fn-wrap/predicate a) nil c)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.Predicate a] [org.apache.kafka.streams.kstream.Named b] [org.apache.kafka.streams.kstream.Materialized c]]")
(defmethod
  filter
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.filter
   this
   (noah.fn-wrap/predicate a)))
(defmethod
  filter
  [:noah.core/table :noah.core/fn-2 org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.filter
   this
   (noah.fn-wrap/predicate a)
   b))
(clojure.core/comment
  noah.impl/skipped
  filter
  "(this (noah.fn-wrap/predicate a) nil)
 <- [[org.apache.kafka.streams.kstream.KTable this] [org.apache.kafka.streams.kstream.Predicate a] [org.apache.kafka.streams.kstream.Named b]]")
(clojure.core/comment
  noah.impl/skipped
  foreach
  "(this (noah.fn-wrap/foreach-action a) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.ForeachAction a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  foreach
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.foreach
   this
   (noah.fn-wrap/foreach-action a)))
(defmethod
  flat-transform-values
  [:noah.core/stream org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
   java.lang.String]
  [this a vararg]
  (.flatTransformValues
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map
     clojure.core/identity
     vararg))))
(clojure.core/comment
  noah.impl/skipped
  flatTransformValues
  "(this a nil (clojure.core/into-array java.lang.String (clojure.core/map clojure.core/identity vararg)))
 <- [[org.apache.kafka.streams.kstream.KStream this nil] [org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier a nil] [org.apache.kafka.streams.kstream.Named b nil] [java.lang.String c java.lang.String]]")
(defmethod
  flat-transform-values
  [:noah.core/stream org.apache.kafka.streams.kstream.ValueTransformerSupplier
   java.lang.String]
  [this a vararg]
  (.flatTransformValues
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map
     clojure.core/identity
     vararg))))
(clojure.core/comment
  noah.impl/skipped
  flatTransformValues
  "(this a nil (clojure.core/into-array java.lang.String (clojure.core/map clojure.core/identity vararg)))
 <- [[org.apache.kafka.streams.kstream.KStream this nil] [org.apache.kafka.streams.kstream.ValueTransformerSupplier a nil] [org.apache.kafka.streams.kstream.Named b nil] [java.lang.String c java.lang.String]]")
(defmethod
  to-table
  [:noah.core/stream]
  [this]
  (.toTable this))
(clojure.core/comment
  noah.impl/skipped
  toTable
  "(this nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.Named a]]")
(clojure.core/comment
  noah.impl/skipped
  toTable
  "(this nil b)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.Named a] [org.apache.kafka.streams.kstream.Materialized b]]")
(defmethod
  to-table
  [:noah.core/stream org.apache.kafka.streams.kstream.Materialized]
  [this a]
  (.toTable this a))
(clojure.core/comment
  noah.impl/skipped
  count
  "(this nil b)
 <- [[org.apache.kafka.streams.kstream.KGroupedStream this] [org.apache.kafka.streams.kstream.Named a] [org.apache.kafka.streams.kstream.Materialized b]]")
(defmethod
  count
  [:noah.core/stream org.apache.kafka.streams.kstream.Materialized]
  [this a]
  (.count this a))
(clojure.core/comment
  noah.impl/skipped
  count
  "(this nil)
 <- [[org.apache.kafka.streams.kstream.KGroupedStream this] [org.apache.kafka.streams.kstream.Named a]]")
(defmethod
  count
  [:noah.core/stream]
  [this]
  (.count this))
(defmethod
  count
  [:noah.core/table org.apache.kafka.streams.kstream.Materialized]
  [this a]
  (.count this a))
(defmethod
  count
  [:noah.core/table]
  [this]
  (.count this))
(clojure.core/comment
  noah.impl/skipped
  count
  "(this nil)
 <- [[org.apache.kafka.streams.kstream.KGroupedTable this] [org.apache.kafka.streams.kstream.Named a]]")
(clojure.core/comment
  noah.impl/skipped
  count
  "(this nil b)
 <- [[org.apache.kafka.streams.kstream.KGroupedTable this] [org.apache.kafka.streams.kstream.Named a] [org.apache.kafka.streams.kstream.Materialized b]]")
(clojure.core/comment
  noah.impl/skipped
  count
  "(this nil)
 <- [[org.apache.kafka.streams.kstream.SessionWindowedKStream this] [org.apache.kafka.streams.kstream.Named a]]")
(defmethod
  count
  [:noah.core/stream org.apache.kafka.streams.kstream.Materialized]
  [this a]
  (.count this a))
(clojure.core/comment
  noah.impl/skipped
  count
  "(this nil b)
 <- [[org.apache.kafka.streams.kstream.SessionWindowedKStream this] [org.apache.kafka.streams.kstream.Named a] [org.apache.kafka.streams.kstream.Materialized b]]")
(defmethod
  count
  [:noah.core/stream]
  [this]
  (.count this))
(defmethod
  count
  [:noah.core/stream]
  [this]
  (.count this))
(clojure.core/comment
  noah.impl/skipped
  count
  "(this nil b)
 <- [[org.apache.kafka.streams.kstream.TimeWindowedKStream this] [org.apache.kafka.streams.kstream.Named a] [org.apache.kafka.streams.kstream.Materialized b]]")
(defmethod
  count
  [:noah.core/stream org.apache.kafka.streams.kstream.Materialized]
  [this a]
  (.count this a))
(clojure.core/comment
  noah.impl/skipped
  count
  "(this nil)
 <- [[org.apache.kafka.streams.kstream.TimeWindowedKStream this] [org.apache.kafka.streams.kstream.Named a]]")
(defmethod
  group-by
  [:noah.core/stream :noah.core/fn-2 :noah.core/serialized]
  [this a b]
  (.groupBy
   this
   (noah.fn-wrap/key-value-mapper
    a)
   (noah.core/serialized b)))
(defmethod
  group-by
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.groupBy
   this
   (noah.fn-wrap/key-value-mapper
    a)))
(defmethod
  group-by
  [:noah.core/stream :noah.core/fn-2 org.apache.kafka.streams.kstream.Grouped]
  [this a b]
  (.groupBy
   this
   (noah.fn-wrap/key-value-mapper
    a)
   b))
(defmethod
  group-by
  [:noah.core/table :noah.core/fn-2 org.apache.kafka.streams.kstream.Grouped]
  [this a b]
  (.groupBy
   this
   (noah.fn-wrap/key-value-mapper
    a)
   b))
(defmethod
  group-by
  [:noah.core/table :noah.core/fn-2 :noah.core/serialized]
  [this a b]
  (.groupBy
   this
   (noah.fn-wrap/key-value-mapper
    a)
   (noah.core/serialized b)))
(defmethod
  group-by
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.groupBy
   this
   (noah.fn-wrap/key-value-mapper
    a)))
(clojure.core/comment
  noah.impl/skipped
  selectKey
  "(this (noah.fn-wrap/key-value-mapper a) nil)
 <- [[org.apache.kafka.streams.kstream.KStream this] [org.apache.kafka.streams.kstream.KeyValueMapper a] [org.apache.kafka.streams.kstream.Named b]]")
(defmethod
  select-key
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.selectKey
   this
   (noah.fn-wrap/key-value-mapper
    a)))
(clojure.core/comment
  noah.impl/skipped
  addGlobalStore
  "(this a b (noah.core/consumed c) nil)
 <- [[org.apache.kafka.streams.StreamsBuilder this] [org.apache.kafka.streams.state.StoreBuilder a] [java.lang.String b] [org.apache.kafka.streams.kstream.Consumed c] [org.apache.kafka.streams.processor.api.ProcessorSupplier d]]")
(defmethod
  add-global-store
  [org.apache.kafka.streams.StreamsBuilder
   org.apache.kafka.streams.state.StoreBuilder
   java.lang.String
   :noah.core/consumed org.apache.kafka.streams.processor.ProcessorSupplier]
  [this a b c d]
  (.addGlobalStore
   this
   a
   b
   (noah.core/consumed c)
   d))
(defmethod
  add-global-store
  [org.apache.kafka.streams.StreamsBuilder
   org.apache.kafka.streams.state.StoreBuilder
   java.lang.String
   java.lang.String
   :noah.core/consumed java.lang.String
   org.apache.kafka.streams.processor.ProcessorSupplier]
  [this a b c d e f]
  (.addGlobalStore
   this
   a
   b
   c
   (noah.core/consumed d)
   e
   f))

