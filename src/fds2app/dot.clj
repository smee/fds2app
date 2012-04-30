(ns fds2app.dot
  (:require [fds2app.fds :as f])
  (:use [clojure.string :only (join)]
        [org.clojars.smee.map :only (map-values)]))

(defn- fix-id [node]
  (str \" (.replaceAll (f/id node) "\"" "\\\"") \"))

(defn create-dot [node max-depth]
  (let [edges (for [node (f/fds-seq node max-depth) 
                    :let [rels (map-values (partial map fix-id) (f/relations node))]
                    :when (not (empty? rels))]
                (apply str (map (fn [[k v]] (apply str (map #(format "%s->%s[label=\"%s\"]" (fix-id node) % k) v))) rels)))]
    (str "digraph {" (apply str (distinct edges)) "}")))

