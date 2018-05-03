# vcr-clj

[![Circle CI](https://circleci.com/gh/gfredericks/vcr-clj.svg?style=svg)](https://circleci.com/gh/gfredericks/vcr-clj)

vcr-clj is a general function recording/playback library for Clojure. It is
intended to be used when testing code that does I/O, to achieve several goals:

- Repeatable tests despite unreliable/unpredictable I/O sources
- Tests that can be run without requiring the availability of I/O sources
- Tests that involve realistic data

Any clojure function (er, var) can be recorded and played back.

## Requirements

vcr-clj requires Clojure 1.4 or later.

## Obtention

`[com.gfredericks/vcr-clj "0.4.15"]`

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
    ... do some testy things ...
    ... that call clj-http functions ...))

(deftest this-test-only-records-some-calls
  (with-cassette {:name :foo-2
                  :recordable? (fn [req] (worth-recording? req))}
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

- `:arg-transformer`: A function with the same argument signature as the
  recorded function, which returns a vector of possibly transformed arguments.
  During recording/playback, the original arguments to the function call are
  passed through this transformer, and the transformed arguments are passed to
  `arg-key-fn`, `recordable?` and the recorded function. This can be useful for
  replacing an argument that would be destructively consumed (e.g. a mutable
  `InputStream`) with an indestructible substitute. The transformed arguments
  ought to be equivalent to the original arguments for the purpose of the code
  under test.  The default is `clojure.core/vector`, which just passes along
  the original arguments.
- `:arg-key-fn`: A function with the same argument signature as the recorded
  function, which returns a value for "fingerprinting" the arguments to each
  call. During recording, the value returned by this function will be saved
  along with the recorded call in the cassette. During playback, the value
  returned by this function will be used to look up a matching recorded call in
  the cassette.  The default is `clojure.core/vector`, which just compares the
  arguments as given.
- `:recordable?`: A function with the same argument signature as the recorded
  function, which returns truthy or falsy. If it returns falsy, the call will
  be passed through to the original function during both recording and
  playback. The default is `clojure.core/identity`.
- `:return-transformer`: A single-argument function that takes a value returned
  by the recorded function and returns a transformed return value. During
  recording, the recorded function will be composed with this transformer
  function. This can be useful for ensuring serializability. The transformed
  return value ought to be equivalent to the original return value for the
  purpose of the code under test. The default is `clojure.core/identity`.

## TODO

* Add a better way to re-record than deleting cassette files.
  Maybe an environment variable?

## License

Copyright (C) 2012 Gary Fredericks

Distributed under the Eclipse Public License, the same as Clojure.
