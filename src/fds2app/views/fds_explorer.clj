(ns fds2app.views.fds-explorer
  (:use 
    [noir 
     [core :only (defpage defpartial url-for)]
     [options :only (resolve-url)]
     [response :only (redirect json)]]
    [hiccup 
     [core :only (html)]
     [element :only (link-to javascript-tag)]
     [util :only (url to-str url-encode)]]
    [fds2app.dot :only (create-dot)])
  (:require [fds2app.fds :as f]
            [fds2app.data 
             [events :as ev]
             [stammbaum :as st]]))

(defn- component-finder [park]
  (fn [node] 
    (when-let [component (-> node f/properties :references :component-id (f/find-by-id park))] 
      (vector component))))

(def ^:private root 
  (let [event-list (ev/read-events "sample-data/events.csv")
        park (st/stammbaum-fds "sample-data/komponenten-sea1.xml")]
    (f/enhanced-tree event-list (component-finder park))))

(defn- serialize [fds-node]
  (if fds-node
    (json {:id (f/id fds-node)
           :type (f/type fds-node)
           :properties (f/properties fds-node)
           :relations (map (juxt f/type #(to-str (url "/fds" {:id (f/id %)}))) (f/relations fds-node))})
    {:status 400
     :body "Unknown id!"}))

(defpage fds-explore "/fds" {:keys [id]}
  (if id
    (serialize (f/find-by-id id root))
    (serialize root)))


(defpage "/fds/visualize" {:keys [id]}
  (let [root (if id (f/find-by-id id root) root)
        graph (create-dot root)]
    (html
      [:img {:src (str "https://chart.googleapis.com/chart?cht=gv&chl=" (url-encode graph))}])))
