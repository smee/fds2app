(ns fds2app.fds
  (:refer-clojure :exclude [type]))

(defprotocol Fds-Node
  "This protocol knows how to handle external datasources, locates data within and links it to other datasources."
  (relations [this] "Collection of related fds-nodes.")
  (properties [this])
  (type [this])
  (id [this]))

(defrecord Enhanced-Node [node node-enhancers]
  Fds-Node
  (id [_] (id node))
  (properties [_] (properties node))
  (type [_] (type node))
  (relations [_]
            (->> node-enhancers
              (map #(% node))
              (apply concat (relations node))
              (map #(->Enhanced-Node % node-enhancers)))))

(defn enhanced-tree 
  "Any function in node-enhancers may inspect an instance of Fds-Node and return child nodes
from any source."
  [fds-root-node & node-enhancers]
  (Enhanced-Node. fds-root-node node-enhancers))


;;;;;;;;;; Demo API ;;;;;;;;;;;;;;;;;;;;;;
(defn fds-seq 
  "Depth first sequence of a tree starting at the root node given."
  [fds-node]
  (tree-seq (constantly true) relations fds-node))

(defn find-by [pred fds-node]
  (->> fds-node fds-seq (filter pred)))

(defn find-by-id [key fds-node]
  (first (find-by #(= key (id %)) fds-node)))

(defn get-by-ids [node ids]
  )