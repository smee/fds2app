;; Parse RDS-PP identifiers
(ns fds2app.data.rds-pp
  (:require [fds2app.fds :as fds]
            [rds_clj.core :as rds-pp]))

(defrecord RDS-PP [id map]
  fds/Fds-Node
  (id [_] id)
  (type [_] :rds-pp-explanation)
  (properties [_] map)
  (relations [_] {})
  (relations [_ _] {}))

(defn explain-rds-keys 
  "If the type of a node is one of `:PV-Park`, `:PV-Anlage` or `:Komponentenbeschreibung` this function
tries to parse its id as a RDS-PP identifier."
  [node]
  (when (#{:PV-Park :PV-Anlage :Komponentenbeschreibung} (fds/type node))
    (let [id (fds/id node)
          explanation (rds-pp/rds-analyze2 id)] 
      (when (not-empty explanation) 
        {:explanation [(RDS-PP. (str "explanation-of-" id) explanation)]})))) 
