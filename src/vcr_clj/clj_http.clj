(ns vcr-clj.clj-http
  "Helpers for using vcr-clj with clj-http. Assumes clj-http (which is
   not an explicit dependency of vcr-clj) has already been required.

   Main entry point is with-cassette, a macro that calls
   vcr-clj.core/with-cassette with args for overwriting
   clj-http.core/request."
  (:require [clojure.data.codec.base64 :as b64]
            [vcr-clj.core :as vcr]
            [vcr-clj.cassettes.serialization :refer [serializablize-input-stream]]))

(def default-req-keys
  [:uri :server-name :server-port :query-string :request-method])

(defn assoc-or
  "Returns (assoc m k v) when m does not have the key k."
  [m k v]
  (cond-> m (not (contains? m k)) (assoc k v)))


(defn serializablize-body
  "When x has a :body entry that is an input-stream, reads its
   contents and converts it to a serializable kind of
   ByteArrayInputStream."
  [x]
  (cond-> x
          (instance? java.io.InputStream (:body x))
          (update-in [:body] serializablize-input-stream)))

(defmacro with-cassette
  "Helper for running a cassette on clj-http.core/request. Optionally
   takes an options map as the second arg, to supply extra keys to
   the spec map passed to vcr-clj.core/with-cassette."
  [name & body]
  (let [[opts body] (if (and (> (count body) 1)
                             (map? (first body)))
                      [(first body) (rest body)]
                      [{} body])]
    `(vcr/with-cassette ~name
       [(-> ~opts
            (assoc :var (var clj-http.core/request))
            (assoc-or :arg-key-fn (fn [req#] (select-keys req# default-req-keys)))
            (assoc-or :return-transformer serializablize-body))]
       ~@body)))
