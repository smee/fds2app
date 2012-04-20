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



(comment 
  (defrecord SimpleNode [id type properties children]
    Fds-Node
    (children   [_] children)
    (type       [_] type)
    (properties [_] properties)
    (id         [_] id))
  
  (def example 
    (SimpleNode. "a" "number" {:value 1}
             [(SimpleNode. "a1" "number" {:value 2} nil) 
              (SimpleNode. "a2" "number" {:value 3} 
                           [(SimpleNode. "a21" "number" {:foo :bar, :value 100} [])])]))
  
  (find-by-id "a21" example) ;; => #fds2app.fds.SimpleNode{:id "a21", :value 100, :type "number", :children []}
  (map id (find-by #(-> % properties :value even?) example));; => ("a1" "a21")
  (map id (find-by #(contains? (properties %) :foo) example)) ;; => ("a21")
  ;; inject nodes into example tree, also add to injected node dynamically
  (def foo (enhanced-tree example 
                          #(when (= "a" (id %)) 
                             [(SimpleNode. "injected" "foobar" {:value 55, :misc "baz"} [])])
                          #(when (= "foobar" (type %)) 
                             [(SimpleNode. "injected-injected" "another type" {:some :properties} [])])))
  (map (comp count children) (tree-of foo))
  )

