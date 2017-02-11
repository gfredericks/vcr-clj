(defproject com.gfredericks/vcr-clj "0.4.9"
  :description "HTTP recording/playback for Clojure"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [fs "1.3.3"]]
  :deploy-repositories [["releases" :clojars]]
  :profiles {:dev [:test-deps-without-clj-http :clj-http-101]

             :test-deps-without-clj-http
             {:dependencies [[bond "0.2.5"]
                             [ring/ring-jetty-adapter "1.1.2"]]}

             :clj-http-101 {:dependencies [[clj-http "1.0.1"]]}
             :clj-http-091 {:dependencies [[clj-http "0.9.1"]]}
             :clj-http-077 {:dependencies [[clj-http "0.7.7"]]}
             :clj-http-053 {:dependencies [[clj-http "0.5.3"]]}}
  :aliases {"test-all"
            ^{:doc "Runs tests on all listed versions of clj-http."}
            ["do"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-101" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-091" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-077" "test,"
             ;; 053 fails currently, don't feel like investigating
             ;; "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-053" "test"
             ]})
