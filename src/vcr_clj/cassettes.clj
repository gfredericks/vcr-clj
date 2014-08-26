(ns vcr-clj.cassettes
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [fs.core :as fs]
            [vcr-clj.cassettes.serialization :refer [data-readers]]))

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
  (with-open [r (java.io.PushbackReader. (io/reader (cassette-file name)))]
    (edn/read {:readers data-readers} r)))
