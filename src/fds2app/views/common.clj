;; Common html layout via hiccup.
(ns fds2app.views.common
  (:use [noir core
         [options :only (resolve-url)]]
        [hiccup core
         [element]
         [page :only (html5 include-css include-js)]])
  (:require noir.request))

(defn base-url []
  (or (noir.options/get :base-url) ""))

;; Header.
(defpartial eumonis-header []
  [:head 
   [:title "MBS_SE_PV"]
   (include-css "/css/bootstrap.min.css"
                "/css/showLoading.css"
                "/css/customizations.css"
                )
   ;(include-js "/js/bootstrap-modal.js")
   [:link {:rel "shortcut icon" :href (str (fds2app.views.common/base-url) "/img/favicon.ico")}]])

;; Link bar
(defpartial eumonis-topbar [[active-idx & links]]
  [:div.navbar
   [:div.navbar-inner
    [:div.container
     [:a.brand {:href (str (fds2app.views.common/base-url) "/")} "EUMONIS - Federated Data System"]
     [:ul.nav
      (map-indexed #(if (= % active-idx) 
                      [:li.active %2] 
                      [:li %2]) 
                   links)]]]])

;; Footer
(defpartial eumonis-footer []
  [:footer
   [:div.span2 [:p "&#169; EUMONIS-Konsortium 2012"]]
    [:div.span8 [:p "Das Projekt \"EUMONIS\" wird gef&#246;rdert durch das
			Bundesministerium f&#252;r Bildung und Forschung (BMBF) -
			F&#246;rderkennzeichen 01IS10033, Laufzeit 01.07.2010 bis
			30.06.2014."]]
    [:div.span2
     (link-to "http://www.bmbf.de/" 
              [:img {:src (resolve-url "/img/bmbf-ohne-rand.gif") 
                     :alt "gef&#246;rdert durch das Bundesministerium f&#252;r Bildung und Forschung"
                     :width "150px"}])]])

;; Breadcrumb shows the last x visited links. This allows for easier navigation to former pages.
(defpartial breadcrumb [links]
  (when links
    [:ul.breadcrumb
     [:li "Navigationsverlauf: "]
     (for [link links]
       [:li [:span.divider "→"] link])]))

;; Main layout
(defn layout-with-links [topbar-links breadcrumb-links sidebar-contents & contents]
  (html5
    (eumonis-header)
    [:body
     (eumonis-topbar topbar-links)
     (breadcrumb breadcrumb-links) 
     [:div.container-fluid
      [:div.row-fluid 
       sidebar-contents
       contents]
      (eumonis-footer)]]))

;; Simplified layout with default topbar links.
(defn layout [& contents]
  (apply layout-with-links [0 [:a {:href "#"} "Home"] [:a {:href "#contact"} "Kontakt"]]
         nil
         nil 
         contents))


(defn absolute-url
  "Construct absolute url for current request."
  ([] (absolute-url (noir.request/ring-request)))
  ([{:keys [scheme server-port uri server-name]}] (format "%s://%s:%d%s" (name scheme) server-name server-port uri)))

