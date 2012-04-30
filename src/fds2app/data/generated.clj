(ns fds2app.data.generated
  (:require [fds2app.fds :as fds]))

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


