(defproject com.gfredericks/vcr-clj "0.3.3"
  :description "HTTP recording/playback for Clojure"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [fs "1.3.3"]]
  :profiles {:dev {:dependencies
                   [[bond "0.2.5"]
                    [clj-http "0.7.7"]
                    [ring/ring-jetty-adapter "1.1.2"]]}})
