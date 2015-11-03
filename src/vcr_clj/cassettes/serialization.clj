(ns vcr-clj.cassettes.serialization
  "Code for printing and reading things."
  (:require [clojure.data.codec.base64 :as b64]))

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

(defn str->bytes
  [s]
  (b64/decode (.getBytes s)))

(defmethod print-method (type (byte-array 2))
  [ba ^java.io.Writer pw]
  (doto pw
    (.append "#vcr-clj/bytes \"")
    (.append (String. (b64/encode ba)))
    (.append "\"")))

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
  [x ^java.io.Writer pw]
  (doto pw
    (.write "#vcr-clj/input-stream \"")
    (.write (String. (b64/encode @x)))
    (.write "\"")))

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

(def data-readers
  {'vcr-clj/bytes               str->bytes
   'vcr-clj/input-stream        read-input-stream
   'vcr-clj/clj-http-header-map read-clj-http-header-map})
