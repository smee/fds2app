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

(deftype Komponente [loc]
  fds/fds-node
  (children [_] (map #(Komponente. %) (xml-> loc children :Komponente)))
  (type [_] :Komponentenbeschreibung)
  (id [_] (xml1-> loc (attr :id)))
  (properties [_] {:Name (xml1-> loc (attr :name))
                   :Einbaudatum (read-timestamp (xml1-> loc children :Einbaudatum text))
                   :Hersteller (xml1-> loc children :Hersteller text)})
  Object
  (toString [this] (format "{:id %s, :type %s, :properties %s}" 
                           (fds/id this)
                           (fds/type this) 
                           (pr-str (fds/properties this)))))

(deftype Anlage [loc]
  fds/fds-node
  (children [this] (map #(Komponente. %) (xml-> loc children :Komponente)))
  (type [this] :PV-Anlage)
  (id [this] (xml1-> loc children :Name text))
  (properties [_] {:park (xml1-> loc children :Park text)})
  
  Object
  (toString [this] (format "{:id %s, :type %s, :properties %s}" 
                           (fds/id this)
                           (fds/type this)
                           (pr-str (fds/properties this)))))

(defn stammbaum-fds [file]
  (Anlage. (-> file io/input-stream xml/parse zip/xml-zip)))


(comment 
  (def x (zip-xml "sample-data/komponenten-sea1.xml"))
  (def component-node (xml1-> x descendants [(attr= :id "temp")] :Komponente))
  (xml1-> component-node (attr :name))
  (parse-timestamp (xml1-> component-node :Einbaudatum text)) 
  (map :tag (xml1-> x children :Name text))
  
  (def anlage (stammbaum-fds "sample-data/komponenten-sea1.xml"))
  (-> (fds/find-by-id "fan1" anlage)) ;; => #<Komponente {:id fan1, :type :Komponentenbeschreibung, :properties {:Name "Lüfter", :Einbaudatum #inst "2002-05-30T09:00:00.000-00:00", :Hersteller "Aldi"}}>
  )