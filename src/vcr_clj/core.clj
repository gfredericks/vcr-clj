(ns vcr-clj.core
  (:require [fs.core :as fs]
            [clj-http.core]
            [vcr-clj.cassettes :refer [read-cassette
                                       write-cassette]]))

(defn ^:private var-name
  [var]
  (let [{:keys [ns name]} (meta var)]
    (str ns "/" name)))

;; TODO: add the ability to configure whether out-of-order
;; calls are allowed, or repeat calls, or such and such.
(defn record
  "Each spec is:
    {
     :var a var
     :arg-key-fn a function of the same arguments as the var that is
                 expected to return a value that can be stored and
                 compared for equality to the expected call. Defaults
                 to clojure.core/vector.
    }

   Redefs the vars to record the calls, and returns [val cassette]
   where val is the return value of func."
  [specs func]
  (let [calls (atom (zipmap (map (comp var-name :var) specs)
                            (repeat [])))

        redeffings
        (into {}
              (for [{:keys [var arg-key-fn]
                     :or {arg-key-fn vector}}
                    specs
                    :let [orig-fn (deref var)
                          the-var-name (var-name var)
                          wrapped (fn [& args]
                                    (let [res (apply orig-fn args)
                                          k (apply arg-key-fn args)
                                          call {:args k
                                                :return res}]
                                      (swap! calls update-in
                                             [the-var-name]
                                             conj call)
                                      res))]]
                [var wrapped]))
        func-return (with-redefs-fn redeffings func)
        cassette {:calls @calls}]
    [func-return cassette]))

;; Assuming that order is only preserved for calls to any var in
;; particular, not necessarily all the vars considered together.
(defn playback
  [specs cassette func]
  (let [redeffings
        (into {}
              (for [{:keys [var arg-key-fn]
                     :or {arg-key-fn vector}}
                    specs
                    :let [calls (atom (get-in cassette
                                              [:calls (var-name var)]))
                          next-call #(do (assert (seq @calls))
                                         (let [x (first @calls)]
                                           (swap! calls rest)
                                           x))
                          wrapped (fn [& args]
                                    (let [k (apply arg-key-fn args)
                                          {args' :args,
                                           x :return} (next-call)]
                                      (assert (= k args'))
                                      x))]]
                [var wrapped]))]
    (with-redefs-fn redeffings func)))

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
