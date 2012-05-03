(ns fds2app.views.rest
  (:use [noir
         [core :only (defpage defpartial)]
         [response :only (json)]]
        [fds2app.views 
         [common :only (layout absolute-url)]]
        [org.clojars.smee.map :only (map-values)])
  (:require [fds2app.fds :as f])
  (:import [java.security NoSuchAlgorithmException MessageDigest]
           java.math.BigInteger))

(defn- encode-node [fds-node]
  {:id (f/id fds-node)
   :type (f/type fds-node)
   :properties (f/properties fds-node)
   :relations (map-values (fn [nodes] (reduce #(update-in % [(f/type %2)] conj (f/id %2)) {} nodes)) (f/relations fds-node))})

(defn node2json 
  "Render Fds-Node as JSON. The relations get serialized as a nested map of relation types to maps of node types to lists of node ids."
  [fds-node]
  (json (encode-node fds-node)))
(defn nodes2json 
  "Render Fds-Node as JSON. The relations get serialized as a nested map of relation types to maps of node types to lists of node ids."
  [& fds-nodes]
  (json (map encode-node fds-nodes)))

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

;;;;;;;;;;;;;;;;;;;;;;;;;; Data source management ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private data-sources (ref {}))

(defpage [:get "/fds/data-sources"] []
  (json @data-sources))

(defpage [:delete "/fds/data-sources/:id"] {:keys [id]}
  (dosync (alter data-sources dissoc id))
  {:status 200})

(defpage [:get "/fds/data-sources/:id"] {:keys [id]}
  (json (@data-sources id)))

(defpage [:post "/fds/data-sources"] {:keys [callback-url]}
  (if (not-empty callback-url) 
    (let [id (md5 (.getBytes callback-url))]
      (dosync (alter data-sources assoc id callback-url))
      {:status 200
       :body (str "/fds/data-sources/" id)})
    {:status 400
     :body "missing parameter 'callback-url'"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;; Documentation page ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defpartial rest-documentation [docs]
  [:table.table
   [:tr [:th "URL"] [:th "HTTP-Methode"] [:th "Parameter"] [:th "Erläuterung"]]
   (for [doc docs]
     [:tr (for [d doc] [:td d])])]) 

(defpage "/fds/doc" []
  (let [registration-docs
        [[(str (absolute-url) "/fds/data-sources") "GET" "-" "JSON mit IDs und URLs aller bekannten Datenquellen"]
         [(str (absolute-url) "/fds/data-sources") "POST" "callback-url" "Füge eine neue Datenquelle hinzu. Liefert die URL fuer die neue Datenquelle."]
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
       [:h3 "REST-Datenquellen"]
       "Datenquellen müssen unter einer zu registrierenden Haupt-URL min. drei Sub-URLs bereitstellen:"
       (rest-documentation data-source-docs)
       [:h3 "Registrierung von Datenquellen"]
       (rest-documentation registration-docs)])))