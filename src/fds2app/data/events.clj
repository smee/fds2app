(ns fds2app.data.events
  (:use [clojure.java.io :only (reader)]
        [clojure.data.csv :only (read-csv)])
  (:require [fds2app.fds :as fds]))

(defrecord Ereignis 
  #_"List of events, each one may have predecessor events as children"
  [id date description type origin park-id power-station-id component-id predecessors]
  fds/Fds-Node
  (children   [_] (map map->Ereignis predecessors))
  (properties [_] {:id id, :date date, :description description, :source origin, :references [park-id power-station-id component-id]})
  (type       [_] (keyword type))
  (id         [_] id))

(defrecord EreignisListe [events]
  fds/Fds-Node
  (children   [_] (map map->Ereignis events))
  (properties [_] {:description "wartungsrelevante Ereignisse"})
  (type       [_] :Ereignis-Liste)
  (id         [_] "dummy-event-id"))

(defn- successors-of [id rows]
  (lazy-seq 
    (when-let [next (first (filter #(= id (:predecessor-id %)) rows))]
      (cons next (successors-of (:id next) (remove #(= next %) rows))))))

(defn read-events 
  "Read events from csv, group common events in reverse order defined by id->predecessor id links."
  [file]
  (with-open [rdr (reader file)]
    (let [[header & rows] (doall (read-csv rdr :separator \;))
          rows (map (partial zipmap (map keyword header)) rows)
          singular (filter (comp empty? :predecessor-id) rows)]
      (reverse
        (sort-by #(get-in % [0 :id])
                 (for [{id :id :as m} singular]
                   (->> rows (successors-of id) (cons m) reverse)))))))


(comment
  (def csv (read-events "sample-data/events.csv"))
  (def x (->EreignisListe (map (fn [[f & r]] (assoc f :predecessors r)) csv)))
  (count (fds/tree-of x))
  (keys (first csv))
  (map #(get % "id") (first csv))
  )