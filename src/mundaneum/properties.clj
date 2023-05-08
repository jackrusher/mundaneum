(ns mundaneum.properties
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.data.json :as json]))

;; Properties fetched using the wikibase command line tool:
;; https://github.com/maxlath/wikibase-cli
;; ... example invocations:
;; $ wb props > props-2021-11-04.json
;; (or via Docker):
;; $ docker run --rm -it maxlath/wikibase-cli props -e https://query.wikidata.org/sparql >props-2023-05-08.json
(def wdt
  (->> (json/read (io/reader (io/resource "props-2023-05-08.json")))
       (reduce (fn [m [id text]]
                 (assoc m
                        (-> text
                            (string/replace #"[ /]" "-")
                            (string/replace #"[\(\)\'\,;\"]" "")
                            keyword)
                        (keyword (str "wdt/" id))))
               {})))

;; reverse loookup
(def wdt->readable
  (reduce (fn [m [readable wdt]] (assoc m wdt readable))
          {}
          wdt))
