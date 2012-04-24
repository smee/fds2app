(ns mbs-se-pv.server
  (:require 
    [noir.server :as server]
    ;; view namespaces need to be required explicitely for tomcat
    [fds2app.views web fds-explorer])
  (:gen-class))


(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'fds2app
                        })))


;;;;;;;;;;;;;;; production settings ;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def handler (server/gen-handler {:mode :prod
                  :ns 'fds2app
                  :base-url "/fds2app"}))

(comment
  (-main)
)