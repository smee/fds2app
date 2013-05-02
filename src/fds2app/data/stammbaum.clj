;; Datasource for component descriptions in XML.
(ns fds2app.data.stammbaum
  (:refer-clojure :exclude [descendants ancestors])
  (:require clojure.walk
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.java.io :as io]
            [fds2app.fds :as fds] )
  (use clojure.data.zip.xml
       clojure.data.zip))

(defn read-timestamp 
  "Parse time instant of format ISO8601 / RFC3339"
  [s]
  (->> s pr-str (str "#inst ") read-string))

(defn- id 
  "Find value of the xml attribute \"id\"."
  [loc]
  (xml1-> loc (attr :id)))


(defrecord Komponente [loc]
  fds/Fds-Node
  (relations  [_] (let [subcomponents (map #(Komponente. %) (xml-> loc children :Komponente))] 
                    (if (not-empty subcomponents) {:component subcomponents} {})))
  (relations  [this t] (select-keys (fds/relations this) [t]))
  (type       [_] :Komponentenbeschreibung)
  (id         [_] (id loc))
  (properties [this] {:Name        (xml1-> loc (attr :name))
                      :Einbaudatum (read-timestamp (xml1-> loc children :Einbaudatum text))
                      :Hersteller  (xml1-> loc children :Hersteller text)
                      :Komponenten (count (fds/relations this))}))

(defrecord Anlage [loc]
  fds/Fds-Node
  (relations  [_] (let [subcomponents (map #(Komponente. %) (xml-> loc children :Komponente))] 
                    (if (not-empty subcomponents) {:component subcomponents} {})))
  (relations  [this t] (select-keys (fds/relations this) [t]))
  (type       [_] :PV-Anlage)
  (id         [_] (id loc))
  (properties [this] {:Name        (xml1-> loc children :Name text)
                      :Komponenten (count (fds/relations this))
                      :Latitude    (xml1-> loc children :Koordinaten :Latitude text)
                      :Longitude   (xml1-> loc children :Koordinaten :Longitude text)}))

(defrecord Park [loc]
  fds/Fds-Node
  (relations  [_] {:plant (map #(Anlage. %) (xml-> loc children :Anlage))})
  (relations  [this t] (select-keys (fds/relations this) [t]))
  (type       [_] :PV-Park)
  (id         [_] (id loc))
  (properties [this] {:Park     (xml1-> loc children :Name text)
                      :Standort (xml1-> loc children :Ort text)
                      :Anlagen  (count (fds/relations this))}))

(defn stammbaum-fds 
  "Deserialize an XML file with component descriptions into instances of `fds2app.Fds-Node`."
  [file]
  (Park. (-> file io/input-stream xml/parse zip/xml-zip)))


(defn component-finder 
  "Connect data about components to events."
  [park]
  (fn [node] 
    (when-let [component (-> node fds/properties :component-id (fds/find-by-id park))] 
      {:component [component]})))
