(ns noah.javanation
  "Our Clojure values and functions will need Javanation for this plan to work..."
  (:require noah.transformer)
  (:import
   [org.apache.kafka.common.serialization Serde]
   [org.apache.kafka.streams KafkaStreams StreamsBuilder StreamsConfig Topology$AutoOffsetReset]
   [org.apache.kafka.streams.kstream Aggregator Consumed ForeachAction GlobalKTable Initializer Joined JoinWindows KeyValueMapper ValueMapperWithKey KGroupedStream KGroupedTable KStream KTable Materialized Merger Predicate Produced Reducer Serialized SessionWindowedKStream SessionWindows ValueJoiner ValueMapper Windows Suppressed Grouped TransformerSupplier Transformer ValueTransformerWithKeySupplier ValueTransformerWithKey ValueTransformerSupplier ValueTransformer Printed]
   [org.apache.kafka.streams.kstream.internals KTableImpl KStreamImpl KGroupedStreamImpl]
   [org.apache.kafka.streams.state KeyValueStore StoreBuilder]
   [org.apache.kafka.streams.processor TopicNameExtractor TimestampExtractor StreamPartitioner ProcessorSupplier]
   [java.util Collections Map Properties]
   [noah.transformer NoahTransformer]
   [java.time Duration]))

;; these are the types which are supported by the defwrappers macro
;; some of them will convert the value before passing it to Java
;; the conversion of Clojure functions by arity is handled in noah.fn-wrap
(def conversion-fn
  {Aggregator                      'noah.fn-wrap/aggregator
   Initializer                     'noah.fn-wrap/initializer
   KeyValueMapper                  'noah.fn-wrap/key-value-mapper
   ForeachAction                   'noah.fn-wrap/foreach-action
   Merger                          'noah.fn-wrap/merger
   Predicate                       'noah.fn-wrap/predicate
   Reducer                         'noah.fn-wrap/reducer
   ValueJoiner                     'noah.fn-wrap/value-joiner
   ValueMapper                     'noah.fn-wrap/value-mapper
   ValueMapperWithKey              'noah.fn-wrap/value-mapper-with-key
   TopicNameExtractor              'noah.fn-wrap/topic-name-extractor
   Consumed                        'noah.core/consumed
   Produced                        'noah.core/produced
   Serialized                      'noah.core/serialized
   Serde                           'noah.serdes/serdes
   java.util.Properties            'noah.core/map->properties
   NoahTransformer                 `identity
   GlobalKTable                    `identity
   KGroupedTable                   `identity
   java.util.regex.Pattern         `identity
   String                          `identity
   Joined                          `identity
   JoinWindows                     `identity
   KGroupedStream                  `identity
   KStream                         `identity
   KTable                          `identity
   Materialized                    `identity
   SessionWindowedKStream          `identity
   SessionWindows                  `identity
   TransformerSupplier             `identity
   ValueTransformerSupplier        `identity
   ValueTransformerWithKeySupplier `identity
   Windows                         `identity
   StreamsBuilder                  `identity
   StoreBuilder                    `identity
   TimestampExtractor              `identity
   Topology$AutoOffsetReset        `identity
   StreamPartitioner               `identity
   Duration                        `identity
   java.util.Map                   `identity
   java.util.Collection            `identity
   Suppressed                      `identity
   Grouped                         `identity
   ProcessorSupplier               `identity
   Printed                         `identity
   })
