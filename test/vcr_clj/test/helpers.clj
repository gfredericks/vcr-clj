(ns vcr-clj.test.helpers
  (:require [me.raynes.fs :as fs]))

(def delete-cassettes-after-test
  (fn [test]
    (try (test)
         (finally (fs/delete-dir "cassettes")))))
