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

`[com.gfredericks/vcr-clj "0.4.16"]`

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

### Cassette Customization

Instead of invoking `with-cassette` with a name, you may invoke it with a map
defining additional cassette data:

- `:name`: the only required key in the map, this defines the name of the
cassette as previously described.
- `:serialization`: an optional map defining settings for controlling how
the cassette is serialized and deserialized.

#### De/serialization

vcr-clj uses [Puget](https://github.com/greglook/puget) for storing cassettes
on disk. The `:serialization` cassette key allows clients to customize the
default configuration. The options available are:

- `:print-handlers`: a function that replaces the
[built-in function](https://github.com/gfredericks/vcr-clj/blob/e8efe21de72e001e846aacd241f8ae2aaacb4f55/src/vcr_clj/cassettes/serialization.clj#L101)
for converting the cassette output to serializable data. See
[Puget's documentation](https://github.com/greglook/puget#type-extensions) for
more details
- `:data-readers`: map that merges over
[the defaults](https://github.com/gfredericks/vcr-clj/blob/e8efe21de72e001e846aacd241f8ae2aaacb4f55/src/vcr_clj/cassettes/serialization.clj#L96).
This mapping determines how specific symbols in the saved cassette are
converted back to the original data.

The following example prints the raw bytes from a byte array instead of
using the default base64 encoding.

``` clojure
(ns my.project-test
  (:require [clojure.test :refer :all]
            [puget.printer :as printer]
            [vcr-clj.cassettes.serialization :as vcr-ser]
            [vcr-clj.core :as vcr]))

(def byte-array-class
  "Standard Java class for byte arrays"
  (class (byte-array 0)))

(defn extended-print-handlers
  "Print handler for vcr-clj library. Enables support of additional object
  instances alongside vcr-clj defaults."
  [cls]
  (if (isa? cls byte-array-class)
    (printer/tagged-handler
      'my.project/printable-bytes
      (fn [data]
        (vcr-ser/split-bytes data 75)))

    (vcr-ser/default-print-handlers cls)))

(deftest here-is-my-bytes-test
  (with-cassette {:name :testaroo
                  :serialization {:print-handlers extended-print-handlers
                                  :data-readers {'my.project/printable-bytes (comp (fn [string] (.getBytes string))
                                                                                   vcr-ser/maybe-join)}}}
    ... do some testy things ...
    ... that will return byte arrays ...))
```

## TODO

* Add a better way to re-record than deleting cassette files.
  Maybe an environment variable?

## License

Copyright (C) 2012 Gary Fredericks

Distributed under the Eclipse Public License, the same as Clojure.
