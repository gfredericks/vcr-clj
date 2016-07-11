# vcr-clj

vcr-clj is a general function recording/playback library for Clojure. It is
intended to be used when testing code that does I/O, to achieve several goals:

- Repeatable tests despite unreliable/unpredictable I/O sources
- Tests that can be run without requiring the availability of I/O sources
- Tests that involve realistic data

Any clojure function (er, var) can be recorded and played back.

## Requirements

vcr-clj requires Clojure 1.4 or later.

## Obtention

`[com.gfredericks/vcr-clj "0.4.8"]`

## Usage

An example with `clojure.test`:

``` clojure
(ns my.project-test
  (:require [clojure.test :refer :all]
            [vcr-clj.core :refer [with-cassette]]))

(deftest here-is-my-test
  (with-cassette :foo [{:var #'my.io.ns/get-data, ...extra options...}]
    ... do some testy things ...
    ... that call my.io.ns/get-data ...))

```

There is also currently a separate namespace for recording `clj-http` requests
in particular:

``` clojure
(ns my.project-test
  (:require [clojure.test :refer :all]
            [vcr-clj.clj-http :refer [with-cassette]]))

(deftest here-is-my-webby-test
  (with-cassette :foo
    ;; can optionally include an options map here
    ... do some testy things ...
    ... that call clj-http functions ...))

```

The first time you run a `with-cassette` block, a cassette file is
created in the `/cassettes` directory. Each subsequent time, playback
is performed using the cassette in the directory. You can delete it to
force a re-record.

Namespaced keywords can be used to group cassettes in the filesystem.

### Customizing

Each var that is recorded can be customized with options:

- `:arg-key-fn`: a function with the same arg signature as the recorded
                 fn, which returns a value that the call will be
                 compared to during playback.  This defaults to
                 `vector`, which just compares the args as given.
- `:recordable?`: a function with the same arg signature as the recorded
                  fn; if it returns falsy, the call will be passed to
                  through to the original function both during recording
                  and playback.

## TODO

* Add a better way to re-record than deleting cassette files.
  Maybe an environment variable?

## License

Copyright (C) 2012 Gary Fredericks

Distributed under the Eclipse Public License, the same as Clojure.
