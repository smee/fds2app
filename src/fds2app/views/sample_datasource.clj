(ns ^{:doc "Sample REST data source. Provides Fds-Nodes that count the number of properties of other Fds-Nodes.
Also they have two relations themselves: predecessor and successor."}
     fds2app.views.sample-datasource
  (:use [noir
         [core :only (defpage)]
         [response :only (json)]]
        [fds2app.serialize :only (fds->map)])
  (:require
    [fds2app.data.generated :as gen]
    [cheshire.core :as json]))

(defpage "/sample-data" []
  "Return some integer numbers. When asked for relations, returns the count of the node's properties."
  "There are three sub-urls: nodes, nodes/:id and relations.")


(defpage "/sample-data/nodes" []
  (json (map fds->map [(gen/new-number 99) (gen/new-number 0) (gen/new-number 5000)])))


(defpage "/sample-data/nodes/:id" {:keys [id]}
  (if-let [node (gen/by-id id)]
    (json (fds->map node))
    {:status 400
     :body "invalid id!"}))


(defpage [:post "/sample-data/relations"] {:keys [node relation-type]}
  (let [{:strs [id type properties relations] :as node} (json/parse-string node)]
    (println relation-type)
    (case relation-type
      ("some-integer" "" nil) (json {:some-integer [(fds->map (gen/new-number (count (keys properties))))]})
      (json []))))

