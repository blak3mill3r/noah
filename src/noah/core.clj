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
           [org.apache.kafka.streams.kstream Aggregator Consumed GlobalKTable Initializer Joined JoinWindows KeyValueMapper ValueMapperWithKey KGroupedStream KGroupedTable KStream KTable Materialized Merger Predicate Produced Reducer Serialized SessionWindowedKStream SessionWindows ValueJoiner ValueMapper Windows TimeWindowedKStream TransformerSupplier Transformer ValueTransformerWithKeySupplier ValueTransformerWithKey ValueTransformerSupplier ValueTransformer]
           [org.apache.kafka.streams.kstream.internals KTableImpl KStreamImpl KGroupedStreamImpl]
           [org.apache.kafka.streams.state KeyValueStore]
           [org.apache.kafka.streams.processor TopicNameExtractor]
           [java.util Collections Map Properties]))

(potemkin/import-vars [noah.serdes serdes])
(potemkin/import-vars [noah.map-wrap consumed produced serialized materialized map->properties])
(potemkin/import-vars [noah.wrap kafka-streams streams-builder kv transduce])
(potemkin/import-vars [noah.impl types-vector types-vector-varargs])
(potemkin/import-vars [noah.transformer context])

;; the macro expansion of this produces the remainder of this source file
;; which is included inline for developer convenience
#_(defwrappers)
#_(macroexpand-1 '(defwrappers))

(defmulti reduce "[stream fn-2 Materialized]\n[stream fn-2]\n[table fn-2 fn-2 Materialized]\n[table fn-2 fn-2]" noah.core/types-vector)
(defmulti windowed-by "[stream SessionWindows]\n[stream windows]" noah.core/types-vector)
(defmulti aggregate "[stream fn-0 fn-3 Materialized]\n[stream fn-0 fn-3 fn-3 Materialized]\n[stream fn-0 fn-3 fn-3]\n[stream fn-0 fn-3]\n[table fn-0 fn-3 fn-3 Materialized]\n[table fn-0 fn-3 fn-3]" noah.core/types-vector)
(defmulti peek "[stream fn-2]" noah.core/types-vector)
(defmulti branch "[stream fn-2]" noah.core/types-vector-varargs)
(defmulti map "[stream fn-2]" noah.core/types-vector)
(defmulti join "[stream global-table fn-2 fn-2]\n[stream stream fn-2 join-windows Joined]\n[stream stream fn-2 join-windows]\n[stream table fn-2 Joined]\n[stream table fn-2]\n[table table fn-2 Materialized]\n[table table fn-2]" noah.core/types-vector)
(defmulti flat-map-values "[stream fn-1]\n[stream fn-2]" noah.core/types-vector)
(defmulti to "[stream String produced]\n[stream String]\n[stream fn-3 produced]\n[stream fn-3]" noah.core/types-vector)
(defmulti suppress "[table Suppressed]" noah.core/types-vector)
(defmulti queryable-store-name "[table]" noah.core/types-vector)
(defmulti group-by-key "[stream Grouped]\n[stream serialized]\n[stream]" noah.core/types-vector)
(defmulti transform "[stream TransformerSupplier String]" noah.core/types-vector-varargs)
(defmulti table "[StreamsBuilder String Materialized]\n[StreamsBuilder String consumed Materialized]\n[StreamsBuilder String consumed]\n[StreamsBuilder String]" noah.core/types-vector)
(defmulti add-state-store "[StreamsBuilder StoreBuilder]" noah.core/types-vector)
(defmulti left-join "[stream global-table fn-2 fn-2]\n[stream stream fn-2 join-windows Joined]\n[stream stream fn-2 join-windows]\n[stream table fn-2 Joined]\n[stream table fn-2]\n[table table fn-2 Materialized]\n[table table fn-2]" noah.core/types-vector)
(defmulti filter-not "[stream fn-2]\n[table fn-2 Materialized]\n[table fn-2]" noah.core/types-vector)
(defmulti map-values "[stream fn-1]\n[stream fn-2]\n[table fn-1 Materialized]\n[table fn-1]\n[table fn-2 Materialized]\n[table fn-2]" noah.core/types-vector)
(defmulti through "[stream String produced]\n[stream String]" noah.core/types-vector)
(defmulti process "[stream ProcessorSupplier String]" noah.core/types-vector-varargs)
(defmulti to-stream "[table fn-2]\n[table]" noah.core/types-vector)
(defmulti flat-transform "[stream TransformerSupplier String]" noah.core/types-vector-varargs)
(defmulti print "[stream Printed]" noah.core/types-vector)
(defmulti outer-join "[stream stream fn-2 join-windows Joined]\n[stream stream fn-2 join-windows]\n[table table fn-2 Materialized]\n[table table fn-2]" noah.core/types-vector)
(defmulti merge "[stream stream]" noah.core/types-vector)
(defmulti stream "[StreamsBuilder Collection consumed]\n[StreamsBuilder Collection]\n[StreamsBuilder Pattern consumed]\n[StreamsBuilder Pattern]\n[StreamsBuilder String consumed]\n[StreamsBuilder String]" noah.core/types-vector)
(defmulti flat-map "[stream fn-2]" noah.core/types-vector)
(defmulti global-table "[StreamsBuilder String Materialized]\n[StreamsBuilder String consumed Materialized]\n[StreamsBuilder String consumed]\n[StreamsBuilder String]" noah.core/types-vector)
(defmulti transform-values "[stream ValueTransformerSupplier String]\n[stream ValueTransformerWithKeySupplier String]\n[table ValueTransformerWithKeySupplier Materialized String]\n[table ValueTransformerWithKeySupplier String]" noah.core/types-vector-varargs)
(defmulti build "[StreamsBuilder Properties]\n[StreamsBuilder]" noah.core/types-vector)
(defmulti filter "[stream fn-2]\n[table fn-2 Materialized]\n[table fn-2]" noah.core/types-vector)
(defmulti foreach "[stream fn-2]" noah.core/types-vector)
(defmulti count "[stream Materialized]\n[stream]\n[table Materialized]\n[table]" noah.core/types-vector)
(defmulti group-by "[stream fn-2 Grouped]\n[stream fn-2 serialized]\n[stream fn-2]\n[table fn-2 Grouped]\n[table fn-2 serialized]\n[table fn-2]" noah.core/types-vector)
(defmulti select-key "[stream fn-2]" noah.core/types-vector)
(defmulti add-global-store "[StreamsBuilder StoreBuilder String String consumed String ProcessorSupplier]\n[StreamsBuilder StoreBuilder String consumed ProcessorSupplier]" noah.core/types-vector)
(defmethod
  reduce
  [:noah.core/stream
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.reduce this (noah.fn-wrap/reducer a) b))
(defmethod
  reduce
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.reduce this (noah.fn-wrap/reducer a)))
(defmethod
  reduce
  [:noah.core/table
   :noah.core/fn-2
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.reduce this (noah.fn-wrap/reducer a) (noah.fn-wrap/reducer b) c))
(defmethod
  reduce
  [:noah.core/table :noah.core/fn-2 :noah.core/fn-2]
  [this a b]
  (.reduce this (noah.fn-wrap/reducer a) (noah.fn-wrap/reducer b)))
(defmethod
  reduce
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.reduce this (noah.fn-wrap/reducer a)))
(defmethod
  reduce
  [:noah.core/stream
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.reduce this (noah.fn-wrap/reducer a) b))
(defmethod
  reduce
  [:noah.core/stream
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.reduce this (noah.fn-wrap/reducer a) b))
(defmethod
  reduce
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.reduce this (noah.fn-wrap/reducer a)))
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
(defmethod
  aggregate
  [:noah.core/stream
   :noah.core/fn-0
   :noah.core/fn-3
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)
   c))
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
  [:noah.core/table
   :noah.core/fn-0
   :noah.core/fn-3
   :noah.core/fn-3
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c d]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)
   (noah.fn-wrap/aggregator c)
   d))
(defmethod
  aggregate
  [:noah.core/table :noah.core/fn-0 :noah.core/fn-3 :noah.core/fn-3]
  [this a b c]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)
   (noah.fn-wrap/aggregator c)))
(defmethod
  aggregate
  [:noah.core/stream
   :noah.core/fn-0
   :noah.core/fn-3
   :noah.core/fn-3
   org.apache.kafka.streams.kstream.Materialized]
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
  [:noah.core/stream
   :noah.core/fn-0
   :noah.core/fn-3
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)
   c))
(defmethod
  aggregate
  [:noah.core/stream :noah.core/fn-0 :noah.core/fn-3]
  [this a b]
  (.aggregate
   this
   (noah.fn-wrap/initializer a)
   (noah.fn-wrap/aggregator b)))
(defmethod
  peek
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.peek this (noah.fn-wrap/foreach-action a)))
(defmethod
  branch
  [:noah.core/stream :noah.core/fn-2]
  [this vararg]
  (.branch
   this
   (clojure.core/into-array
    org.apache.kafka.streams.kstream.Predicate
    (clojure.core/map noah.fn-wrap/predicate vararg))))
(defmethod
  map
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.map this (noah.fn-wrap/key-value-mapper a)))
(defmethod
  join
  [:noah.core/stream
   :noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Joined]
  [this a b c]
  (.join this a (noah.fn-wrap/value-joiner b) c))
(defmethod
  join
  [:noah.core/stream :noah.core/table :noah.core/fn-2]
  [this a b]
  (.join this a (noah.fn-wrap/value-joiner b)))
(defmethod
  join
  [:noah.core/stream
   :noah.core/stream
   :noah.core/fn-2
   :noah.core/join-windows
   org.apache.kafka.streams.kstream.Joined]
  [this a b c d]
  (.join this a (noah.fn-wrap/value-joiner b) c d))
(defmethod
  join
  [:noah.core/stream
   :noah.core/stream
   :noah.core/fn-2
   :noah.core/join-windows]
  [this a b c]
  (.join this a (noah.fn-wrap/value-joiner b) c))
(defmethod
  join
  [:noah.core/stream
   :noah.core/global-table
   :noah.core/fn-2
   :noah.core/fn-2]
  [this a b c]
  (.join
   this
   a
   (noah.fn-wrap/key-value-mapper b)
   (noah.fn-wrap/value-joiner c)))
(defmethod
  join
  [:noah.core/table :noah.core/table :noah.core/fn-2]
  [this a b]
  (.join this a (noah.fn-wrap/value-joiner b)))
(defmethod
  join
  [:noah.core/table
   :noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.join this a (noah.fn-wrap/value-joiner b) c))
(defmethod
  flat-map-values
  [:noah.core/stream :noah.core/fn-1]
  [this a]
  (.flatMapValues this (noah.fn-wrap/value-mapper a)))
(defmethod
  flat-map-values
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.flatMapValues this (noah.fn-wrap/value-mapper-with-key a)))
(defmethod
  to
  [:noah.core/stream java.lang.String]
  [this a]
  (.to this a))
(defmethod
  to
  [:noah.core/stream :noah.core/fn-3]
  [this a]
  (.to this (noah.fn-wrap/topic-name-extractor a)))
(defmethod
  to
  [:noah.core/stream :noah.core/fn-3 :noah.core/produced]
  [this a b]
  (.to
   this
   (noah.fn-wrap/topic-name-extractor a)
   (noah.core/produced b)))
(defmethod
  to
  [:noah.core/stream java.lang.String :noah.core/produced]
  [this a b]
  (.to this a (noah.core/produced b)))
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
  (.groupByKey this (noah.core/serialized a)))
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
(defmethod
  transform
  [:noah.core/stream
   org.apache.kafka.streams.kstream.TransformerSupplier
   java.lang.String]
  [this a vararg]
  (.transform
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map clojure.core/identity vararg))))
(defmethod
  table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.table this a (noah.core/consumed b) c))
(defmethod
  table
  [org.apache.kafka.streams.StreamsBuilder java.lang.String]
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
  (.table this a (noah.core/consumed b)))
(defmethod
  add-state-store
  [org.apache.kafka.streams.StreamsBuilder
   org.apache.kafka.streams.state.StoreBuilder]
  [this a]
  (.addStateStore this a))
(defmethod
  left-join
  [:noah.core/stream
   :noah.core/stream
   :noah.core/fn-2
   :noah.core/join-windows
   org.apache.kafka.streams.kstream.Joined]
  [this a b c d]
  (.leftJoin this a (noah.fn-wrap/value-joiner b) c d))
(defmethod
  left-join
  [:noah.core/stream :noah.core/table :noah.core/fn-2]
  [this a b]
  (.leftJoin this a (noah.fn-wrap/value-joiner b)))
(defmethod
  left-join
  [:noah.core/stream
   :noah.core/stream
   :noah.core/fn-2
   :noah.core/join-windows]
  [this a b c]
  (.leftJoin this a (noah.fn-wrap/value-joiner b) c))
(defmethod
  left-join
  [:noah.core/stream
   :noah.core/global-table
   :noah.core/fn-2
   :noah.core/fn-2]
  [this a b c]
  (.leftJoin
   this
   a
   (noah.fn-wrap/key-value-mapper b)
   (noah.fn-wrap/value-joiner c)))
(defmethod
  left-join
  [:noah.core/stream
   :noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Joined]
  [this a b c]
  (.leftJoin this a (noah.fn-wrap/value-joiner b) c))
(defmethod
  left-join
  [:noah.core/table
   :noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.leftJoin this a (noah.fn-wrap/value-joiner b) c))
(defmethod
  left-join
  [:noah.core/table :noah.core/table :noah.core/fn-2]
  [this a b]
  (.leftJoin this a (noah.fn-wrap/value-joiner b)))
(defmethod
  filter-not
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.filterNot this (noah.fn-wrap/predicate a)))
(defmethod
  filter-not
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.filterNot this (noah.fn-wrap/predicate a)))
(defmethod
  filter-not
  [:noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.filterNot this (noah.fn-wrap/predicate a) b))
(defmethod
  map-values
  [:noah.core/stream :noah.core/fn-1]
  [this a]
  (.mapValues this (noah.fn-wrap/value-mapper a)))
(defmethod
  map-values
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.mapValues this (noah.fn-wrap/value-mapper-with-key a)))
(defmethod
  map-values
  [:noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.mapValues this (noah.fn-wrap/value-mapper-with-key a) b))
(defmethod
  map-values
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.mapValues this (noah.fn-wrap/value-mapper-with-key a)))
(defmethod
  map-values
  [:noah.core/table
   :noah.core/fn-1
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.mapValues this (noah.fn-wrap/value-mapper a) b))
(defmethod
  map-values
  [:noah.core/table :noah.core/fn-1]
  [this a]
  (.mapValues this (noah.fn-wrap/value-mapper a)))
(defmethod
  through
  [:noah.core/stream java.lang.String]
  [this a]
  (.through this a))
(defmethod
  through
  [:noah.core/stream java.lang.String :noah.core/produced]
  [this a b]
  (.through this a (noah.core/produced b)))
(defmethod
  process
  [:noah.core/stream
   org.apache.kafka.streams.processor.ProcessorSupplier
   java.lang.String]
  [this a vararg]
  (.process
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map clojure.core/identity vararg))))
(defmethod to-stream [:noah.core/table] [this] (.toStream this))
(defmethod
  to-stream
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.toStream this (noah.fn-wrap/key-value-mapper a)))
(defmethod
  flat-transform
  [:noah.core/stream
   org.apache.kafka.streams.kstream.TransformerSupplier
   java.lang.String]
  [this a vararg]
  (.flatTransform
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map clojure.core/identity vararg))))
(defmethod
  print
  [:noah.core/stream org.apache.kafka.streams.kstream.Printed]
  [this a]
  (.print this a))
(defmethod
  outer-join
  [:noah.core/stream
   :noah.core/stream
   :noah.core/fn-2
   :noah.core/join-windows]
  [this a b c]
  (.outerJoin this a (noah.fn-wrap/value-joiner b) c))
(defmethod
  outer-join
  [:noah.core/stream
   :noah.core/stream
   :noah.core/fn-2
   :noah.core/join-windows
   org.apache.kafka.streams.kstream.Joined]
  [this a b c d]
  (.outerJoin this a (noah.fn-wrap/value-joiner b) c d))
(defmethod
  outer-join
  [:noah.core/table :noah.core/table :noah.core/fn-2]
  [this a b]
  (.outerJoin this a (noah.fn-wrap/value-joiner b)))
(defmethod
  outer-join
  [:noah.core/table
   :noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.outerJoin this a (noah.fn-wrap/value-joiner b) c))
(defmethod
  merge
  [:noah.core/stream :noah.core/stream]
  [this a]
  (.merge this a))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder java.util.regex.Pattern]
  [this a]
  (.stream this a))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder
   java.util.Collection
   :noah.core/consumed]
  [this a b]
  (.stream this a (noah.core/consumed b)))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder java.lang.String]
  [this a]
  (.stream this a))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder java.util.Collection]
  [this a]
  (.stream this a))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed]
  [this a b]
  (.stream this a (noah.core/consumed b)))
(defmethod
  stream
  [org.apache.kafka.streams.StreamsBuilder
   java.util.regex.Pattern
   :noah.core/consumed]
  [this a b]
  (.stream this a (noah.core/consumed b)))
(defmethod
  flat-map
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.flatMap this (noah.fn-wrap/key-value-mapper a)))
(defmethod
  global-table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed]
  [this a b]
  (.globalTable this a (noah.core/consumed b)))
(defmethod
  global-table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   :noah.core/consumed
   org.apache.kafka.streams.kstream.Materialized]
  [this a b c]
  (.globalTable this a (noah.core/consumed b) c))
(defmethod
  global-table
  [org.apache.kafka.streams.StreamsBuilder
   java.lang.String
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.globalTable this a b))
(defmethod
  global-table
  [org.apache.kafka.streams.StreamsBuilder java.lang.String]
  [this a]
  (.globalTable this a))
(defmethod
  transform-values
  [:noah.core/stream
   org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
   java.lang.String]
  [this a vararg]
  (.transformValues
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map clojure.core/identity vararg))))
(defmethod
  transform-values
  [:noah.core/stream
   org.apache.kafka.streams.kstream.ValueTransformerSupplier
   java.lang.String]
  [this a vararg]
  (.transformValues
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map clojure.core/identity vararg))))
(defmethod
  transform-values
  [:noah.core/table
   org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
   java.lang.String]
  [this a vararg]
  (.transformValues
   this
   a
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map clojure.core/identity vararg))))
(defmethod
  transform-values
  [:noah.core/table
   org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
   org.apache.kafka.streams.kstream.Materialized
   java.lang.String]
  [this a b vararg]
  (.transformValues
   this
   a
   b
   (clojure.core/into-array
    java.lang.String
    (clojure.core/map clojure.core/identity vararg))))
(defmethod
  build
  [org.apache.kafka.streams.StreamsBuilder]
  [this]
  (.build this))
(defmethod
  build
  [org.apache.kafka.streams.StreamsBuilder java.util.Properties]
  [this a]
  (.build this (noah.core/map->properties a)))
(defmethod
  filter
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.filter this (noah.fn-wrap/predicate a)))
(defmethod
  filter
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.filter this (noah.fn-wrap/predicate a)))
(defmethod
  filter
  [:noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Materialized]
  [this a b]
  (.filter this (noah.fn-wrap/predicate a) b))
(defmethod
  foreach
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.foreach this (noah.fn-wrap/foreach-action a)))
(defmethod
  count
  [:noah.core/stream org.apache.kafka.streams.kstream.Materialized]
  [this a]
  (.count this a))
(defmethod count [:noah.core/stream] [this] (.count this))
(defmethod
  count
  [:noah.core/table org.apache.kafka.streams.kstream.Materialized]
  [this a]
  (.count this a))
(defmethod count [:noah.core/table] [this] (.count this))
(defmethod
  count
  [:noah.core/stream org.apache.kafka.streams.kstream.Materialized]
  [this a]
  (.count this a))
(defmethod count [:noah.core/stream] [this] (.count this))
(defmethod count [:noah.core/stream] [this] (.count this))
(defmethod
  count
  [:noah.core/stream org.apache.kafka.streams.kstream.Materialized]
  [this a]
  (.count this a))
(defmethod
  group-by
  [:noah.core/stream :noah.core/fn-2 :noah.core/serialized]
  [this a b]
  (.groupBy
   this
   (noah.fn-wrap/key-value-mapper a)
   (noah.core/serialized b)))
(defmethod
  group-by
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.groupBy this (noah.fn-wrap/key-value-mapper a)))
(defmethod
  group-by
  [:noah.core/stream
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Grouped]
  [this a b]
  (.groupBy this (noah.fn-wrap/key-value-mapper a) b))
(defmethod
  group-by
  [:noah.core/table
   :noah.core/fn-2
   org.apache.kafka.streams.kstream.Grouped]
  [this a b]
  (.groupBy this (noah.fn-wrap/key-value-mapper a) b))
(defmethod
  group-by
  [:noah.core/table :noah.core/fn-2 :noah.core/serialized]
  [this a b]
  (.groupBy
   this
   (noah.fn-wrap/key-value-mapper a)
   (noah.core/serialized b)))
(defmethod
  group-by
  [:noah.core/table :noah.core/fn-2]
  [this a]
  (.groupBy this (noah.fn-wrap/key-value-mapper a)))
(defmethod
  select-key
  [:noah.core/stream :noah.core/fn-2]
  [this a]
  (.selectKey this (noah.fn-wrap/key-value-mapper a)))
(defmethod
  add-global-store
  [org.apache.kafka.streams.StreamsBuilder
   org.apache.kafka.streams.state.StoreBuilder
   java.lang.String
   :noah.core/consumed
   org.apache.kafka.streams.processor.ProcessorSupplier]
  [this a b c d]
  (.addGlobalStore this a b (noah.core/consumed c) d))
(defmethod
  add-global-store
  [org.apache.kafka.streams.StreamsBuilder
   org.apache.kafka.streams.state.StoreBuilder
   java.lang.String
   java.lang.String
   :noah.core/consumed
   java.lang.String
   org.apache.kafka.streams.processor.ProcessorSupplier]
  [this a b c d e f]
  (.addGlobalStore this a b c (noah.core/consumed d) e f))

