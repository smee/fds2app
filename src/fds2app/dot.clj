(ns fds2app.dot
  (:require [fds2app.fds :as f])
  (:use [clojure.string :only (join)]))

(defn- id [node]
  (str \" (.replaceAll (f/id node) "\"" "\\\"") \"))

(defn create-dot [node]
  (let [edges (for [node (f/fds-seq node) 
                    :let [rel-ids (map id (f/relations node))]]
                (format "%s->{%s};" (id node) (join "," rel-ids)))]
    (str "digraph {" (apply str edges) "}")))

