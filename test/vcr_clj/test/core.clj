(ns vcr-clj.test.core
  (:refer-clojure :exclude [get])
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

(def hehe-okay-server (constantly {:body "haha"
                                   :status 200
                                   :headers {}}))

(defn get
  [path]
  (-> (str "http://localhost:28366" path)
      (client/get)
      (:body)))

(deftest basic-test
  (with-jetty-server hehe-okay-server
    (is (not (fs/exists? "cassettes/foo.clj")))
    (let [f (fn []
              (with-cassette :foo
                (is (= "haha" (get "/haha")))))]
      (is (empty? (server-requests)))
      (f)
      (is (fs/exists? "cassettes/foo.clj"))
      (is (= 1 (count (server-requests))))
      (f)
      (is (fs/exists? "cassettes/foo.clj"))
      (is (= 1 (count (server-requests)))))))

(defn echo-server
  [req]
  {:status 200 :headers {} :body (subs (:uri req) 1)})

(deftest different-order-requests-test
  (with-jetty-server echo-server
    (with-cassette :bar-bar
      (is (= "foo" (get "/foo")))
      (is (= "bar" (get "/bar"))))
    (is (= 2 (count (server-requests))))
    (with-cassette :bar-bar
      (is (= "bar" (get "/bar")))
      (is (= "foo" (get "/foo"))))))

(def gzipp'd-response
  "A GZIP'd HTTP response representing []"
  {:status 200,
   :headers
   {"server" "",
    "content-encoding" "gzip",
    "content-type" "application/json; charset=UTF-8",
    "transfer-encoding" "chunked",
    "date" "Thu, 16 Aug 2012 01:11:12 GMT",
    "connection" "close"},
   :body #vcr-clj/bytes "H4sIAAAAAAAAAIuOBQApu0wNAgAAAA=="})

(deftest gzip-test
  (fs/mkdir "cassettes")
  (spit "cassettes/foob.clj" (pr-str {{:uri "/hoot"
                                       :server-name "localhost"
                                       :server-port 28366
                                       :query-string nil
                                       :request-method :get}
                                      [gzipp'd-response]}))
  (with-cassette :foob
    (is (= "[]" (get "/hoot")))))