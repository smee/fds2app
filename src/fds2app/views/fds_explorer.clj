(ns fds2app.views.fds-explorer
  (:use 
    [clojure.core.incubator :only (-?>)] 
    [noir 
     [core :only (defpage defpartial)]
     [response :only (json content-type)]
     [session :only (flash-get flash-put!)]]
    [hiccup 
     [element :only (link-to unordered-list)]
     [util :only (url url-encode)]]
    [org.clojars.smee.map :only (map-values)])
  (:require 
    [fds2app.fds :as f]
    [fds2app.views 
     [common :refer (layout-with-links layout)]
     [rest :refer (node2json root-node)]]
    [ring.util.mime-type :as mime]
    [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;  Federated data as JSON

(defpage "/fds.json" {:keys [id]}
  (let [node (root-node)] 
    (if id
      (node2json (f/find-by-id id node))
      (node2json node))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; host documents
(def office-mime-types 
  {"xlsx"   "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
   "xltx"   "application/vnd.openxmlformats-officedocument.spreadsheetml.template"
   "potx"   "application/vnd.openxmlformats-officedocument.presentationml.template"
   "ppsx"   "application/vnd.openxmlformats-officedocument.presentationml.slideshow"
   "pptx"   "application/vnd.openxmlformats-officedocument.presentationml.presentation"
   "sldx"   "application/vnd.openxmlformats-officedocument.presentationml.slide"
   "docx"   "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
   "dotx"   "application/vnd.openxmlformats-officedocument.wordprocessingml.template"
   "xlam"   "application/vnd.ms-excel.addin.macroEnabled.12"
   "xlsb"   "application/vnd.ms-excel.sheet.binary.macroEnabled.12})))"})

(defpage "/fds/document" {:keys [id]}
  (if-let [file (-> id (f/find-by-id (root-node)) f/properties :file (java.io.File.))]
    (content-type (mime/ext-mime-type 
                    (.getName file)
                    office-mime-types)
                  (io/input-stream file))
    {:status 400
     :body "Invalid id!"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; ### HTML page

(defpartial map->table [m]
  [:table.table.table-striped.table-condensed
   [:tr [:th "Schlüssel"] [:th "Wert"]] 
   (for [[k v] m]
     [:tr [:td k] [:td v]])])

(defpartial seqs->table [headers seqs]
  [:table.table.table-striped.table-condensed
   [:tr (for [h headers] [:th h])] 
   (for [vs seqs]
     [:tr (for [v vs] 
            [:td v])])])

(defn- breadcrumb-links 
  "Add link to current page to session, create vector of links to the last 8 visited pages."
  [crnt-node]
  (let [crumbs (or (flash-get :breadcrumbs) [])
        next-link (link-to (str "/fds.html?id=" (url-encode (f/id crnt-node))) (f/type crnt-node))
        next-crumbs (->> next-link (conj crumbs) (partition-by identity) (map first) (take-last 8) vec)]
    (flash-put! :breadcrumbs next-crumbs)
    next-crumbs))

(defpage "/fds.html" {:keys [id]}
  (let[root (root-node)
       node (if (not-empty id) (f/find-by-id id root) root)
       links (for [[k vs] (f/relations node), v vs]
               [k (f/type v) (link-to (str "/fds.html?id=" (url-encode (f/id v))) "Link")])]
    (layout-with-links
      ;; top links
      [0 [:a {:href "/"} "Home"] [:a {:href "mailto:sdienst@informatik.uni-leipzig.de"} "Kontakt"]]
      ;; bread crumb trail
      (breadcrumb-links node)
      ;; left side bar
      [:div.span2 
       [:h4 "Federated data system"]
       "Anzeige virtuell integrierter Daten bezüglich einer Energieerzeugungsanlage"]
      ;; main contents
      [:div.span10
       [:h3 "Inhalt"]
       (map->table (f/properties node))
       [:h4 "Weiterführende Informationen"]
       (seqs->table ["Referenzart" "Knotenart" "Link"] links)])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; ### Main page - documentation
(defpage "/" []
  (layout
    [:h3 "Zugriff auf föderierte Daten"]
    (unordered-list
      [(link-to "/fds.json" "REST-Schnittstelle für föderierte Daten. Optionaler Parameter \"id\" für den Zugriff auf einen bestimmten Knoten")
       (link-to "/fds.html" "Weboberfläche für föderierte Daten. Optionaler Parameter \"id\" für den Zugriff auf einen bestimmten Knoten")])
    [:h3 "Registrierung von weiteren Datenquellen per REST"]
    (unordered-list
      [(link-to "/fds/doc" "Dokumentation REST für externe Datenquellen")
       (link-to "/sample-data" "Beispiel für eine REST Datenquelle")])))
