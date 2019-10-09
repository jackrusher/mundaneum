(ns mundaneum.properties
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.data.json :as json]))

;; Properties fetched from:
;; from http://quarry.wmflabs.org/run/45013/output/1/json
(def properties
  (->> (json/read (io/reader (io/resource "props-2019-08-18.json")) :key-fn keyword)
       :rows
       (reduce (fn [m [id kw]]
                 (assoc m
                        (-> kw
                            (string/replace #"[ /]" "-")
                            (string/replace #"[\(\)\'\,]" "")
                            keyword)
                        id))
               {})))

