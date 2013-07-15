(ns vcr-clj.cassettes
  (:require [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]))

(defn str->bytes
  [s]
  (b64/decode (.getBytes s)))

;; TODO: this is specifically needed for the HTTP implementation, so
;; we could probably move the bytes stuff there. Also we could use
;; hiredman's lib instead of doing it ourselves.
(defmethod print-method (type (byte-array 2))
  [ba pw]
  (doto pw
    (.append "#vcr-clj/bytes \"")
    (.append (String. (b64/encode ba)))
    (.append "\"")))

(defn write-cassette
  [file cassette]
  (let [writer (io/writer file)]
    (binding [*out* writer]
      (prn cassette))))

;; TODO: use clojure.edn?
(defn read-cassette
  [file]
  (->> file
       slurp
       read-string))
