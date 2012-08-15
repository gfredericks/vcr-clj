(ns vcr-clj.core
  (:use [clojure.pprint :only [pprint]])
  (:require [fs.core :as fs]
            [clojure.walk :as wk]
            [clojure.java.io :as io]
            clj-http.core))

;; * TODO
;; ** Handle streams
;; ** Actually check that response matches request. Maybe allow out-of-order.

(defn- write-cassette
  [file cassette]
  (let [writer (io/writer file)]
    (binding [*out* writer]
      (prn
       (for [resp cassette]
         (update-in resp [:body] #(String. %)))))))

(defn- read-cassette
  [file]
  (for [resp (-> file slurp read-string)]
    (update-in resp [:body] #(.getBytes %))))

(defn- fake-request
  "Given a cassette, returns a replacement (stateful) request function."
  [cassette]
  ;; For first try, just ignore the request and return the responses in order
  (let [remaining (atom cassette)]
    (fn [req]
      (let [resp (first @remaining)]
        (swap! remaining rest)
        resp))))

(defn- record
  [func]
  (let [orig-request clj-http.core/request
        responses (atom [])]
    (with-redefs [clj-http.core/request (fn [req] (let [resp (orig-request req)]
                                                    (swap! responses conj resp)
                                                    resp))]
      (func))
    @responses))

(defn- run-with-existing-cassette
  [cassette func]
  (with-redefs [clj-http.core/request (fake-request cassette)]
    (func)))

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
