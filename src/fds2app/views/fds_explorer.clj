(ns fds2app.views.fds-explorer
  (:use 
    [noir 
     [core :only (defpage defpartial)]
     [response :only (json)]
     [session :only (flash-get flash-put!)]]
    [hiccup 
     [element :only (link-to unordered-list)]
     [util :only (url url-encode)]]
    [org.clojars.smee.map :only (map-values)])
  (:require 
    [clojure.string :as string] 
    [fds2app.fds :as f]
    [fds2app.views 
     [common :refer (layout-with-links layout)]
     [rest :refer (node2json root-node)]]
    [fds2app.data.rds-pp :as rds]
    [hiccup.form :as form]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;  Federated data as JSON

(defpage "/fds.json" {:keys [id]}
  (let [node (root-node)] 
    (if id
      (node2json (f/find-by-id id node))
      (node2json node))))
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
       (map->table (map-values str (f/properties node)))
       [:h4 "Weiterführende Informationen"]
       (seqs->table ["Referenzart" "Knotenart" "Link"] links)])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; ### Parser for RDS-PP
(defpage "/rds-pp" {:keys [key]}
  (if key 
    (let [res (rds/query-rds key)]
      (layout (-> res (string/replace "\n" "<br/>") (string/replace "\t" (apply str (repeat 4 "&nbsp;"))))))
    (layout
      [:h2 "Bedeutung von RDS-PP-Schlüsseln"]
      (form/form-to [:GET "/rds-pp"]
           (form/label "key" "Bitte geben Sie einen RDS-PP-Schlüssel ein: ") 
           (form/text-field {:class "span8"} "key" "#PV01.L1SB0A =G001 MDL10 BT012 -WD001 QQ321 &MBB100/D00141")
           (form/submit-button "Übersetzen")))))

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
       (link-to "/sample-data" "Beispiel für eine REST Datenquelle")])
    [:h3 "Weitere Funktionen"]
    (unordered-list
      [(link-to "/rds-pp" "Parsen von RDS-PP-Bezeichnern")])))
