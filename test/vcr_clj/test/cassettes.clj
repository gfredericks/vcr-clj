(ns vcr-clj.test.cassettes
  (:require [vcr-clj.cassettes :refer [cassette-path]]
            [clojure.test :refer :all]))

(deftest cassette-path-test
  (are [input expected] (= (cassette-path input) expected)
    "foobar" ["foobar"]
    :foobar ["foobar"]
    ::foobar ["vcr-clj.test.cassettes" "foobar"]))
