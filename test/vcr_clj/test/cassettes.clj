(ns vcr-clj.test.cassettes
  (:require [vcr-clj.cassettes :as cassettes]
            [vcr-clj.test.helpers :as help]
            [me.raynes.fs :as fs]
            [clojure.test :refer :all]))

(use-fixtures :each help/delete-cassettes-after-test)

(deftest cassette-path-test
  (are [input expected] (= (cassettes/cassette-path input) expected)
    "foobar" ["foobar.edn"]
    :foobar ["foobar.edn"]
    ::foobar ["vcr-clj.test.cassettes" "foobar.edn"]))

(deftest legacy-file-extension-support
  (fs/mkdirs "cassettes")
  (fs/touch "cassettes/old-kind.clj")
  (fs/touch "cassettes/new-kind.edn")
  (is (re-matches #".*/cassettes/old-kind.clj"
                  (str (cassettes/cassette-file "old-kind"))))
  (is (re-matches #".*/cassettes/new-kind.edn"
                  (str (cassettes/cassette-file "new-kind"))))
  (is (re-matches #".*/cassettes/new-kind-new-file.edn"
                  (str (cassettes/cassette-file "new-kind-new-file")))))
