(ns vcr-clj.cassettes.serialization
  "Code for printing and reading things."
  (:require [clojure.data.codec.base64 :as b64]
            [clojure.string :as string]))

;; Support serialization for the HeaderMap type when it is around
(try (require 'clj-http.headers)
     (let [constructor @(resolve 'clj-http.headers/header-map)]
       (defmethod print-method ::clj-http-header-map
         [{hm :m} pw]
         (.write pw "#vcr-clj/clj-http-header-map (")
         (doseq [k (keys hm)
                 :let [v (get hm k)]]
           (.write pw " ")
           (print-method k pw)
           (.write pw " ")
           (print-method v pw))
         (.write pw ")"))
       (defn read-clj-http-header-map
         [args]
         (apply constructor args))
       (defn serializablize-clj-http-header-map
         "Given a clj-http HeaderMap object, returns a wrapper that will
         serialize to a tagged edn representation that roundtrips back to
         a HeaderMap."
         [m]
         (with-meta {:m m} {:type ::clj-http-header-map})))
     (catch Throwable t))

(defn split-bytes [^bytes ba maxl]
  (let [l (alength ba)]
    (persistent!
      (loop
        [start-index 0
         acc (transient [])]
        (if (>= start-index l)
          acc
          (recur
            (+ start-index maxl)
            (conj! acc (String. ba start-index (min maxl (- l start-index))))))))))

(defn str->bytes
  [s]
  (if (empty? s)
    (.getBytes "")
    (b64/decode (.getBytes s))))

(defn write-bytes [ba ^java.io.Writer pw ^String tag]
  (doto pw
    (.append tag)
    (.append " [\n"))
  (doseq [line (split-bytes (b64/encode ba) 75)]
    (print-method line pw)
    (.append pw \newline))
  (.append pw "]"))

(defmethod print-method (type (byte-array 2))
  [ba pw]
  (write-bytes ba pw "#vcr-clj/bytes"))

(defn slurp-bytes
  "Consumes an input stream and returns a byte array of its contents."
  [^java.io.InputStream is]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (loop [b (.read is)]
      (if (neg? b)
        (.toByteArray baos)
        (do (.write baos b)
            (recur (.read is)))))))
;;
;; InputStream serializability -- we convert InputStreams to a special
;; kind of ByteArrayInputStream that serializes without having to
;; consume itself.
;;

(defn serializablize-input-stream
  [input-stream]
  (let [bytes (-> input-stream slurp-bytes)]
    (proxy [java.io.ByteArrayInputStream clojure.lang.IDeref clojure.lang.IMeta] [bytes]
      ;; we implement IDeref as a simple way of allowing print-method
      ;; to pull the bytes out
      (deref [] bytes)
      ;; exposing :type metadata allows us to define custom behavior
      ;; for the print-method multimethod below
      (meta [] {:type ::serializable-input-stream}))))

(defmethod print-method ::serializable-input-stream
  [x pw]
  (write-bytes @x pw "#vcr-clj/input-stream"))

(defn read-input-stream
  [hex-str]
  (-> hex-str
      .getBytes
      ;; data.codec cannot roundtrip an empty string
      ;; through a base64 encode/decode
      ;; http://dev.clojure.org/jira/browse/DCODEC-4
      (cond-> (seq hex-str) b64/decode)
      java.io.ByteArrayInputStream.
      serializablize-input-stream))

(defn maybe-join [s]
  (if (string? s) s (string/join s)))

(def data-readers
  {'vcr-clj/bytes               (comp str->bytes maybe-join)
   'vcr-clj/input-stream        (comp read-input-stream maybe-join)
   'vcr-clj/clj-http-header-map read-clj-http-header-map})
