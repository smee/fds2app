(ns fds2app.fds
  (:refer-clojure :exclude [type]))

(defprotocol fds-node
  "This protocol knows how to handle external datasources, locates data within and links it to other datasources."
  (children [this] "Collection of fds-nodes.")
  (properties [this])
  (type [this])
  (id [this]))


(deftype SimpleNode [id type properties children]
  fds-node
  (children   [_] children)
  (type       [_] type)
  (properties [_] properties)
  (id         [_] id))



(defn tree-of [fds-node]
  (tree-seq (constantly true) children fds-node))

(defn find-by [pred fds-node]
  (->> fds-node tree-of (filter pred)))

(defn find-by-id [key fds-node]
  (first (find-by #(= key (id %)) fds-node)))



(comment 
  
  (def example 
    (SimpleNode. "a" "number" {:value 1}
             [(SimpleNode. "a1" "number" {:value 2} nil) 
              (SimpleNode. "a2" "number" {:value 3} 
                           [(SimpleNode. "a21" "number" {:foo :bar, :value 100} [])])]))
  
  (find-by-id "a21" example) ;; => #fds2app.fds.SimpleNode{:id "a21", :value 100, :type "number", :children []}
  (map id (find-by #(-> % properties :value even?) example));; => ("a1" "a21")
  (map id (find-by #(contains? (properties %) :foo) example)) ;; => ("a21")
  )