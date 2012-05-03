(ns fds2app.views.sample-datasource
  (:use [noir
         [core :only (defpage)]
         [response :only (json)]]
        [fds2app.views 
         [common :only (layout)]
         [rest :only (nodes2json node2json fds->map)]]
        [org.clojars.smee.util :only (s2i)])
  (:require [fds2app.fds :as f]
            [fds2app.data.generated :as gen]
            [clojure.java.io :as io]
            [cheshire.core :as json]))

(defpage "/sample-data" []
  "Return some integer numbers. When asked for relations, returns the count of the node's properties."
  "There are three sub-urls: nodes, nodes/:id and relations.")


(defpage "/sample-data/nodes" []
  (nodes2json (gen/new-number 99) (gen/new-number 0) (gen/new-number 5000)))


(defpage "/sample-data/nodes/:id" {:keys [id]}
  (if-let [node (gen/by-id id)] 
    (node2json node)
    {:status 400
     :body "invalid id!"}))


(defpage [:post "/sample-data/relations"] {:keys [node relation-type]}
  (let [{:strs [id type properties relations] :as node} (json/parse-string node)]
    (def n node)
    (case relation-type
      ("some-integer" "" nil) (json {:some-integer [(fds->map (gen/new-number (count (keys properties))))]})
      (json []))))

