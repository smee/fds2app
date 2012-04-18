(ns fds2app.data.events
  (:use [clojure.java.io :only (reader)]
        [clojure.data.csv :only (read-csv)])
  (:require [fds2app.fds :as fds]))


(deftype Ereignis [id predecessor-id park-id power-station-id component-id date description type origin]
  fds/fds-node
  (children [_] [])
  (properties [_] {})
  (type [_] (keyword type))
  (id [_] (str "event." id)))

(defn read-events [file]
  (with-open [rdr (reader file)]
    (doall (read-csv rdr :separator \;))))




(comment
  (def csv (read-events "sample-data/events.csv"))
  
  )