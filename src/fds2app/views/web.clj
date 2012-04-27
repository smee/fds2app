(ns fds2app.views.web
  (:require [fds2app.data.sharepoint :as sp]
            [fds2app.data.rds-pp :refer (query-rds)]
            clojure.walk
            [clojure.string :as string]
            [hiccup.form :as f]
            noir.request)
  (:use
    [cheshire.core :only (parse-string)]
    [noir 
     [core :only (defpage defpartial url-for)]
     [options :only (resolve-url)]
     [response :only (redirect json)]]
    [hiccup 
     [core :only (html)]
     [element :only (link-to javascript-tag)]]
    [org.clojars.smee.util :only (ignore-exceptions)]))

(defn- parse-query [query]
  (ignore-exceptions (parse-string query)))

(defpage [:post "/sharepoint"] {:keys [query]}
  (if-let [q (parse-query query)]
    (let [f (fn [[k v]]  [(keyword k) (if (string? v) (re-pattern v) v)]) ; replace keys with keywords, values with regexpressions
          q (clojure.walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) q)] ; walk query, replace keys/values
      (println "query: " (pr-str q))
      (json (sp/find-matching-files q)))
    {:status 400
     :body "invalid query!"}))

(defpage "/sharepoint" []
  (html 
    (f/form-to 
      [:post "/sharepoint"]
      (f/text-area {:cols 50 :rows 8} :query "{\"status\": \"released\", \n\"mime\": \"pdf\", \n\"version\": {\"number\": \"03.00\"}}")
      (f/submit-button "Suchen..."))))

(defn absolute-url
  "Construct absolute url for current request."
  ([] (absolute-url (noir.request/ring-request)))
  ([{:keys [scheme server-port uri server-name]}] (format "%s://%s:%d%s" (name scheme) server-name server-port uri)))

(defpage "/explore" []
  (absolute-url))

(defpage "/rds-pp" {:keys [key]}
  (if key 
    (let [res (query-rds key)]
      (-> res (string/replace "\n" "<br/>") (string/replace "\t" (apply str (repeat 4 "&nbsp;")))))
    (html
      [:h2 "Bedeutung von RDS-PP-Schlüsseln"]
      (f/form-to [:GET "/rds-pp"]
               (f/label "key" "Bitte geben Sie einen RDS-PP-Schlüssel ein: ") 
               (f/text-field {:size "90"} "key" "#PV01.L1SB0A =G001 MDL10 BT012 -WD001 QQ321 &MBB100/D00141")
               (f/submit-button "Übersetzen")))))