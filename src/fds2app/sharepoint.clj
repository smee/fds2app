(ns fds2app.sharepoint
  (:use [org.clojars.smee
         [file :only (find-files)]
         [util :only (ignore-exceptions)]]
        [clojure.core.match :only (match)]
        clojure.core.match.regex)
  #_(:require [net.cgrand.parsley :as p]))

(def doc-type-descriptions
  {"PMD" {:description "Projekt Management Dokument"
          "000" "Vorhabenbeschreibung"
          "010" "Kooperationsvereinbarung"
          "050" "Ansprechpartner"
          "100" "LOP (Liste offener Punkte)"
          "200" "Terminpläne"
          "300" "Protokolle"
          "310" "Statusberichte"
          "315" "Zwischenberichte"
          "320" "Memoranden"
          "400" "RESERVE"
          "500" "Pressemitteilungen"
          "510" "Projektsteckbrief / Flyer"
          "520" "Bilder zur Öffentlichkeitsarbeit"}
   "RFD"	{:description "Referenzdokument"
          "000" "Definitionen (Dokumentation)"
          "005" "Definitionen (Abkürzungen, ...)"
          "010" "Definitionen (Fachtermini, ...)"
          "015" "Definitionen (Daten)"
          "020" "Definitionen (Rollen)"
          "025" "Definitionen (Systeme)"
          "100" "Information (Standards, Bezeichnungssysteme, …)"}
   "DMD"	{:description "Entwicklung begleitendes Dokument"
          "000" "Arbeitskreise"
          "100" "Arbeitsgruppen"
          "200" "Studien - allgemein / übergreifend"
          "210" "Machbarkeitsstudien"
          "215" "Konzeptstudien"
          "300" "RESERVE"
          "400" "Abfragedokumente"
          "500" "Ergebnisdokumente"}
   "SRD"	{:description "Lastenheft"
          "000" "Vorwort"
          "020" "Einführung"
          "040" "Ziele"
          "060" "Markt, Wettbewerb und Portfolio"
          "080" "Vermarktung"
          "100" "Wirtschaftliche Daten"
          "120" "Risiken und offene Punkte"
          "140" "Beschreibung der Ist-Situation"
          "160" "Anforderungen an Zielsystem"
          "180" "Beziehungen zu anderen Anwendungsgebieten"
          "200" "Appendix"
          "220" "Product Master Data"
          "240" "Supply Chain Management"}
   "SAD"	{:description "Architekturdokument"}
   "FSD"	{:description "Pflichtenheft"
          "000" "Vorwort"
          "020" "Pflichtenheft - Einführung"
          "040" "Kommerzialisierung"
          "060" "Kosten"
          "080" "Risiken und offene Punkte"
          "100" "Systemarchitektur"
          "120" "Hardware-Funktionen"
          "140" "Software-Funktionen"
          "160" "Qualität"
          "180" "Ergänzungsfunktionen"
          "200" "Product Master Data"
          "220" "Supply Chain Management"}
   "DSD"	{:description "detaillierte Spezifikationen für die Realisierung"}})

(def energy-type
  {"0" "übergreifend"
   "2" "Wind"
   "4" "Solar"
   "6" "Bio"})

(def partner-shortcuts
  {"BE"	"bse Engineering Leipzig GmbH"
   "FI" "Forschungsinstitut für Rationalisierung an der RWTH Aachen"
   "IN" "Institut für Angewandte Informatik e. V. an der Universität Leipzig"
   "NX" "Nordex Energy GmbH"
   "PO" "Provedo GmbH"
   "PA" "PSIPENTA Software Systems GmbH"
   "PM" "psm Nature Power Service & Management GmbH & Co. KG"
   "ST" "SCHOTT Solar AG"
   "SI" "Siemens Aktiengesellschaft"
   "SK" "SKF Maintenance Services GmbH"
   "UL" "Universität Leipzig"
   "UV" "UV Sachsen Projektentwicklungs- und Verwaltungsgesellschaft mbH"})

(defn- read-sharepoint [root-path]
  (find-files root-path))

(defn valid-name? [file-name]
  (boolean (re-matches #"EUMONIS__\w{3}\.\d{3}\.\w{3}__.*__V_\d\d\.\d\d\.\w\w(__R_\d{8}\.\d{4}\.\w{3})?\.\w+" file-name)))

(defn- decode-file-name 
  "Decode EUMONIS file name format. Returns a map with translated codes."
  [file-name]
  (let [[_ file-name file-type] (re-find #"(.*)\.(.*)" file-name) 
        [_ eum-type name version review] (.split file-name "__")
        [_ version language] (re-find #"V_(.*)\.(\w+)" version)
        [doc-type maj min] (.split eum-type "\\.")
        [_ energy partner] (re-find #"(.)(..)" min)
        [_ date time person] (when review (re-find #"R_(\d{8})\.(\d{4})\.(.*)" review))]
    {:name name
     :file file-name
     :mime file-type
     :doc-type {:main (get-in doc-type-descriptions [doc-type :description])
                :detail (get-in doc-type-descriptions [doc-type maj])
                :energy (energy-type energy)
                :partner (partner-shortcuts partner)}
     :version {:number version
               :review (when review 
                         {:date date
                          :time time
                          :reviewer person})
               :language language}
     :status (cond 
               review "under-review"
               (and (not (re-matches #"..\.00" version))) "draft"
               :else "released")
     }))

(def ^:private sharepoint-files (read-sharepoint "x:/"))
(def ^:private sharepoint-names (->> sharepoint-files
                                  (map #(.getName %))
                                  (filter valid-name?)
                                  (pmap decode-file-name)))

(defn find-matching-files [query]
  ;; eval is needed to make sure that query is really the value when calling the 'match' macro
  (keep  identity (pmap #(eval `(match [~%] [~query] ~%)) sharepoint-names)))

(comment 
  
  (def f (read-sharepoint "x:/"))
  (count 
    (filter #(match [%] 
                    [{:status :released 
                      :version {:number "03.00"} 
                      :mime "pdf"}] %)
            (map #(ignore-exceptions (decode-file-name (.getName %))) f))
    )
  )
#_(def eumonis-file-name-parser
  (p/parser {:main :filename
             ;:space :__?
             :root-tag :root}
            ;:__ "__"
            :filename [:tag :file-type :name :version :review?]
            :tag "EUMONIS"
            :file-type #"\w{3}\.\d{3}\.\w{3}"
            :name #".*"
            :version #"V_\d\d\.\d\d\.\w\w"
            :review  #"R_\d{8}\.\d{4}\..*"))