(ns fds2app.fds
  (:refer-clojure :exclude [type])
  (:use [org.clojars.smee
         [map :only (map-values)]
         [seq :only (bf-tree-seq df-tree-seq distinct-by)]]))

(defprotocol Fds-Node
  "This protocol knows how to handle external datasources, locates data within and links it to other datasources."
  (relations [this] [this type] "Collection of relations (map of relationship types to collection of nodes)")
  (properties [this] "Arbitrary map of properties. These are the contents of this information node")
  (type [this] "Type of this node.")
  (id [this] "Unique id of this node."))

(defrecord Enhanced-Node [node node-enhancers]
  Fds-Node
  (id [_] (id node))
  (properties [_] (properties node))
  (type [_] (type node))
  (relations [_]
             (->> node-enhancers
               (map #(% node))
               (cons (relations node))
               (apply merge-with concat) 
               (map-values (fn [nodes] (map #(->Enhanced-Node % node-enhancers) nodes)))))
  (relations [_ t]
             (->> node-enhancers
               (map #(% node t))               
               (cons (relations node t))
               (apply merge-with concat)
               (map-values (fn [nodes] (map #(->Enhanced-Node % node-enhancers) nodes))))))

(defn enhanced-tree 
  "Any function in node-enhancers may inspect an instance of Fds-Node and return references to child nodes
from any source."
  [fds-root-node & node-enhancers]
  (Enhanced-Node. fds-root-node node-enhancers))

;;;;;;;;;; Demo API ;;;;;;;;;;;;;;;;;;;;;;
(defn relationship-types 
  "Get all relationship types of all outgoing relationships of this node."
  [fds-node]
  (keys (relations fds-node)))

(defn nodes 
  "Get all related Fds-Nodes of relations (result of (relations fds-node))."
  [relations]
  (apply concat (vals relations)))

(defn fds-seq 
  "Breadth first sequence of a tree starting at the root node given. 
Takes one optional parameter:
- max-depth ... maximum traversal depth"
  ([fds-node] (fds-seq fds-node 10))
  ([fds-node max-depth]
  (bf-tree-seq (constantly true) #(nodes (relations %)) fds-node max-depth)))

(defn distinct-fds-seq
  "Traverses the graph starting at fds-node in a breadth first fashion. Visits every node only once (skips duplicates, i.e. nodes with the same id).
Takes one optional parameter:
- max-depth ... maximum traversal depth"
  ([fds-node] (distinct-fds-seq fds-node 10))
  ([fds-node max-depth] (distinct-by id (fds-seq fds-node max-depth)))) 

(defn find-by 
  "Find any node within the tree sequence spanned starting at the given node that returns true for the given predicate."
  [pred fds-node]
  (->> fds-node distinct-fds-seq (filter pred)))

(defn find-by-id 
  "Find any node within the tree seq. spanned starting at the given node with id=key."
  [key fds-node]
  (first (find-by #(= key (id %)) fds-node)))

