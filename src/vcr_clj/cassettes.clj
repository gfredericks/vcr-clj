(ns vcr-clj.cassettes
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [fs.core :as fs]
            [vcr-clj.cassettes.serialization :refer [data-readers]]))

(defn cassette-path [cassette-name]
  (if (keyword? cassette-name)
    (remove nil? ((juxt namespace name) cassette-name))
    [(name cassette-name)]))

(defn cassette-file
  "Returns the File object for a given cassette name. Ensures parent
   directories exist."
  [cassette-name]
  (let [path (concat ["cassettes"] (cassette-path cassette-name) [".clj"])]
    (doto (apply fs/file path)
      (-> fs/parent fs/mkdirs))))

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
