(ns fds2app.views.fds-explorer
  (:use 
    [noir 
     [core :only (defpage defpartial url-for)]
     [options :only (resolve-url)]
     [response :only (redirect json)]
     [session :only (flash-get flash-put!)]]
    [hiccup 
     [core :only (html)]
     [element :only (link-to javascript-tag)]
     [util :only (url to-str url-encode)]]
    [fds2app.dot :only (create-dot)]
    [org.clojars.smee
     [map :only (map-values)]
     [util :only (s2i)]])
  (:require [fds2app.fds :as f]
            [fds2app.data 
             [events :as ev]
             [stammbaum :as st]
             [documents :as d]
             generated]
            [fds2app.views 
             [common :refer (layout-with-links layout)]
             [rest :refer (node2json)]]))

(defn- component-finder 
  "Connect data about components with events."
  [park]
  (fn [node] 
    (when-let [component (-> node f/properties :references :component-id (f/find-by-id park))] 
      {:component [component]})))

(def ^:private root 
  (let [event-list (ev/read-events "sample-data/events.csv")
        park (st/stammbaum-fds "sample-data/komponenten-sea1.xml")]
    (f/enhanced-tree event-list (component-finder park) d/join-documents)))

;;;;;;;;;;;;;;;;;;;;;; REST ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defpage "/fds.json" {:keys [id]}
  (if id
    (node2json (f/find-by-id id root))
    (node2json root)))


;;;;;;;;;;;;;;;;;;;; html page ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- create-dot-chart-url [node max-depth & [width height]]
  (let [graph (create-dot node max-depth)
        link (str "https://chart.googleapis.com/chart?cht=gv&chl=" (url-encode graph))]
    (if (and width height)
      (str link "&chs=" width "x" height)
      link)))


(defpartial map->table [m]
  [:table.table.table-striped.table-condensed
   [:tr [:th "Schlüssel"] [:th "Wert"]] 
   (for [[k v] m]
     [:tr [:td k] [:td v]])])

(defpartial seqs->table [headers seqs]
  [:table.table.table-striped.table-condensed
   [:tr (for [h headers] [:th h])] 
   (for [vs seqs]
     [:tr (for [v vs] 
            [:td v])])])

(defn- breadcrumb-links 
  "Add link to current page to session, create vector of links to the last 8 visited pages."
  [crnt-node]
  (let [crumbs (or (flash-get :breadcrumbs) [])
        next-link (link-to (str "/fds.html?id=" (f/id crnt-node)) (f/type crnt-node))
        next-crumbs (->> next-link (conj crumbs) (partition-by identity) (map first) (take-last 8) vec)]
    (println next-crumbs)
    (flash-put! :breadcrumbs next-crumbs)
    next-crumbs))

(defpage "/fds.html" {:keys [id]}
  (let[node (if (not-empty id) (f/find-by-id id root) root)
       links (for [[k vs] (f/relations node), v vs]
               [k (f/type v) (link-to (str "/fds.html?id=" (f/id v)) "Link")])]
    (layout-with-links
      ;; top links
      [0 [:a {:href "#"} "Home"] [:a {:href "#contact"} "Kontakt"]]
      ;; bread crumb trail
      (breadcrumb-links node)
      ;; left side bar
      [:div.span2 
       [:h4 "Federated data system"]
       "Anzeige virtuell integrierter Daten bezüglich einer Energieerzeugungsanlage"
       [:img {:src (create-dot-chart-url root 10 200 300)}]]
      ;; main contents
      [:div.span10
       [:h3 "Inhalt"]
       (map->table (map-values str (f/properties node)))
       [:h4 "Weiterführende Informationen"]
       (seqs->table ["Referenzart" "Knotenart" "Link"] links)
       [:h4 "Visualisierung"]
       [:img {:src (create-dot-chart-url node 1)}]])))
