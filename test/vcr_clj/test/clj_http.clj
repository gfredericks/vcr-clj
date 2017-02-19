(ns vcr-clj.test.clj-http
  (:refer-clojure :exclude [get])
  (:require [clj-http.client :as client]
            [clojure.test :refer :all]
            [fs.core :as fs]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [vcr-clj.cassettes.serialization :refer [str->bytes]]
            [vcr-clj.clj-http :refer [with-cassette]]
            [vcr-clj.test.helpers :as help]))

(use-fixtures :each help/delete-cassettes-after-test)

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
   :body (str->bytes "H4sIAAAAAAAAAIuOBQApu0wNAgAAAA==")})

(deftest gzip-test
  (fs/mkdir "cassettes")
  ;; this won't work if we change the cassette format; might be able
  ;; to do a regular ring server??
  (spit "cassettes/foob.clj" (pr-str {:calls
                                      [{:var-name "clj-http.core/request"
                                        :arg-key {:uri "/hoot"
                                                  :server-name "localhost"
                                                  :server-port 28366
                                                  :query-string nil
                                                  :request-method :get}
                                        :return gzipp'd-response}]}))
  (with-cassette :foob
    (is (= "[]" (get "/hoot")))))

(deftest gzip-recording-test
  (with-redefs [clj-http.core/request
                (constantly
                 (update-in gzipp'd-response [:body]
                            #(java.io.ByteArrayInputStream. %)))]
    (with-cassette :tamborines
      (is (= "[]" (get "/not/a/meaningful/path"))))))


;;
;; https://github.com/fredericksgary/vcr-clj/issues/2
;;

(defn redirecting-server
  [{:keys [uri]}]
  (case uri
    "/foo" (assoc (resp/redirect "/bar")
             :body "not bar")
    "/bar" {:status 200
            :body "bar"
            :headers {}}))

(deftest redirect-test
  (with-jetty-server redirecting-server
    (with-cassette :whale
      (is (= "bar" (get "/foo")))))
  (with-cassette :whale
    (is (= "bar" (get "/foo")))))

(defn time-server [&args]
  {:status 200
   :body (str (System/currentTimeMillis))
   :headers {}})


(deftest recording-new-http-episodes
  (with-jetty-server time-server
    (with-local-vars [result-with-a nil
                      result-with-b nil]
      (with-cassette :recording-new-episodes
        (var-set result-with-a (get "/a")))
      (with-cassette :recording-new-episodes {:record-new-episodes? true}
        (var-set result-with-b (get "/b")))
      (with-cassette :recording-new-episodes
        (is (= (get "/a") @result-with-a))
        (is (= (get "/b") @result-with-b))))))
