(ns vcr-clj.core
  (:require [vcr-clj.cassettes :refer [cassette-exists?
                                       read-cassette
                                       write-cassette]]))

(defn ^:private var-name
  [var]
  (let [{:keys [ns name]} (meta var)]
    (str ns "/" name)))

(defn ^:private add-meta-from
  "Returns a version of x1 with its metadata set to the metadata
   of x2."
  [x1 x2]
  (with-meta x1 (meta x2)))

;; TODO: add the ability to configure whether out-of-order
;; calls are allowed, or repeat calls, or such and such.
(defn record
  "Redefs the vars to record the calls, and returns [val cassette]
   where val is the return value of func."
  [specs func]
  (let [calls (atom [])

        redeffings
        (into {}
              (for [{:keys [var arg-key-fn recordable? return-transformer]
                     :or {arg-key-fn vector
                          recordable? (constantly true)
                          return-transformer identity}}
                    specs
                    :let [orig-fn (deref var)
                          the-var-name (var-name var)
                          wrapped (fn [& args]
                                    (let [res (return-transformer (apply orig-fn args))
                                          k (apply arg-key-fn args)
                                          call {:var-name the-var-name
                                                :arg-key k
                                                :return res}]
                                      (when (apply recordable? args)
                                        (swap! calls conj call))
                                      res))]]
                [var (add-meta-from wrapped orig-fn)]))
        func-return (with-redefs-fn redeffings func)
        cassette {:calls @calls}]
    [func-return cassette]))

;; I guess currently we aren't recording actual arguments, just the arg-key.
;; Should that change?
(defn playbacker
  "Given a cassette, returns a stateful function which, when called with
   the var name and the arguments key, either throws an exception if the
   ordering has been violated or returns the return value for that call.

   order-scope can be:
     :global   all requests must come in the same order they were recorded in
     :var      all requests to the same function must come in the same order
     :key      requests can come in (more or less) any order, as ordering is
               only scoped to the arg key"
  [cassette order-scope]
  ;; don't support anything else yet
  (case order-scope
    :key (let [calls (atom (group-by (juxt :var-name :arg-key) (:calls cassette)))]
           (fn [var-name arg-key]
             (let [next-val (swap! calls
                                   (fn [x]
                                     (let [v (first (get x [var-name arg-key]))]
                                       (with-meta
                                         (update-in x [[var-name arg-key]] rest)
                                         {:v v}))))]
               (or (:v (meta next-val))
                   (throw (ex-info (format "No more recorded calls to %s!"
                                           var-name)
                                   {:function var-name
                                    :arg-key arg-key}))))))))

;; Assuming that order is only preserved for calls to any var in
;; particular, not necessarily all the vars considered together.
(defn playback
  [specs cassette func]
  (let [the-playbacker (playbacker cassette :key)
        redeffings
        (into {}
              (for [{:keys [var arg-key-fn recordable?]
                     :or {arg-key-fn vector
                          recordable? (constantly true)}}
                    specs
                    :let [orig (deref var)
                          the-var-name (var-name var)
                          wrapped (fn [& args]
                                    (let [k (apply arg-key-fn args)]
                                      (if (apply recordable? args)
                                        (:return (the-playbacker the-var-name k))
                                        (apply orig args))))]]
                [var (add-meta-from wrapped orig)]))]
    (with-redefs-fn redeffings func)))

(when-not (resolve 'clojure.core/update)
  (defn- update
    [m k f]
    (update-in m [k] f)))

;; * TODO
;; ** Handle streams

(def ^:dynamic *verbose?* false)
(defn println'
  [& args]
  (when *verbose?* (apply println args)))

(defn with-cassette-fn*
  [cassette-name specs func]
  (if (cassette-exists? cassette-name)
    (do
      (println' "Running test with existing" cassette-name "cassette...")
      (playback specs (read-cassette cassette-name) func))
    (do
      (println' "Recording new" cassette-name "cassette...")
      (let [[return cassette] (record specs func)]
        (println' "Serializing...")
        (write-cassette cassette-name cassette)
        return))))

(defmacro with-cassette
  "Each spec is:
    {
     :var a var
     :arg-key-fn  a function of the same arguments as the var that is
                  expected to return a value that can be stored and
                  compared for equality to the expected call. Defaults
                  to clojure.core/vector.
     :recordable? a predicate with the same arg signature as the var.
                  If the predicate returns false/nil on any call, the
                  call will be passed through transparently to the
                  original function without recording/playback.
     :return-transformer
                  a function that the return value will be passed through
                  while recording, which can be useful for doing things
                  like ensuring serializability.
    }"
  [cname specs & body]
  `(with-cassette-fn* ~cname ~specs (fn [] ~@body)))
