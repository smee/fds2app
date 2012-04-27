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
    [fds2app.dot :only (create-dot)]
    [org.clojars.smee.util :only (s2i)])
  (:require [fds2app.fds :as f]
            [fds2app.data 
             [events :as ev]
             [stammbaum :as st]
             [documents :as d]
             generated]
            [fds2app.views.common :refer (layout-with-links)]
            ))

(defn- component-finder [park]
  (fn [node] 
    (when-let [component (-> node f/properties :references :component-id (f/find-by-id park))] 
      (vector component))))

(def ^:private root 
  (let [event-list (ev/read-events "sample-data/events.csv")
        park (st/stammbaum-fds "sample-data/komponenten-sea1.xml")]
    (f/enhanced-tree event-list (component-finder park) d/join-documents)))

(defn- serialize [fds-node]
  (if fds-node
    (json {:id (f/id fds-node)
           :type (f/type fds-node)
           :properties (f/properties fds-node)
           :relations (map (juxt f/type #(to-str (url "/fds" {:id (f/id %)}))) (f/relations fds-node))})
    {:status 400
     :body "Unknown id!"}))

(defpage fds-explore "/fds.json" {:keys [id]}
  (if id
    (serialize (f/find-by-id id root))
    (serialize root)))


(defpage "/fds/visualize" {:keys [id max-depth]}
  (let [max-depth (s2i max-depth 10)
        root (if id (f/find-by-id id root) root)
        ;root (fds2app.data.generated.NaturalNumber. 0)
        graph (create-dot root max-depth)]
    (html
      [:img {:src (str "https://chart.googleapis.com/chart?cht=gv&chl=" (url-encode graph))}])))


(defpartial map->table [m]
  [:table.table.table-striped.table-condensed {:width "60%"}
   [:tr [:th "Schlüssel"] [:th "Wert"]] 
   (for [[k v] m]
     [:tr [:td k] [:td v]])])

(defpage "/fds.html" {:keys [id]}
  (let[node (if id (f/find-by-id id root) root)
       relations (f/relations node)
       links (map #(link-to (to-str (url "/fds.html" {:id (f/id %)})) "Link") relations)]
    (layout-with-links
      [0 [:a {:href "#"} "Home"] [:a {:href "#contact"} "Kontakt"]]
      [:div.span2 "Federated data system"]
      [:div.span10
       [:h4 "Inhalt"]
       (map->table (f/properties node))
       [:h4 "Weiterführende Informationen"]
       (map->table (zipmap (map f/type relations) links))])))
