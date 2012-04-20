(ns fds2app.fds
  (:refer-clojure :exclude [type]))

(defprotocol Fds-Node
  "This protocol knows how to handle external datasources, locates data within and links it to other datasources."
  (children [this] "Collection of fds-nodes.")
  (properties [this])
  (type [this])
  (id [this]))

(defn tree-of [fds-node]
  (tree-seq (constantly true) children fds-node))

(defn find-by [pred fds-node]
  (->> fds-node tree-of (filter pred)))

(defn find-by-id [key fds-node]
  (first (find-by #(= key (id %)) fds-node)))

;(defprotocol Node-Enhancer
  ;"Query for additional informations about a fds-node"
  ;(related [this ^fds-node node]))
(defrecord Enhanced-Node [node node-enhancers]
  Fds-Node
  (id [_] (id node))
  (properties [_] (properties node))
  (type [_] (type node))
  (children [_]
            (->> node-enhancers
              (map #(% node))
              (apply concat (children node))
              (map #(->Enhanced-Node % node-enhancers)))))

(defn enhanced-tree 
  "Any function in node-enhancers may inspect an instance of Fds-Node and return child nodes
from any source."
  [fds-node & node-enhancers]
  (Enhanced-Node. fds-node node-enhancers))



