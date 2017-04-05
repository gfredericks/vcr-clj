(ns vcr-clj.clj-http
  "Helpers for using vcr-clj with clj-http. Assumes clj-http (which is
   not an explicit dependency of vcr-clj) has already been required.

   Main entry point is with-cassette, a macro that calls
   vcr-clj.core/with-cassette with args for overwriting
   clj-http.core/request."
  (:require [clojure.data.codec.base64 :as b64]
            [vcr-clj.core :as vcr]
            [vcr-clj.cassettes.serialization :as ser]))


(def default-req-keys
  [:uri :server-name :server-port :query-string :request-method])

(defn assoc-or
  "Returns (assoc m k v) when m does not have the key k."
  [m k v]
  (cond-> m (not (contains? m k)) (assoc k v)))

(try
  (require 'clj-http.headers)
  (let [c (resolve 'clj_http.headers.HeaderMap)]
    (defn clj-http-header-map?
      [x]
      (instance? c x)))
  (catch Throwable t
    (defn clj-http-header-map? [x] false)))

(defmethod print-method ::serializable-http-request
  [req pw]
  (-> req
      (vary-meta dissoc :type)
      (update-in [:headers] (fn [headers]
                              (cond-> headers
                                      (clj-http-header-map? headers)
                                      (ser/serializablize-clj-http-header-map))))
      (print-method pw)))

(defn serializablize
  "Transforms :body when necessary, and changes the :type metadata so
  the request serializes correctly."
  [x]
  (-> x
      (update-in [:body] (fn [body]
                           (cond-> body
                                   (instance? java.io.InputStream body)
                                   (ser/serializablize-input-stream))))
      (vary-meta assoc :type ::serializable-http-request)))

(defn default-arg-key-fn
  [req & more]
  (select-keys req default-req-keys))

(defmacro with-cassette
  "Helper for running a cassette on clj-http.core/request.

  E.g.:

  (with-cassette :recording-some-http-calls
    (do)
    (some)
    (test)
    (things))

  To modify the options passed to vcr-clj.core/with-cassette,
  use this syntax:

  (with-cassette {:name :recording-some-http-calls
                  :recordable? #(re-find #\"foo\" (:uri %))}
    (do)
    (some)
    (test)
    (things))"
  [name-or-opts-map & body]
  (let [[opts body]
        ;; backwards-compatible support for the old syntax of
        ;; (with-cassette :foo {...opts..} ...)
        (cond (and (> (count body) 1)
                   (map? (first body)))
              [(assoc (first body) :name name-or-opts-map)
               (rest body)]

              (or (keyword? name-or-opts-map)
                  (string?  name-or-opts-map))
              [{:name name-or-opts-map} body]

              :else
              [name-or-opts-map body])]
    `(let [opts# ~opts
           opts# (if (map? opts#) opts# {:name opts#})]
       (vcr/with-cassette (:name opts#)
         [(-> opts#
              (dissoc :name)
              (assoc :var (var clj-http.core/request))
              (assoc-or :arg-key-fn default-arg-key-fn)
              (assoc-or :return-transformer serializablize))]
         ~@body))))
