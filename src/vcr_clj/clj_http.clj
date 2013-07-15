(ns vcr-clj.clj-http
  "Helpers for using vcr-clj with clj-http."
  (:require [vcr-clj.core :as vcr]))

(def default-req-keys
  [:uri :server-name :server-port :query-string :request-method])

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
            (update-in [:arg-key-fn] #(or % (fn [req#] (select-keys req# default-req-keys)))))]
       ~@body)))
