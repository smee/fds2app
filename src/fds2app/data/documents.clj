(ns fds2app.data.documents
  (:require [fds2app.fds :as fds]
            [clojure.java.io :refer ()]
            [org.clojars.smee.file :refer (find-files)]
            [clojure.string :refer (join)]))

(defrecord Documentation [power-station-id component-id mime date type]
  fds/Fds-Node
  (id [_] (join "-" [power-station-id component-id mime date type]))
  (type [_] type)
  (properties [this] (into {} this))
  (relations [_] []))

(defn- extract-metadata [file]
  (let [[ps-id comp-id doc-type rest] (seq (.split (.getName file) "__"))
        [date type] (.split rest "\\.")]
    (->Documentation ps-id comp-id type date doc-type)))

(def ^:private all-docs (map extract-metadata (find-files "sample-data/documents/")))

(defn join-documents [node]
  (let [docs (filter #(and (= (-> % fds/properties :power-station-id)
                              (-> node fds/properties :references :power-station-id))
                           (= (-> % fds/properties :component-id)
                              (-> node fds/properties :references :component-id)))
                     all-docs)]
    docs))