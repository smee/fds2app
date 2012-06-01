;; Example data source that contains informations about documents (i.e. poor man's document management system).
;; Read document file name from local directory _sample-data/documents_, analyzes them and connects them
;; to events (see namespace fds2app.data.events).
(ns fds2app.data.documents
  (:require [fds2app.fds :as fds]
            [clojure.java.io :refer ()]
            [org.clojars.smee.file :refer (find-files)]
            [clojure.string :refer (join)]))

;; Record for informations about document file names.
(defrecord Documentation [power-station-id component-id mime date type file]
  fds/Fds-Node
  (id [_] (join "-" [power-station-id component-id date type mime]))
  (type [_] type)
  (properties [this] (assoc (into {} this) "Download" (format "<a href=\"/fds/document?id=%s\">Dokument</a>" (fds/id this))))
  (relations [_] {})
  (relations  [this t] {}))

(defn- extract-metadata 
  "Extracts information from file names. This functions expects file names with the following
parts separated by __ (double underscores):
id of power station / id of component / type of document / date
"
  [file]
  (let [[ps-id comp-id doc-type rest] (seq (.split (.getName file) "__"))
        [date type] (.split rest "\\.")]
    (->Documentation ps-id comp-id type date doc-type (.getAbsolutePath file))))

;; Statically read all documents once.
(def all-docs (map extract-metadata (find-files "sample-data/documents/")))

(defn join-documents 
  "Use this function as a parameter for `fds2app.fds/enhanced-tree`. It connects informations about documents
to components."
  [node]
  (let [docs (filter #(or 
                        (and  (= (-> % fds/properties :power-station-id)
                                 (-> node fds/properties :power-station-id))
                              (= (-> % fds/properties :component-id)
                                 (-> node fds/properties :component-id)))
                        (= (-> % fds/properties :component-id)
                           (fds/id node)))
                     all-docs)]
    (if (not-empty docs) 
      {:document docs}
      {})))
