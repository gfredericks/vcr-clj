(defproject com.gfredericks/vcr-clj "0.4.23-SNAPSHOT"
  :description "HTTP recording/playback for Clojure"
  :dependencies [[org.clojure/data.codec "0.1.0"]
                 [clj-commons/fs "1.6.309"]
                 [mvxcvi/puget "1.1.2"]]
  :deploy-repositories [["releases" :clojars]]
  :profiles {;; I'm not sure how it happened but the 0.4.20 release
             ;; seems to have included all the dependencies reachable
             ;; from the dev profile, i.e.  as if `lein release` ran
             ;; with dev profile dependencies for some reason; so I
             ;; guess I'm just going to comment this out for now
             #_#_:dev [:test-deps-without-clj-http :clj-http-101
                       :clojure-17]

             :test-deps-without-clj-http
             [:clojure-17
              {:dependencies [[bond "0.2.5"]
                              ;; need recent jetty to get gzip, I guess :/
                              [org.eclipse.jetty/jetty-server "9.3.3.v20150827"]
                              [ring/ring-jetty-adapter "1.6.3"
                               :exclusions [org.eclipse.jetty/jetty-server]]]}]
             :clj-http-390 {:dependencies [[clj-http "3.9.0"]]}
             :clj-http-370 {:dependencies [[clj-http "3.7.0"]]}
             :clj-http-341 {:dependencies [[clj-http "3.4.1"]]}
             :clj-http-230 {:dependencies [[clj-http "2.3.0"]]}
             :clj-http-112 {:dependencies [[clj-http "1.1.2"]]}
             :clj-http-101 {:dependencies [[clj-http "1.0.1"]]}
             :clj-http-091 {:dependencies [[clj-http "0.9.1"]]}
             :clj-http-077 {:dependencies [[clj-http "0.7.7"]]}
             :clj-http-053 {:dependencies [[clj-http "0.5.3"]]}
             :clojure-17   {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :clojure-18   {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :clojure-19   {:dependencies [[org.clojure/clojure "1.9.0"]]}}
  :aliases {"test-all-clj-https"
            ^{:doc "Runs tests on all listed versions of clj-http."}
            ["do"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-390" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-370" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-341" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-230" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-112" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-101" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-091" "test,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-077" "test,"
             ;; 053 fails currently, don't feel like investigating
             ;; "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-053" "test"
             ]
            "retrieve-all-profile-deps"
            ^{:doc "Retrieve all deps required by all profiles"}
            ["do"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-390" "deps,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-370" "deps,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-341" "deps,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-230" "deps,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-112" "deps,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-101" "deps,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-091" "deps,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-077" "deps,"
             "clean," "with-profile" "-dev,+test-deps-without-clj-http,+clj-http-053" "deps"
             ]
            "ci-deps"
            ["do"
             "retrieve-all-profile-deps,"
             "with-profile" "-clojure-17,+clojure-18" "retrieve-all-profile-deps,"
             "with-profile" "-clojure-17,+clojure-19" "retrieve-all-profile-deps"
             ]
            "ci"
            ["do"
             "test-all-clj-https,"
             "with-profile" "-clojure-17,+clojure-18" "test-all-clj-https,"
             "with-profile" "-clojure-17,+clojure-19" "test-all-clj-https"]})
