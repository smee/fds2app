(ns fds2app.views.web
  (:require [fds2app.data.sharepoint :as sp]
           clojure.walk
           [hiccup.form :as form])
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
    (form/form-to 
      [:post "/sharepoint"]
      (form/text-area {:cols 50 :rows 8} :query "{\"status\": \"released\", \n\"mime\": \"pdf\", \n\"version\": {\"number\": \"03.00\"}}")
      (form/submit-button "Suchen..."))))

