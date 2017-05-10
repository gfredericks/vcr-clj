(ns vcr-clj.test.cassettes.serialization
  (:require [clojure.edn :as edn]
            [clojure.test :refer :all]
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

(deftest can-read-old-header-map-format
  (when-let [c (try
                 (Class/forName "clj_http.headers.HeaderMap")
                 (catch Exception e false))]
    (with-open [r (java.io.PushbackReader.
                   (java.io.StringReader.
                    "#vcr-clj/clj-http-header-map
                   (\"a\" \"b\" \"c\" \"d\")"))]
      (binding [serialization/*warn?* false]
        (let [hm (edn/read {:readers serialization/data-readers} r)]
          (is (instance? c hm))
          (is (= {"a" "b" "c" "d"} (into {} hm))))))))
