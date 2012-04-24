(ns fds2app.data.generated
  (:require [fds2app.fds :as f]))

(defrecord NaturalNumber [n]
  f/Fds-Node
  (id [_] (str "natural-number-" n))
  (type [_] (.getName Integer/TYPE))
  (properties [_] {:value n})
  (relations [this] [(->NaturalNumber (inc n))]))


