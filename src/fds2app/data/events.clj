;; Poor man's event/ticket system
;; This is an example of a data source that contains informations about events.
;; See _sample-data/events.csv_ for sample events.
(ns fds2app.data.events
  (:use [clojure.java.io :only (reader)]
        [clojure.data.csv :only (read-csv)])
  (:require [fds2app.fds :as fds]))

;;  List of events, each one may have predecessor events as relations
(defrecord Ereignis 
  [id date description type origin park-id power-station-id component-id predecessors]
  fds/Fds-Node
  (relations   [_] {:pred (map map->Ereignis predecessors)})
  (relations  [this t] (select-keys (fds/relations this) [t]))
  (properties [this] {:id id, 
                      :date date, 
                      :description description, 
                      :source origin, 
                      :references {:park-id park-id, :power-station-id power-station-id, :component-id component-id}
                      :depth (count (fds/relations this))})
  (type       [_] (str "Ereignis " type))
  (id         [_] id))

;; This is a list of events that implements the `Fds-Node` protocol.
(defrecord EreignisListe [events]
  fds/Fds-Node
  (relations   [_] {:event (map map->Ereignis events)})
  (relations  [this t] (select-keys (fds/relations this) [t]))
  (properties [this] {:description "wartungsrelevante Ereignisse", 
                      :events (count (fds/relations this))})
  (type       [_] :Ereignis-Liste)
  (id         [_] "dummy-event-id"))

;;;;;;;;;;;; ## IO
(defn- successors-of 
  "Reconstruct all descendant rows of one id."
  [id rows]
  (lazy-seq 
    (when-let [next (first (filter #(= id (:predecessor-id %)) rows))]
      (cons next (successors-of (:id next) (remove #(= next %) rows))))))

(defn read-events 
  "Read events from a csv file, group common events defined by id->predecessor id links, sort in reverse date order."
  [file]
  (with-open [rdr (reader file)]
    (let [[header & rows] (doall (read-csv rdr :separator \;))
          rows (map (partial zipmap (map keyword header)) rows)
          singular (filter (comp empty? :predecessor-id) rows)]
      (->> (for [{id :id :as m} singular]
             (->> rows (successors-of id) (cons m) reverse))
        (sort-by #(get-in % [0 :id])) 
        reverse 
        (map (fn [[f & r]] (assoc f :predecessors r)))
        ->EreignisListe))))

