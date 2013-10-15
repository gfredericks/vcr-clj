(ns vcr-clj.cassettes
  (:require [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [fs.core :as fs]))

(defn str->bytes
  [s]
  (b64/decode (.getBytes s)))

;; TODO: this is specifically needed for the HTTP implementation, so
;; we could probably move the bytes stuff there. Also we could use
;; hiredman's lib instead of doing it ourselves.
(defmethod print-method (type (byte-array 2))
  [ba ^java.io.Writer pw]
  (doto pw
    (.append "#vcr-clj/bytes \"")
    (.append (String. (b64/encode ba)))
    (.append "\"")))

(defn cassette-file
  "Returns the File object for a given cassette name. Ensures parent
   directories exist."
  [cassette-name]
  (doto (fs/file "cassettes" (str (name cassette-name) ".clj"))
    (-> fs/parent fs/mkdirs)))

(defn cassette-exists?
  [name]
  (-> name cassette-file fs/exists?))

(defn write-cassette
  [name cassette]
  (with-open [writer (-> name cassette-file io/writer)]
    (binding [*out* writer]
      (prn cassette))))

;; TODO: use clojure.edn?
(defn read-cassette
  [name]
  (-> name
      cassette-file
      slurp
      read-string))
