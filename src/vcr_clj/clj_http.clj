(ns vcr-clj.clj-http
  "Helpers for using vcr-clj with clj-http."
  (:require [vcr-clj.core :as vcr]))

(def default-req-keys
  [:uri :server-name :server-port :query-string :request-method])

;; These vars are the primary method for customizing the behavior of
;; vcr-clj.

;; I think we stopped supporting this
(def ^:dynamic *record?*
  "Predicate which, given a ring request, determines if it should
  be recorded or passed through."
  (constantly true))

(def ^:dynamic *req-key*
  "Given a ring request, returns a key that it should be grouped
  under. Requests are allowed to come out of order as long as
  they are in-order with respect to other requests with the same
  key."
  #(select-keys % default-req-keys))

(defmacro with-cassette
  [name & body]
  `(vcr/with-cassette ~name
     [{:var        (var clj-http.core/request)
       :arg-key-fn *req-key*}]
     ~@body))
