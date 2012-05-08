(ns fds2app.views.rest
  (:use [clojure.string :only (split)] 
        [noir
         [core :only (defpage defpartial)]
         [response :only (json)]]
        [fds2app.views 
         [common :only (layout absolute-url)]]
        [fds2app.serialize :only (fds->map)]
        [org.clojars.smee 
         [map :only (map-values)]
         [util :only (md5)]])
  (:require [fds2app.fds :as f]
            [fds2app.data
             [events :as ev]
             [stammbaum :as st]
             [documents :as d]
             [rest-proxy :as rest]]
            [cemerick.url :as u]))

(defn node2json 
  "Render Fds-Node as JSON. The relations get serialized as a nested map of relation types to maps of node types to lists of node ids."
  [fds-node]
  (json (fds->map fds-node)))

(defn nodes2json 
  "Render Fds-Node as JSON. The relations get serialized as a nested map of relation types to maps of node types to lists of node ids."
  [& fds-nodes]
  (json (map fds->map fds-nodes)))

;; ## REST data source management - register remote REST data sources 

(defpage [:get "/fds/data-sources"] []
  (json @rest/data-sources))

(defpage [:delete "/fds/data-sources/:id"] {:keys [id]}
  (dosync (alter rest/data-sources dissoc id))
  {:status 200})

(defpage [:get "/fds/data-sources/:id"] {:keys [id]}
  (json (@rest/data-sources id)))

(defpage [:post "/fds/data-sources"] {:keys [callback-url name]}
  (if (and (not-empty callback-url) (not-empty name)) 
    (let [id (subs (md5 (.getBytes (str callback-url name))) 0 8)]
      (dosync (alter rest/data-sources assoc id  (rest/->RemoteDataSource name callback-url)))
      {:status 200
       :body (str "/fds/data-sources/" id)})
    {:status 400
     :body "missing parameter 'callback-url' and 'name'"}))



;; ## internal root node 

(def ^:private internal-root-node 
  (let [event-list (ev/read-events "sample-data/events.csv")
        park (st/stammbaum-fds "sample-data/komponenten-sea1.xml")]
    (f/enhanced-tree event-list (st/component-finder park) d/join-documents rest/remote-find-relations)))

(defn root-node 
  "Get root node of the contents of this federated data server."
  []
  internal-root-node)

;;;;;;;;;;;;;;;;;;;;;;;;;;; Documentation page 
(defpartial rest-documentation [docs]
  [:table.table
   [:tr [:th "URL"] [:th "HTTP-Methode"] [:th "Parameter"] [:th "Erläuterung"]]
   (for [doc docs]
     [:tr (for [d doc] [:td d])])]) 

(defpage "/fds/doc" []
  (let [registration-docs
        [[(str (u/url (absolute-url) "/fds/data-sources")) "GET" "-" "JSON mit IDs und URLs aller bekannten Datenquellen"]
         [(str (u/url (absolute-url) "/fds/data-sources")) "POST" "callback-url, name" "Füge eine neue Datenquelle hinzu. Liefert die URL fuer die neue Datenquelle."]
         [(str (u/url (absolute-url) "/fds/data-sources/ID")) "GET" "-" "Registrierte URL einer Datenquelle"]
         [(str (u/url (absolute-url) "/fds/data-sources/ID")) "DELETE" "-" "Lösche URL einer Datenquelle"]]
        data-source-docs
        [[".../nodes" "GET" "-" "Liste der Wurzelknoten dieser Datenquelle (bisher nicht verwendet)"]
         [".../nodes/:id" "GET" "-" "\":id\" wird durch eine ID eines Datenknotens ersetzt. Liefert den vollständigen Datenknoten zu dieser ID zurück."]
         [".../relations" "POST" "node, relation-type (optional)" 
          [:p "Liefere alle mit dem übergebenen Datenknoten verknüpfte Informationen aus dieser Datenquelle zurück.
Als Rückgabewert wird eine JSON-Map (Objekt) erwartet mit den Relationsarten als Schlüssel und als Werten wiederum Maps mit Knotentypen als Schlüsseln und Arrays mit IDs der Kindknoten. Siehe als Beispiel " [:a {:href"/fds.json"} "hier."]]]]]
    (layout
      [:div.span2]
      [:div.span10 
       [:h2 "REST für Federated Data Service"]
       "Informationen werden uniform mit Hilfe einer JSON-Serialisierung dargestellt. Struktur ist:"
       [:table.table [:tr [:th "Schlüssel"] [:th "Wert"]]
        [:tr [:td "id"] [:td "Eindeutiger interner Bezeichner dieses Informationsknotens"]]
        [:tr [:td "type"] [:td "Typ des Informationsknotens"]]
        [:tr [:td "properties"] [:td "Map mit beliebigen Informationen dieses Knotens"]]
        [:tr [:td "relations"] [:td "Relationen zu anderen Knoten. Map von Relationstypen auf Maps von Knotentypen auf Listen von Knoten-IDs."]]]
       [:h3 "REST-Datenquellen"]
       "Datenquellen müssen unter einer zu registrierenden Haupt-URL min. drei Sub-URLs bereitstellen:"
       (rest-documentation data-source-docs)
       [:h4 "Beispiel"]
       "siehe " [:a {:href"/sample-data"} "diese Datenquelle"] " für natürliche Zahlen"
       [:h3 "Registrierung von Datenquellen"]
       (rest-documentation registration-docs)])))

