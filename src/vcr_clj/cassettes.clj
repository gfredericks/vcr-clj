(ns vcr-clj.cassettes
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [vcr-clj.cassettes.serialization :as serialization])
  (:import (java.io File)))

(defn cassette-path
  ([cassette-name] (cassette-path cassette-name "edn"))
  ([cassette-name extension]
   (let [[name ns] (if (keyword? cassette-name)
                     ((juxt name namespace) cassette-name)
                     [(name cassette-name)])]
     (remove nil? [ns (str name "." extension)]))))

(defn cassette-file
  "Returns the File object for a given cassette name. Ensures parent
   directories exist. Returns a file with the legacy .clj extension if
   it exists."
  [cassette-name]
  (let [path (cons "cassettes" (cassette-path cassette-name))
        file (doto (apply fs/file path)
               (-> fs/parent fs/mkdirs))]
    (if (.exists ^File file)
      file
      (let [legacy-file (apply fs/file (cons "cassettes" (cassette-path cassette-name
                                                                        "clj")))]
        (if (.exists ^File legacy-file)
          legacy-file
          file)))))

(defn cassette-exists?
  [name]
  (-> name cassette-file fs/exists?))

(defn write-cassette
  [name cassette & [opts]]
  (with-open [writer (-> name cassette-file io/writer)]
    (binding [*out* writer]
      (serialization/print cassette opts))))

(defn read-cassette
  [name & [{:keys [data-readers]
            :or {data-readers {}}}]]
  (with-open [r (java.io.PushbackReader. (io/reader (cassette-file name)))]
    (edn/read {:readers (merge serialization/data-readers data-readers)} r)))
