(ns vcr-clj.test.core
  (:use [vcr-clj.core])
  (:use [clojure.test])
  (:require [ring.adapter.jetty :as jetty]
            [clj-http.client :as client]
            [fs.core :as fs]))

(def delete-cassettes-after-test
  (fn [test]
    (try (test)
         (finally (fs/delete-dir "cassettes")))))

(use-fixtures :each delete-cassettes-after-test)

(def ^:dynamic *server-requests* nil)
(defn server-requests [] @*server-requests*)

(defn with-jetty-server-fn
  [ring-server func]
  (let [a (atom [])
        ring-server (fn [req]
                      (swap! a conj req)
                      (ring-server req))
        server (jetty/run-jetty ring-server {:join? false :port 28366})]
    (try (binding [*server-requests* a]
           (func))
         (finally
           (.stop server)))))

(defmacro with-jetty-server
  [server & body]
  `(with-jetty-server-fn ~server (fn [] ~@body)))

(def hehe-okay-server (constantly {:body "Hehe okay" :status 200 :headers {}}))

(deftest basic-test
  (with-jetty-server hehe-okay-server
    (is (not (fs/exists? "cassettes/foo.clj")))
    (let [f (fn []
              (with-cassette :foo
                (->
                 (client/get "http://localhost:28366/haha")
                 :body
                 (= "Hehe okay")
                 (is))))]
      (is (empty? (server-requests)))
      (f)
      (is (fs/exists? "cassettes/foo.clj"))
      (is (= 1 (count (server-requests))))
      (f)
      (is (fs/exists? "cassettes/foo.clj"))
      (is (= 1 (count (server-requests)))))))