(ns fds2app.data.generated
  (:require [fds2app.fds :as fds]
            [org.clojars.smee.util :refer (s2i)]))

(defrecord NaturalNumber [n]
  fds/Fds-Node
  (id [_] (str "natural-number-" n))
  (type [_] (.getName Integer/TYPE))
  (properties [_] {:value n})
  (relations [this] 
             {:succ [(->NaturalNumber (inc n))] 
              :pred [(->NaturalNumber (dec n))]})
  (relations [this t] 
             (select-keys (fds/relations this) [t])))


(defn new-number [n]
  (NaturalNumber. n))

(defn by-id [^String id]
  (let [[_ n] (re-find #"natural-number-([-]?\d+)" id)]
    (when n
      (new-number (s2i n)))))