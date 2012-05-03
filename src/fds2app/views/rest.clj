(ns fds2app.views.rest
  (:use [noir
         [core :only (defpage)]
         [response :only (json)]]
        [fds2app.views.common :only (layout)]
        [org.clojars.smee.map :only (map-values)])
  (:require [fds2app.fds :as f])
  (:import [java.security NoSuchAlgorithmException MessageDigest]
           java.math.BigInteger))

(defn node2json 
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


(def ^:private data-sources (ref {}))

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