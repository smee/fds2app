(ns fds2app.data.rds-pp
  (:require [org.clojars.smee.map :refer (map-values)]
            [clojure.string :refer (trim)]))

(defn init-swi [file] 
  (jpl.Query/hasSolution (format "reconsult('%s')." file)))


(defmulti fix-result class)
(defmethod fix-result jpl.Atom [val]
  (.name val))
(defmethod fix-result jpl.Compound [c]
  (->> (.args c) seq (map fix-result) flatten (map trim)))
(defmethod fix-result nil [_]
  "Fehler: Unbekannter Bezeichner!")

(defn query-rds [rds-pp-key]
  (let [results (into {} (jpl.Query/oneSolution 
                           (format "rds_clj('%s',Result)." rds-pp-key)))] 
    (when results (-> results vals first fix-result))))


(init-swi "../rds-pp-parser/rds_main.pl")

(comment
  
  (time (dotimes [_ 10000] (query-rds "#PV01.L1SB0A =G001 MDL10 BT012 -WD001 QQ321 &MBB100/D00141")))
  
  (def x (jpl.Query/allSolutions "member(X, [\"test\",5,6,7,1])."))
  (seq x)
  (count x)
  )