(ns vcr-clj.test.helpers
  (:require [fs.core :as fs]))

(def delete-cassettes-after-test
  (fn [test]
    (try (test)
         (finally (fs/delete-dir "cassettes")))))
