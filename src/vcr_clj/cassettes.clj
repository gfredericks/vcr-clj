(ns vcr-clj.cassettes
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [fs.core :as fs]
            [vcr-clj.cassettes.serialization :refer [data-readers]]))

(defn cassette-path [cassette-name]
  (let [[name ns] (if (keyword? cassette-name)
                    ((juxt name namespace) cassette-name)
                    [(name cassette-name)])]
    (remove nil? [ns (str name ".clj")])))

(defn cassette-file
  "Returns the File object for a given cassette name. Ensures parent
   directories exist."
  [cassette-name]
  (let [path (cons "cassettes" (cassette-path cassette-name))]
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

(defn- reattach-http-meta
  [cassette]
  (let [set-http-meta #(vary-meta % assoc :type :vcr-clj.clj-http/serializable-http-request)
        update-episode #(update % :return set-http-meta)]
    (update cassette :calls #(mapv update-episode %))))

;; TODO: use clojure.edn?
(defn read-cassette
  [name]
  (let [cassette (with-open [r (java.io.PushbackReader. (io/reader (cassette-file name)))]
                   (edn/read {:readers data-readers} r))]
    (reattach-http-meta cassette)))
