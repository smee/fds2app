(ns fds2app.test.core
  (:refer-clojure :exclude [type])
  (:require [fds2app.data.generated :as gen])
  (:use
    [clojure.test]
    fds2app.fds 
    [fds2app.data.stammbaum :only (stammbaum-fds component-finder)]
    [org.clojars.smee.seq :only (bf-tree-seq)]
    [fds2app.data.events :only (read-events ->EreignisListe)]))

(defrecord SimpleNode [id type properties rels]
    Fds-Node
    (relations  [_] {:foo rels})
    (relations  [this t] (select-keys (relations this) [t]))
    (type       [_] type)
    (properties [_] properties)
    (id         [_] id))
(def example (SimpleNode. "a" "number" {:value 1}
                             [(SimpleNode. "a1" "number" {:value 2} nil) 
                              (SimpleNode. "a2" "number" {:value 3} 
                                           [(SimpleNode. "a21" "number" {:foo :bar, :value 100} [])])]))
(deftest simple-fds-node
  (is (= "a21" (:id (find-by-id "a21" example))))
  (is (= ["a1" "a21"] (map id (find-by #(-> % properties :value even?) example))))
  (is (= ["a21"] (map id (find-by #(contains? (properties %) :foo) example)))))

;; inject nodes into example tree, also add to injected node dynamically
(deftest inject-nodes
  (let [enhanced (enhanced-tree example 
                                #(when (= "a" (id %)) 
                                   {:foo [(SimpleNode. "injected" "foobar" {:value 55, :misc "baz"} [])]})
                                #(when (= "foobar" (type %)) 
                                   {:foo [(SimpleNode. "injected-injected" "another type" {:some :properties} [])]}))]
    (is (= [2 0 1 0] (map (comp count nodes relations) (fds-seq example))))
    (is (= [3 0 1 1 0 0] (map (comp count nodes relations) (fds-seq enhanced))))))

(deftest stammbaum-test
  (let [park (stammbaum-fds "sample-data/komponenten-sea1.xml")]
    (is (= "Aldi" (-> (find-by-id "fan1" park) properties :Hersteller)))))
  
(deftest events-test
  (let [events-root (read-events "sample-data/events.csv")]
    (is (= 3 (count (nodes (relations events-root)))))
    (is (= 7 (count (fds-seq events-root))))))

(deftest combine-events-and-stammbaum
  (let [event-list (read-events "sample-data/events.csv")
        park (stammbaum-fds "sample-data/komponenten-sea1.xml")
        root-node (enhanced-tree event-list (component-finder park))]
    (def e root-node)
    (is (= 7 (-> event-list fds-seq count)))
    (is (= 8 (-> park fds-seq count)))
    (is (= 13 (-> root-node fds-seq count)))))

(deftest max-depth-traversal
  (let [natural-numbers (fds-seq (fds2app.data.generated.NaturalNumber. 0) 4)]
    (is (= 31 (count (take 100 natural-numbers))) "breadth first traversal of depth 4, 2 numbers as relations per node should result in 1+2^1+2^2+2^3+2^4=31 nodes all in all")))