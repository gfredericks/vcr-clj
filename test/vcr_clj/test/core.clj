(ns vcr-clj.test.core
  (:refer-clojure :exclude [get])
  (:require [clj-http.client :as client]
            [clojure.test :refer :all]
            [vcr-clj.core :refer [with-cassette]]
            [vcr-clj.test.helpers :as help]))

