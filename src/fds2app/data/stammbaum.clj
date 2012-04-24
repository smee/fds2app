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

(defn- id [loc]
  (xml1-> loc (attr :id)))


(defrecord Komponente [loc]
  fds/Fds-Node
  (relations   [_] (map #(Komponente. %) (xml-> loc children :Komponente)))
  (type       [_] :Komponentenbeschreibung)
  (id         [_] (id loc))
  (properties [this] {:Name        (xml1-> loc (attr :name))
                      :Einbaudatum (read-timestamp (xml1-> loc children :Einbaudatum text))
                      :Hersteller  (xml1-> loc children :Hersteller text)
                      :Komponenten (count (fds/relations this))}))

(defrecord Anlage [loc]
  fds/Fds-Node
  (relations   [_] (map #(Komponente. %) (xml-> loc children :Komponente)))
  (type       [_] :PV-Anlage)
  (id         [_] (id loc))
  (properties [this] {:Name        (xml1-> loc children :Name text)
                      :Komponenten (count (fds/relations this))
                      :Latitude    (xml1-> loc children :Koordinaten :Latitude text)
                      :Longitude   (xml1-> loc children :Koordinaten :Longitude text)}))

(defrecord Park [loc]
  fds/Fds-Node
  (relations   [_] (map #(Anlage. %) (xml-> loc children :Anlage)))
  (type       [_] :PV-Park)
  (id         [_] (id loc))
  (properties [this] {:Park     (xml1-> loc children :Name text)
                      :Standort (xml1-> loc children :Ort text)
                      :Anlagen  (count (fds/relations this))}))

(defn stammbaum-fds [file]
  (Park. (-> file io/input-stream xml/parse zip/xml-zip)))





(comment 
  (def x (zip-xml "sample-data/komponenten-sea1.xml"))
  (parse-timestamp (xml1-> component-node :Einbaudatum text)) 
  )
