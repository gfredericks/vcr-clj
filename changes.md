# vcr-clj changes

## 0.4.16 2018-07-02

- Changes default cassette file extension from .clj to .edn; old files
  should still work.

## 0.4.15 2018-05-03

- Fixed [bug](https://github.com/gfredericks/vcr-clj/issues/25) that
  manifests when using connection managers, due to the input stream
  not being closed (would hang when recording)
- Fixed bug that affects clj-http 3.9 (maybe also 3.8?) due to the new
  `:http-client` key in responses

## 0.4.14 2017-05-09

- Add backwards-compatible support (with a warning) for the old
  cassette format so users upgrading from <= 0.4.12 won't be forced to
  re-record.

## 0.4.13 2017-05-08

- Start pretty-printing cassettes with the
  [puget](https://github.com/greglook/puget/) library.
  - also stops extending `print-method` for byte arrays
    which was a terrible idea
- Write `:recorded-at` key to the cassette
- Upgrade the `fs` library
- Cassette data format changed slightly:
  #vcr-clj/clj-http-header-map values are now maps instead
  of sequences

## 0.4.12 2017-04-18

Add `clj-http.vcr-clj/default-spec`.

## 0.4.11 2017-04-17

Adds an `:arg-transformer` option, similar to `:return-transformer`.

## 0.4.10 2017-04-05

Fix #16, which caused problems when trying to record calls made on
other threads.

Fixed #19, adding a new syntax for specifying options in
`vcr-clj.clj-http/with-cassette`.

Fixed #18, which caused problems for clj-http usage when selectively
recording.

## 0.4.9 2017-02-11

Fix #14: don't record self-calls, or any nested calls

This is particularly useful for newer versions of clj-http where the
`request` function has a self-calling arity.

## 0.4.8

Allow namespaced keywords ([#12](https://github.com/gfredericks/vcr-clj/pull/12)).

## 0.4.7

Serialize long byte arrays on multiple lines ([#10](https://github.com/gfredericks/vcr-clj/pull/10)).

## 0.4.6

Fixed another bug [#9](https://github.com/gfredericks/vcr-clj/pull/9)
similar to the one in the previous release.

## 0.4.5

Fixed bug [#8](https://github.com/gfredericks/vcr-clj/pull/8), in which empty http bodies didn't work.

## 0.4.4

Same as 0.4.3, but for real this time.

## 0.4.3

Fix compilation warnings from clojure 1.7 w.r.t. `clojure.core/update`.

## 0.4.2

Include the arg key when no recordings are found.

## 0.4.1

Release. `2015-02-14`.

Fixed issues #2 and #4, related to newer versions of clj-http and its
special HeaderMap type.

## 0.4.0

Released `2014-08-26`.

No longer prints by default during test runs, and does not use a
`data_readers.clj` file.
