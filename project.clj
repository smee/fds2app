(defproject fds2app "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.zip "0.1.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.clojars.smee/common "1.2.3"]
                 [com.cemerick/url "0.0.5"]
                 [clj-http "0.4.0"]
                 #_[net.cgrand/parsley "0.8.0"]
                 #_[org.clojure/core.match "0.2.0-alpha9"]
                 [noir "1.3.0-beta2"]]
  :main fds2app.server
  :ring {:handler fds2app.server/handler})