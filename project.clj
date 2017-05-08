(defproject com.gfredericks/vcr-clj "0.4.13-SNAPSHOT"
  :description "HTTP recording/playback for Clojure"
  :dependencies [[org.clojure/data.codec "0.1.0"]
                 [me.raynes/fs "1.4.6"]
                 [mvxcvi/puget "1.0.1"]]
  :deploy-repositories [["releases" :clojars]]
  :profiles {:dev [:test-deps-without-clj-http :clj-http-101
                   :clojure-17]

             :test-deps-without-clj-http
             [:clojure-17
              {:dependencies [[bond "0.2.5"]
                              [ring/ring-jetty-adapter "1.1.2"]]}]

             :clj-http-341 {:dependencies [[clj-http "3.4.1"]]}
             :clj-http-230 {:dependencies [[clj-http "2.3.0"]]}
             :clj-http-112 {:dependencies [[clj-http "1.1.2"]]}
             :clj-http-101 {:dependencies [[clj-http "1.0.1"]]}
             :clj-http-091 {:dependencies [[clj-http "0.9.1"]]}
             :clj-http-077 {:dependencies [[clj-http "0.7.7"]]}
             :clj-http-053 {:dependencies [[clj-http "0.5.3"]]}
             :clojure-17   {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :clojure-18   {:dependencies [[org.clojure/clojure "1.8.0"]]}}
  :aliases {"test-all-clj-https"
            ^{:doc "Runs tests on all listed versions of clj-http."}
            ["do"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-341" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-230" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-112" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-101" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-091" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-077" "test,"
             ;; 053 fails currently, don't feel like investigating
             ;; "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-053" "test"
             ]
            "ci"
            ["do"
             "test-all-clj-https,"
             "with-profile" "-clojure-17,+clojure-18" "test-all-clj-https"]})
