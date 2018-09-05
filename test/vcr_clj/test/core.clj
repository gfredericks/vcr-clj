(ns vcr-clj.test.core
  (:refer-clojure :exclude [get])
  (:require [bond.james :refer [calls with-spy]]
            [clj-http.client :as client]
            [clojure.test :refer :all]
            [puget.printer :as printer]
            [vcr-clj.cassettes.serialization :as serialization]
            [vcr-clj.cassettes :as cassettes]
            [vcr-clj.core :refer [with-cassette]]
            [vcr-clj.test.helpers :as help]))

(use-fixtures :each help/delete-cassettes-after-test)

;; some test fns
(defn plus [a b] (+ a b))
(defn increment [x] (inc x))
(defn put-foo [m v] (assoc m :foo v))
(defn java-obj [val] (proxy [java.io.InputStream] []
                       (read ([] val))))

(deftest basic-test
  (with-spy [plus]
    (is (empty? (calls plus)))
    (with-cassette :bang [{:var #'plus}]
      (is (= 5 (plus 2 3)))
      (is (= 1 (count (calls plus)))))
    ;; Check that it replays correctly without calling the original
    (with-cassette :bang [{:var #'plus}]
      (is (= 5 (plus 2 3)))
      (is (= 1 (count (calls plus)))))
    ;; Check that different calls throw an exception
    (with-cassette :bang [{:var #'plus}]
      (is (thrown? clojure.lang.ExceptionInfo
            (plus 3 4))))))

(deftest two-vars-test
  (with-spy [plus increment]
    (is (empty? (calls plus)))
    (is (empty? (calls increment)))

    (with-cassette :baz [{:var #'plus} {:var #'increment}]
      (is (= 42 (increment 41)))
      (is (= 79 (plus 42 37)))
      (is (= 42 (increment 41))))

    (is (= 2 (count (calls increment))))
    (is (= 1 (count (calls plus))))

    (with-cassette :baz [{:var #'plus} {:var #'increment}]
      (is (= 42 (increment 41)))
      (is (= 79 (plus 42 37)))
      (is (= 42 (increment 41)))
      (is (= 2 (count (calls increment))))
      (is (= 1 (count (calls plus)))))

    (is (= 2 (count (calls increment))))
    (is (= 1 (count (calls plus))))))

(deftest recordable?-test
  (with-spy [plus]
    (with-cassette :breezy [{:var #'plus
                             :recordable? (fn [a _] (even? a))}]
      (is (= 7 (plus 3 4)))
      (is (= 9 (plus 4 5))))

    (is (= 2 (count (calls plus))))

    (with-cassette :breezy [{:var #'plus
                             :recordable? (fn [a _] (even? a))}]
      (is (= 7 (plus 3 4)))
      (is (= 9 (plus 4 5))))

    ;; One of the two calls went through
    (is (= 3 (count (calls plus))))))

(deftest short-circuit-when-recordable?-predicate-returns-falsey
  (with-spy [increment]
    (with-cassette :skip-calls [{:var #'increment
                                 :recordable? (constantly nil)
                                 :arg-key-fn (fn [& _] (is false))
                                 :return-transformer (fn [& _] (is false))}]
      (is (= 42 (increment 41))))
    (is (= 1 (count (calls increment))))
    (is (empty? (:calls (cassettes/read-cassette :skip-calls))))))

(deftest arg-key-fn-test
  (with-cassette :blammo [{:var #'increment
                           :arg-key-fn #(mod % 2)}]
    (is (= 42 (increment 41))))

  (with-cassette :blammo [{:var #'increment
                           :arg-key-fn #(mod % 2)}]
    (is (= 42 (increment 29)))))

(deftest arg-transformer-test
  (with-spy [put-foo]
    (with-cassette :shebang
      [{:var #'put-foo
        :arg-transformer (fn [m v]
                           (is (= [{} 42] [m v]))
                           [(vary-meta m assoc :arg-transformer true) v])
        :recordable?  (fn [m v]
                        (is (= [{} 42] [m v]))
                        (is (:arg-transformer (meta m)))
                        true)
        :arg-key-fn (fn [m v]
                      (is (= [{} 42] [m v]))
                      (is (:arg-transformer (meta m)))
                      [m v])}]
      (is (= {:foo 42} (put-foo {} 42)))
      (is (= 1 (count (calls put-foo))))
      (is (= [{} 42]
             (:args (first (calls put-foo)))))
      (is (-> (calls put-foo) first :args first meta :arg-transformer)))))

(defn test-print-handlers
  [cls]
  (if (isa? cls java.io.InputStream)
    (printer/tagged-handler
      'vcr/inputstream
      (fn [stream]
        (.read stream)))

    (serialization/default-print-handlers cls)))

(deftest cassette-data-test
  (with-spy [java-obj]
    (with-cassette
      {:name :boom
       :serialization {:print-handlers test-print-handlers}}
      [{:var #'java-obj}]
      (is (instance? java.io.InputStream (java-obj 3)))
      (is (= 1 (count (calls java-obj)))))))

(defn self-caller
  "Multi-arity function that calls itself when called with one argument"
  ([x]
   (self-caller x 1))
  ([x n]
   (+ x n)))

(deftest do-not-record-self-calls-test
  (with-spy [self-caller]
    (with-cassette :self-caller [{:var #'self-caller}]
      (is (= 42 (self-caller 41))))
    (is (= [{:args [41 1] :return 42}
            {:args [41] :return 42}]
           ;; with-spy records calls in the order of their returns
           (calls self-caller))
        "the original function calls itself")
    (is (= 1 (count (:calls (cassettes/read-cassette :self-caller))))
        "only the outermost call is recorded"))
  (with-spy [self-caller]
    (with-cassette :self-caller [{:var #'self-caller}]
      (is (= 42 (self-caller 41))))
    (is (empty? (calls self-caller))
        "the recorded call does not result in any self-calls")))

;; https://github.com/gfredericks/vcr-clj/issues/16

(deftest recording-on-other-threads
  (with-spy [increment]
    (with-cassette :multithreaded-plussing [{:var #'increment}]
      (is (= 42 (increment 41)))
      (is (= 1 (count (calls increment))))
      (doto (Thread. #(increment 99))
        (.start)
        (.join))
      (is (->> (calls increment)
               (map :args)
               (= [[41] [99]]))))
    (with-cassette :multithreaded-plussing [{:var #'increment}]
      (is (= 42 (increment 41)))
      (is (= 2 (count (calls increment))))
      (let [p (promise)]
        (doto (Thread. #(deliver p (increment 99)))
          (.start)
          (.join))
        (is (= 2 (count (calls increment))))
        (is (= 100 @p))))))
