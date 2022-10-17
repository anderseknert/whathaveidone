(ns whathaveidone.main
  (:require [cljc.java-time.local-date :as ld]
            [whathaveidone.github :as gh]))

(defn -main []
  (let [today (str (ld/now))]
    (println (gh/report today))))
