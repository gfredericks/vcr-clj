# vcr-clj

vcr-clj is a clojure library in the spirit of the VCR Ruby library. It lets you
record HTTP interactions as you run your tests, and use the recordings later to
play back the interaction so you can run your tests in a repeatable way without
needing the external components to be available/accessible.

## Usage

Document this library.

## TODO

* Add a dynamic var that determines if new things are recorded (else throw an
  error) -- default to no
* Add a leiningen task for running tests with new recordings
 * It could simply pass its args through to lein-test but wrapped in setting
   the above var to true.

## License

Copyright (C) 2012 Gary Fredericks

Distributed under the Eclipse Public License, the same as Clojure.
