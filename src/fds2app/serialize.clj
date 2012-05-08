;; Convert an instance of fds2app.Fds-Node to a serializable format.
;; TODO think of a serialization format independent way to enable pluggable
;; serialization formats.
(ns fds2app.serialize
  (:require [fds2app.fds :as f])
  (:use [org.clojars.smee.map :only (map-values)]))


(defn fds->map 
  "Convert an instance of fds2app.Fds-Node to a simple map."
  [fds-node]
  {:id (f/id fds-node)
   :type (f/type fds-node)
   :properties (f/properties fds-node)
   :relations (map-values (fn [nodes] (reduce #(update-in % [(f/type %2)] conj (f/id %2)) {} nodes)) (f/relations fds-node))})

