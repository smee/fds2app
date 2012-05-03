(ns fds2app.views.rest
  (:use [clojure.string :only (split)] 
        [noir
         [core :only (defpage defpartial)]
         [response :only (json)]]
        [fds2app.views 
         [common :only (layout absolute-url)]]
        [org.clojars.smee.map :only (map-values)])
  (:require [fds2app.fds :as f]
            [fds2app.data
             [events :as ev]
             [stammbaum :as st]
             [documents :as d]]
            [cemerick.url :as u]
            [clj-http.client :as client]
            [cheshire.core :as json])
  (:import [java.security NoSuchAlgorithmException MessageDigest]
           java.math.BigInteger))

(defn fds->map [fds-node]
  {:id (f/id fds-node)
   :type (f/type fds-node)
   :properties (f/properties fds-node)
   :relations (map-values (fn [nodes] (reduce #(update-in % [(f/type %2)] conj (f/id %2)) {} nodes)) (f/relations fds-node))})

(defn node2json 
  "Render Fds-Node as JSON. The relations get serialized as a nested map of relation types to maps of node types to lists of node ids."
  [fds-node]
  (json (fds->map fds-node)))

(defn nodes2json 
  "Render Fds-Node as JSON. The relations get serialized as a nested map of relation types to maps of node types to lists of node ids."
  [& fds-nodes]
  (json (map fds->map fds-nodes)))

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

;;;;;;;;;;;;;;;;;;;;;;;;;; REST data source management ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord RemoteDataSource [name url])
(defonce ^:private data-sources (ref {}))

(defpage [:get "/fds/data-sources"] []
  (json @data-sources))

(defpage [:delete "/fds/data-sources/:id"] {:keys [id]}
  (dosync (alter data-sources dissoc id))
  {:status 200})

(defpage [:get "/fds/data-sources/:id"] {:keys [id]}
  (json (@data-sources id)))

(defpage [:post "/fds/data-sources"] {:keys [callback-url name]}
  (if (and (not-empty callback-url) (not-empty name)) 
    (let [id (md5 (.getBytes callback-url))]
      (dosync (alter data-sources assoc id  (RemoteDataSource. name callback-url)))
      {:status 200
       :body (str "/fds/data-sources/" id)})
    {:status 400
     :body "missing parameter 'callback-url' and 'name'"}))

;;;;;;;;;;;;;;;;;;;;;; Federated Data ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:private separator "<>")
(defn- fix-remote-id [prefix {:strs [id] :as node-map}]
  (let [node-map (assoc node-map "id" (str prefix separator id))]
    (map-values keyword identity node-map)))

(defn- find-remote-source [id]
  (let [[source-id id] (.split id (str separator))]
    (@data-sources source-id)))

;(defn- )
(defrecord RemoteNode [id type properties relations]
  f/Fds-Node
  (id [_] id)
  (type [_] type)
  (properties [_] properties)
  (relations [_] {})
  (relations [_ t] {}))

(defn- remote-find-relations
  "Create a function that queries a remote data source for relations to the given node."
  [node & [relation-type]]
  (let [params {:node (json/generate-string (fds->map node))}
        params (if relation-type 
                 (assoc params :relation-type relation-type) 
                 params)]    
    (reduce-kv 
      (fn [relations source-id {:keys [url]}]
        (let [rel-url (str (u/url url "relations"))
              ;asking remote data source at url rel-url via REST
              response (client/post rel-url 
                                    {:form-params params 
                                     :socket-timeout 1000
                                     :conn-timeout 1000
                                     :accept :json})
              ;parse response
              serialized-relations (json/parse-string (:body response))
              ;replace ids with ids that contain the id of the datasource
              ;so we know where to look later on
              ids-fixed (map-values #(map (partial fix-remote-id source-id) %) serialized-relations)
              _ (prn ids-fixed)
              _ (prn (map-values (partial map map->RemoteNode) ids-fixed))
              _ (prn relations)
              ]
        (merge-with concat relations (map-values (partial map map->RemoteNode) ids-fixed))))
      {}
      @data-sources)))

;;;;;;;;;;;;;;;;;;;;;;;;;; internal root node ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private internal-root-node 
  (let [event-list (ev/read-events "sample-data/events.csv")
        park (st/stammbaum-fds "sample-data/komponenten-sea1.xml")]
    (f/enhanced-tree event-list (st/component-finder park) d/join-documents remote-find-relations)))

(defn root-node 
  "Get root node of the contents of this federated data server."
  []
  internal-root-node)

;;;;;;;;;;;;;;;;;;;;;;;;;;; Documentation page ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defpartial rest-documentation [docs]
  [:table.table
   [:tr [:th "URL"] [:th "HTTP-Methode"] [:th "Parameter"] [:th "Erläuterung"]]
   (for [doc docs]
     [:tr (for [d doc] [:td d])])]) 

(defpage "/fds/doc" []
  (let [registration-docs
        [[(str (absolute-url) "/fds/data-sources") "GET" "-" "JSON mit IDs und URLs aller bekannten Datenquellen"]
         [(str (absolute-url) "/fds/data-sources") "POST" "callback-url, name" "Füge eine neue Datenquelle hinzu. Liefert die URL fuer die neue Datenquelle."]
         [(str (absolute-url) "/fds/data-sources/ID") "GET" "-" "Registrierte URL einer Datenquelle"]
         [(str (absolute-url) "/fds/data-sources/ID") "DELETE" "-" "Lösche URL einer Datenquelle"]]
        data-source-docs
        [[".../nodes" "GET" "-" "Liste der Wurzelknoten dieser Datenquelle"]
         [".../nodes/:id" "GET" "-" "\":id\" wird durch eine ID eines Datenknotens ersetzt. Liefert den vollständigen Datenknoten zu dieser ID zurück."]
         [".../relations" "POST" "node, relation-type (optional)" "Liefere mit dem übergebenen Datenknoten verknüpfte Informationen aus dieser Datenquelle zurück."]]]
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