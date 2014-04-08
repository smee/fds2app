(defproject fds2app "1.0.0-SNAPSHOT"
  :description "Federated data server (FDS) is a prototype for integrating heterogenous data sources."
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.zip "0.1.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.clojars.smee/common "1.2.5"]
                 [com.cemerick/url "0.0.5"]
                 [clj-http "0.4.0"]
                 [noir "1.3.0-beta10"]
                 ;[rds_clj "1.0.0-SNAPSHOT"]
				 ]
  :main fds2app.server
  :ring {:handler fds2app.server/handler})
