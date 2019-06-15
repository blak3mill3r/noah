[![streams animation][streams]](http://erdalinci.com/)

# noah
> (makes ark fast)
>
> A simple Clojure interface to the Kafka Streams API

[![Build Status](https://travis-ci.org/blak3mill3r/noah.svg?branch=master)](https://travis-ci.org/blak3mill3r/noah)
[![codecov](https://codecov.io/gh/blak3mill3r/noah/branch/master/graph/badge.svg)](https://codecov.io/gh/blak3mill3r/noah)
[![Clojars Project](https://img.shields.io/clojars/v/noah.svg)](https://clojars.org/noah)

This is not on Clojars yet.

```clj
blak3mill3r/noah {:git/url "https://github.com/blak3mill3r/noah" :sha "look it up"}
```

## Why?

Kafka and Clojure seem like natural companions, both emphasizing immutability and the primacy of data.

#### What's inside?

* A Clojure interface for building topologies and running an app using the high-level Streams API
  * Clojure functions work on streams/tables
  * maps to provide options
  * surprisingly complete (it is generated reflectively)
* A few serialization choices including [edn](https://github.com/edn-format/edn) and [nippy](https://github.com/ptaoussanis/nippy)
* Test utilities exposing `TopologyTestDriver`
* A macro to ease the definition of a `Transformer`
  * Write access to `StateStore`s
  * Punctuations
* Transducers
  * Fault-tolerant stateful transducers

## Status

Working (with `2.0` cluster and `2.2` client libraries). Probably more interesting than useful at this point. This is a young project which is far from battle-tested and there are some inadequacies.

### Kicking the tires

Clone this repo, then try

```bash
clj aot.clj
clj -A:test:runner
```

### Usage

See the [intro](doc/intro.md) and the [tests](test/noah/).

At this time no example app code is provided.

:warning: If you want to make an app, you will want to AOT at least `noah.serdes`

-------------

### Inadequacies

* There are some gaps in the wrapping, where you would still have to use ugly interop
  * Good news: *all signatures of all public methods are exposed*
  * Bad news: some types have no sugar; you should be able to get an idea from the docstrings
* No sugar for `Processor`, only `Transformer`
* Stateful transducers must be rewritten in order to instrument their state construction
* can't pass reader options for edn serialization, nor can you pass nippy options (well, you can, but it's DIY)
* probably a lot more, feel free to point them out

### Reporting bugs

Please open an issue on Github if you spot something amiss.

### Contributing

If you find it useful or interesting, I would love to hear about it. PRs are very welcome. It would be great to see a whole lot more transducers get added. I would also welcome feedback and discussion.

### Acknowledgements

A tip of the hat is graciously offered to Bobby Calderwood, in whose experiments with [KStream transducers](https://github.com/bobby/kafka-streams-clojure) I found much inspiration.

### License

Copyright © 2018, 2019 Blake Miller

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[streams]: streams.gif
