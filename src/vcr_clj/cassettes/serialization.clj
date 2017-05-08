(ns vcr-clj.cassettes.serialization
  "Code for printing and reading things."
  (:refer-clojure :exclude [print])
  (:require [clojure.data.codec.base64 :as b64]
            [clojure.string :as string]
            [puget.printer :as printer]))

;; Support serialization for the HeaderMap type when it is around
(def ^:private clj-http-header-class?
  (try (require 'clj-http.headers)
       (let [constructor @(resolve 'clj-http.headers/header-map)]
         (defn read-clj-http-header-map
           [m]
           (apply constructor (apply concat m))))
       true
       (catch Throwable t
         false)))

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

(def print-handlers
  (some-fn
   (cond-> {(class (byte-array 0))
            (printer/tagged-handler
             'vcr-clj/bytes
             (fn [ba]
               (split-bytes (b64/encode ba) 75)))}
     clj-http-header-class?
     (merge
      (eval '{clj_http.headers.HeaderMap
              (printer/tagged-handler
               'vcr-clj/clj-http-header-map
               #(into {} %))})))
   (fn [cls]
     (if (isa? cls java.io.ByteArrayInputStream)
       (printer/tagged-handler
        'vcr-clj/input-stream
        (fn [is]
          (when-not (= ::serializable-input-stream (type is))
            (throw (Exception. "Unexpected ByteArrayInputStream!")))
          (split-bytes (b64/encode @is) 75)))))))

(def ^:private puget-options
  {:escape-types       nil,
   :map-coll-separator " ",
   :map-delimiter      ",",
   :print-color        false,
   :print-fallback     :print,
   :print-handlers     print-handlers
   :print-meta         false,
   :sort-keys          true,
   :strict             false,
   :width              80})

(defn print
  [cassette]
  (printer/pprint cassette puget-options))
