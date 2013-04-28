(ns vcr-clj.core
  (:use [clojure.pprint :only [pprint]])
  (:require [fs.core :as fs]
            [clj-http.core]
            [vcr-clj.cassettes :refer [read-cassette
                                       write-cassette]]))

(defn- update
  [m k f]
  (update-in m [k] f))

;; * TODO
;; ** Handle streams

(def req-keys
  [:uri :server-name :server-port :query-string :request-method])

;; These vars are the primary method for customizing the behavior of
;; vcr-clj.
(def ^:dynamic record?
  "Predicate which, given a ring request, determines if it should
  be recorded or passed through."
  (constantly true))

(def ^:dynamic req-key
  "Given a ring request, returns a key that it should be grouped
  under. Requests are allowed to come out of order as long as
  they are in-order with respect to other requests with the same
  key."
  #(select-keys % req-keys))

(defn handle-req-by-pred
  [orig-handler modified-handler request]
  ((if (record? request) modified-handler orig-handler)
   request))

(defn- fake-request
  "Given a cassette, returns a replacement (stateful) request function."
  [orig-handler cassette]
  (let [remaining (ref cassette)
        modified-handler
        (fn [req]
          (dosync
           (let [key (req-key req)
                 resp (first (@remaining key))]
             (when-not resp
               (throw (Exception. (str "No response in vcr-clj cassette for request: " (pr-str req)))))
             (alter remaining update key rest)
             resp)))]
    (partial handle-req-by-pred orig-handler modified-handler)))

(defn- record
  [func]
  (let [orig-request clj-http.core/request
        responses (atom {})
        modified-handler
        (fn [req]
          (let [resp (orig-request req)
                key (req-key req)]
            (swap! responses
                   update
                   key
                   (fn [x] (conj (or x []) resp)))
            resp))]
    (with-redefs [clj-http.core/request (partial handle-req-by-pred orig-request modified-handler)]
      (func))
    @responses))

(defn- run-with-existing-cassette
  [cassette func]
  (let [orig clj-http.core/request]
    (with-redefs [clj-http.core/request (fake-request orig cassette)]
      (func))))

(defn- cassette-file
  [cassette-name]
  (let [f (fs/file "cassettes" (str (name cassette-name) ".clj"))]
    (-> f fs/parent fs/mkdirs)
    f))

(defn with-cassette-fn*
  [cassette-name func]
  (let [f (cassette-file cassette-name)]
    (if (fs/exists? f)
      (do
        (println "Running test with existing" cassette-name "cassette...")
        (run-with-existing-cassette (read-cassette f) func))
      (do
        (println "Recording new" cassette-name "cassette...")
        (let [recording (record func)]
          (println "Serializing...")
          (write-cassette f recording))))))

(defmacro with-cassette
  [cname & body]
  `(with-cassette-fn* ~cname (fn [] ~@body)))
