(ns mundaneum.properties
  (:require [clojure.string :as string]
            [clojure.data.json :as json]))

;; Properties fetched from:
;; from http://quarry.wmflabs.org/run/45013/output/1/json
(def properties
  (->> (json/read (clojure.java.io/reader "resources/props-2019-08-18.json") :key-fn keyword)
       :rows
       (reduce (fn [m [id kw]]
                 (assoc m
                        (-> kw
                            (string/replace #"[ /]" "-")
                            (string/replace #"[\(\)\'\,]" "")
                            keyword)
                        id))
               {})))

