(ns vcr-clj.test.cassettes.serialization
  (:require [clojure.test :refer :all]
            [vcr-clj.cassettes.serialization :as serialization])
  (:import (java.util Arrays)))

(deftest can-read-input-stream
  (testing "simple base64 encoded string"
    (let [istream (serialization/read-input-stream "aGVsbG8=")]
      (is (= "hello" (slurp istream)))))
  (testing "empty string"
    (let [istream (serialization/read-input-stream "")]
      (is (= "" (slurp istream))))))

(deftest can-read-base64-bytes
  (testing "Works with empty data"
    (is (true? (Arrays/equals (.getBytes "") (serialization/str->bytes "")))))
  (testing "Works with standard data"
    (is (true? (Arrays/equals  (.getBytes "testing") (serialization/str->bytes "dGVzdGluZw=="))))))
