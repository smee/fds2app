(ns fds2app.test.rds-pp)

(defn init-swi [file] 
  (jpl.Query/hasSolution (format "reconsult('%s')." file)))


(defn q [s]
  (jpl.Query/oneSolution s))

(defn query-rds [rds-pp-key]
  (let [key (.replaceAll rds-pp-key "\\s+" "")] 
    (into {} (jpl.Query/oneSolution (format "atom_chars('%s',X), rds_analyze(X,Result,Conj,Ref,Spec)." key)))))



(comment
  (init-swi "../rds-pp-parser/rds_main.pl")
  (clojure.pprint/pprint (query-rds "#RDS01.PP12 =MDL10BT012 &DDD123/S001"))
  
  (def x (jpl.Query/allSolutions "member(X, [\"test\",5,6,7,1])."))
  (seq x)
  (count x)
  )