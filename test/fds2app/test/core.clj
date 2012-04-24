(ns fds2app.test.core
  (:refer-clojure :exclude [type])
  (:use
    [clojure.test]
    fds2app.fds 
    [fds2app.data.stammbaum :only (stammbaum-fds)]
    [fds2app.data.events :only (read-events ->EreignisListe)]))

(defrecord SimpleNode [id type properties relations]
    Fds-Node
    (relations   [_] relations)
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
  (is (= ["a21"] (map id (find-by #(contains? (properties %) :foo) example))))
  
  
  ;; inject nodes into example tree, also add to injected node dynamically
  (deftest inject-nodes
    (let [enhanced (enhanced-tree example 
                                  #(when (= "a" (id %)) 
                                     [(SimpleNode. "injected" "foobar" {:value 55, :misc "baz"} [])])
                                  #(when (= "foobar" (type %)) 
                                     [(SimpleNode. "injected-injected" "another type" {:some :properties} [])]))]
      (is (= [2 0 1 0] (map (comp count relations) (fds-seq example))))
      (is (= [3 0 1 0 1 0] (map (comp count relations) (fds-seq enhanced)))))))

(deftest stammbaum-test
  (let [park (stammbaum-fds "sample-data/komponenten-sea1.xml")]
    (is (= "Aldi" (-> (find-by-id "fan1" park) properties :Hersteller)))))
  
(deftest events-test
  (let [events-root (read-events "sample-data/events.csv")]
    (is (= 2 (count (relations events-root))))
    (is (= 5 (count (fds-seq events-root))))))

(defn- component-finder [park]
  (fn [node] 
    (when-let [component (-> node properties :references :component-id (find-by-id park))] 
      (vector component))))

(deftest combine-events-and-stammbaum
  (let [event-list (read-events "sample-data/events.csv")
        park (stammbaum-fds "sample-data/komponenten-sea1.xml")
        root-node (enhanced-tree event-list (component-finder park))]
    (is (= 5 (-> event-list fds-seq count)))
    (is (= 8 (-> park fds-seq count)))
    (is (= 9 (-> root-node fds-seq count)))))