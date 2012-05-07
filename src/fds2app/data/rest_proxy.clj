(ns fds2app.data.rest-proxy
  (:require
    [cemerick.url :as u]
    [cheshire.core :as json]
    [fds2app.fds :as f]
    [clj-http.client :as client])
  (:use 
    [org.clojars.smee 
     [map :only (map-values)]]
    ))

(defrecord RemoteDataSource [name url])
(defonce data-sources (ref {}))

(defn fds->map 
  "Serialize an instance of fds2app.Fds-Node as a map."
  [fds-node]
  {:id (f/id fds-node)
   :type (f/type fds-node)
   :properties (f/properties fds-node)
   :relations (map-values (fn [nodes] (reduce #(update-in % [(f/type %2)] conj (f/id %2)) {} nodes)) (f/relations fds-node))})

;; ## Federated Data 
(def ^:private separator "<>")

(defn- replace-related-nodes 
  "Take a nested map of relation type to maps of node types to sequences of objects (ids, Fds-Nodes, ...). Transforms those
objects using f and returns the relations."
  [f relations]
  (map-values (fn [m] (map-values #(map f %) m)) relations))

(defn- encode-remote-ids 
  "Encode remote ids by prepending the name of the remote source, convert all json keys to keywords."
  [prefix {:strs [id] :as json-node}]
  (let [node-map (map-values keyword identity json-node)
        id-fix (partial str prefix separator)
        node-map (update-in node-map [:id] id-fix)
        rels (:relations node-map)
        fixed-rels (replace-related-nodes id-fix rels)]
    (assoc node-map :relations fixed-rels)))

(defn- decode-remote-id 
  "Split remote id into vector of [id, id of data source (key in @data-sources), real id]."
  [id]
  (let [[source-id id] (.split id (str separator))]
    [id source-id (@data-sources source-id)]))

(declare find-by-remote-id)

;; RemoteNode is a Fds-Node that represents a remote information via REST. 
(defrecord RemoteNode [id type properties relations]
  f/Fds-Node
  (id [_] id)
  (type [_] type)
  (properties [_] properties)
  (relations [_] (map-values (fn [m] (map find-by-remote-id (apply concat (vals m)))) relations))
  (relations [_ t] (map-values (fn [m] (map find-by-remote-id (apply concat (vals m)))) (select-keys relations [t]))))

(defn- find-by-remote-id 
  "Query remote REST datasource for a specific node by id."
  [remote-id]
  ;(println "fetching remote node for id" id)
  (let [[id source-id {:keys [url]}] (decode-remote-id remote-id)
        node-url (str (u/url url "nodes" id))
        response (client/get node-url)
        json-node (json/parse-string (:body response))]
    ;(println "got remote node" (pr-str json-node))
    (map->RemoteNode (encode-remote-ids source-id json-node))))

(defn remote-find-relations
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
              ids-fixed (map-values #(map (partial encode-remote-ids source-id) %) serialized-relations)]
          (merge-with concat relations (map-values (partial map map->RemoteNode) ids-fixed))))
      {}
      @data-sources)))

;; evaluate this line to add our local data REST data source
(comment
  (dosync (alter data-sources assoc "local" (RemoteDataSource. "localintegers" "http://localhost:8080/sample-data")))
  )