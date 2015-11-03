(ns vcr-clj.test.cassettes.serialization
  (:require [clojure.test :refer :all]
            [vcr-clj.cassettes.serialization :refer :all]))

(deftest can-read-input-stream
  (testing "simple base64 encoded string"
    (let [istream (read-input-stream "aGVsbG8=")]
      (is (= "hello" (slurp istream)))))
  (testing "empty string"
    (let [istream (read-input-stream "")]
      (is (= "" (slurp istream))))))
