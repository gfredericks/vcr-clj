(ns vcr-clj.core
  (:use [clojure.pprint :only [pprint]])
  (:require [fs.core :as fs]
            [clojure.walk :as wk]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            clj-http.core))

;; * TODO
;; ** Handle streams

(defn- update
  [m k f]
  (update-in m [k] f))

(defn str->bytes
  [s]
  (b64/decode (.getBytes s)))

(defmethod print-method (type (byte-array 2))
  [ba pw]
  (.append pw (str "#vcr-clj.core/bytes \""))
  (.append pw (String. (b64/encode ba)))
  (.append pw "\""))

(defn- write-cassette
  [file cassette]
  (let [writer (io/writer file)]
    (binding [*out* writer]
      (prn cassette))))

(defn- read-cassette
  [file]
  (->> file
       slurp
       read-string))

(def req-keys
  [:uri :server-name :server-port :query-string :request-method])

(defn- fake-request
  "Given a cassette, returns a replacement (stateful) request function."
  [cassette]
  (let [remaining (ref cassette)]
    (fn [req]
      (dosync
       (let [req-key (select-keys req req-keys)
             resp (first (@remaining req-key))]
         (when-not resp
           (throw (Exception. (str "No response in vcr-clj cassette for request: " (pr-str req)))))
         (alter remaining update req-key rest)
         resp)))))

(defn- record
  [func]
  (let [orig-request clj-http.core/request
        responses (atom {})]
    (with-redefs [clj-http.core/request (fn [req] (let [resp (orig-request req)
                                                        req-key (select-keys req req-keys)]
                                                    (swap! responses
                                                           update
                                                           req-key
                                                           (fn [x] (conj (or x []) resp)))
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
