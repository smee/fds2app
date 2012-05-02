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
            [fds2app.views.common :refer (layout-with-links layout)]
            )
  (:import [java.security NoSuchAlgorithmException MessageDigest]
           java.math.BigInteger))

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

(def ^:private data-sources (ref {}))

(defn- serialize 
  "Render Fds-Node as JSON. The relations get serialized as a nested map of relation types to maps of node types to lists of node ids."
  [fds-node]
  (if fds-node
    (json {:id (f/id fds-node)
           :type (f/type fds-node)
           :properties (f/properties fds-node)
           :relations (map-values (fn [nodes] (reduce #(update-in % [(f/type %2)] conj (f/id %2)) {} nodes)) (f/relations fds-node))
           
           })
    {:status 400
     :body "Unknown id!"}))

(defpage fds-explore "/fds.json" {:keys [id]}
  (if id
    (serialize (f/find-by-id id root))
    (serialize root)))

(defn- md5
  "Compute the hex MD5 sum of a byte array."
  [#^bytes b]
  (let [alg (doto (MessageDigest/getInstance "MD5")
              (.reset)
              (.update b))]
    (try
      (.toString (new BigInteger 1 (.digest alg)) 16)
      (catch NoSuchAlgorithmException e
        (throw (new RuntimeException e))))))

(defpage [:get "/fds/data-sources"] []
  (json @data-sources))

(defpage [:delete "/fds/data-sources/:id"] {:keys [id]}
  (dosync (alter data-sources dissoc id))
  {:status 200})

(defpage [:get "/fds/data-sources/:id"] {:keys [id]}
  (@data-sources id))


(defpage [:post "/fds/data-sources"] {:keys [callback-url]}
  (if (not-empty callback-url) 
    (let [id (md5 (.getBytes callback-url))]
      (dosync (alter data-sources assoc id callback-url))
      {:status 200
       :body (str "/fds/data-sources/" id)})
    {:status 400
     :body "missing parameter 'callback-url'"}))

(defpage "/fds" []
  (let [docs [["/fds/data-sources" "GET" "-" "JSON mit IDs und URLs aller bekannten Datenquellen"]
              ["/fds/data-sources" "POST" "callback-url" "Füge eine neue Datenquelle hinzu. Liefert die URL fuer die neue Datenquelle."]
              ["/fds/data-sources/ID" "GET" "-" "Registrierte URL einer Datenquelle"]
              ["/fds/data-sources/ID" "DELETE" "-" "Lösche URL einer Datenquelle"]]
        ] 
    (layout
      [:div.span2]
      [:div.span10 [:h3 "REST für Federated Data Service"]
       [:table.table
        [:tr [:th "URL"] [:th "HTTP-Methode"] [:th "Parameter"] [:th "Erklärung"]]
        (for [doc docs]
          [:tr (for [d doc] [:td d])])]])))

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
