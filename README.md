# noah
> (makes ark fast)
>
> A simple Clojure interface to the Kafka Streams API

[![Build Status](https://travis-ci.org/blak3mill3r/noah.svg?branch=master)](https://travis-ci.org/blak3mill3r/noah)
[![codecov](https://codecov.io/gh/blak3mill3r/noah/branch/master/graph/badge.svg)](https://codecov.io/gh/blak3mill3r/noah)
[![Clojars Project](https://img.shields.io/clojars/v/noah.svg)](https://clojars.org/noah)

```clj
[noah "0.0.0"]
```

## Why?

Kafka and Clojure seem like natural companions, both emphasizing immutability. Kafka Streams is "a client library for building applications and microservices, where the input and output data are stored in Kafka cluster".

With Clojure's Java interop, it's pretty straightforward to implement a Kafka Streams application in Clojure. It's just cumbersome and gross, littered with plenty of `proxy`s to provide something like a `KeyValueMapper`, just to `map` some Clojure code over a stream. Let us abstract over the Javaness of the API, while not diverging from that API very dramatically, so that we can just use Clojure functions and data with a minimum of fuss.

This may serve as a starting point for something better, like a pure-data representation of topologies and two-way translation from Clojure data, but first there are some simple interface wrappers which assume very little and clean things up a lot.

## It looks like this

```clojure
(require '[noah.core :as n])
(let [b (n/streams-builder)
        text (-> b (n/stream "text" #:serdes{:k :string :v :string}))]
    (-> text
        (n/flat-map-values #(str/split % #"\s+"))
        (n/group-by #(-> %2))
        n/count
        n/to-stream
        (n/to "word-counts" #:serdes{:k :string :v :long}))
    (n/build b))
```

## Goals

* Correctness
* Simplicity
* Common things should be simple and intuitive for Clojure users
* Uncommon things should be possible (the library should not hide anything about the underlying API)

## Status

Largely untested, probably dangerous, but working (with `2.0` cluster and client libraries).

## Usage

Well, you'll need to declare a dependency on `noah` in your `project.clj`

```clojure
[blak3mill3r/noah "0.0.0"]
```

as well as a dependency on the Kafka Streams client library, and potentially also the Kafka client library. The version is up to you, but `noah` uses Java reflection to generate the wrapper code with a macro, and this macro was expanded reflecting against the `2.0.0` version.

```clojure
[org.apache.kafka/kafka-streams "2.0.0"]
[org.apache.kafka/kafka-clients "2.0.0"]
```

## License

Copyright © 2018 Blake Miller

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
