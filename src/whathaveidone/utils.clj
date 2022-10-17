(ns whathaveidone.utils
  (:require [clojure.pprint :as pprint]))

(defn printr [val]
  (pprint/pprint val)
  val)
