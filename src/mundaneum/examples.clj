(ns mundaneum.examples
  (:require [mundaneum.query    :refer [describe entity label property query *default-language*]]
            [backtick           :refer [template]]
            [clj-time.format    :as    tf]))

;; To understand what's happening here, it would be a good idea to
;; read through this document:

;; https://m.wikidata.org/wiki/Wikidata:SPARQL_query_service/queries

;; The first challenge when using Wikidata is finding the right IDs
;; for the properties and entities you must use to phrase your
;; question properly. We have functions to help:

(entity "U2")

;; Now we know the ID for U2... or do we? Which U2 is it, really?

(describe (entity "U2"))

(entity "U2" :part-of (entity "Berlin U-Bahn"))

(describe (entity "U2" :part-of (entity "Berlin U-Bahn")))

;; We also have functions that turn keywords into property values:
(property :instance-of)
;;=> "P31"

;; ... but we use it indirectly through a set of helper functions
;; named for Wikidata namespaces, like wdt, p, ps and pq. The link
;; above will help you understand which one of these you might want
;; for a given query, but it's most often wdt.

;; we can ask which things contain an administrative territory and get
;; a list of the pairs filtered to the situation where the container
;; is Ireland
(query
 '[:select ?biggerLabel ?smallerLabel
   :where [[?bigger (wdt :contains-administrative-territorial-entity) ?smaller]
           :filter [?bigger = (entity "Ireland")]]
   :limit 10])

;; Discoveries/inventions grouped by person on the clojure side,
(->> (query
      '[:select ?thingLabel ?whomLabel
        :where [[?thing (wdt :discoverer-or-inventor) ?whom]]
        :limit 100])
     (group-by :whomLabel)
     (reduce #(assoc %1 (first %2) (mapv :thingLabel (second %2))) {}))

;; eye color popularity, grouping and counting as part of the query
(query
 '[:select ?eyeColorLabel (count ?person :as ?count)
   :where [[?person (wdt :eye-color) ?eyeColor] ]
   :group-by ?eyeColorLabel
   :order-by (desc ?count)])

;; U7 stations in Berlin w/ geo coords
(query (template
        [:select ?stationLabel ?coord
         :where [[?station (wdt :connecting-line) (entity "U7" :part-of ~(entity "Berlin U-Bahn"))
                  _ (wdt :coordinate-location) ?coord]]]))

;; born in Rome or territories thereof
(query
 '[:select ?itemLabel ?pobLabel
   :where [:union [[?item (wdt :place-of-birth) (entity "Rome")]
                   [[?item (wdt :place-of-birth) ?pob]
                    [?pob (wdt :located-in-the-administrative-territorial-entity) * (entity "Rome")]]]]
   :limit 10])


;; What places in Germany have names that end in -ow/-itz (indicating
;; that they were historically Slavic)
;;
;; (note the _, which translates to SPARQL's ; which means "use the
;; same subject as before")
(query
 '[:select *
   :where [[?ort (wdt :instance-of) / (wdt :subclass-of) * (entity "human settlement")
            _ (wdt :country) (entity "Germany")
            _ rdfs:label ?name
            _ (wdt :coordinate-location) ?wo]
           :filter ((lang ?name) = "de")
           :filter ((regex ?name "(ow|itz)$"))]
   :limit 10])
;;=>
;; [{:ort "Q6448", :wo "Point(13.716666666 50.993055555)", :name "Bannewitz"}
;;  {:ort "Q93223", :wo "Point(14.233333333 51.233333333)", :name "Crostwitz"}
;;  {:ort "Q160693", :wo "Point(14.2275 51.2475)", :name "Caseritz"}
;;  {:ort "Q160779", :wo "Point(14.2628 51.2339)", :name "Prautitz"}
;;  {:ort "Q162721", :wo "Point(14.265 51.2247)", :name "Nucknitz"}
;;  {:ort "Q2795", :wo "Point(12.9222 50.8351)", :name "Chemnitz"}
;;  {:ort "Q115077", :wo "Point(13.5589 54.5586)", :name "Quoltitz"}
;;  {:ort "Q160799", :wo "Point(14.43713889 51.79291667)", :name "Groß Lieskow"}
;;  {:ort "Q318609", :wo "Point(7.28119 53.4654)", :name "Abelitz"}
;;  {:ort "Q1124721", :wo "Point(13.3096 53.7516)", :name "Conerow"}]

;; WikiData is multilingual! Here's a query to list species of Swift
;; (the bird) with their English and German (and often Latin) names
(query
 (template [:select ?englishName ?germanName
            :where [[?item (wdt :parent-taxon) (entity "Apodiformes")]
                    [?item rdfs:label ?germanName]
                    [?item rdfs:label ?englishName]
                    :filter ((lang ?germanName) = "de")
                    :filter ((lang ?englishName) = "en")]
            :limit 10]))
;;=>
;; [{:germanName "Jungornithidae", :englishName "Jungornithidae"}
;;  {:germanName "Eocypselus", :englishName "Eocypselus"}
;;  {:germanName "Eocypselidae", :englishName "Eocypselidae"}
;;  {:germanName "Segler", :englishName "Apodidae"}
;;  {:germanName "Höhlenschwalme", :englishName "Aegothelidae"}
;;  {:germanName "Aegialornithidae", :englishName "Aegialornithidae"}
;;  {:germanName "Apodi", :englishName "Apodi"}
;;  {:germanName "Baumsegler", :englishName "treeswift"}
;;  {:germanName "Kolibris", :englishName "Trochilidae"}]

;; We can also use triples to find out about analogies in the dataset
(defn make-analogy
  "Return known analogies for the form `a1` is to `a2` as `b1` is to ???"
  [a1 a2 b1]
  (->> (query
        (template [:select ?isto ?analogyLabel
                   :where [[~(symbol (str "wd:" a1)) ?isto ~(symbol (str "wd:" a2))]
                           [~(symbol (str "wd:" b1)) ?isto ?analogy]
                           ;; tightens analogies by requiring that a2/b2 be of the same kind,
                           ;; but loses some interesting loose analogies:
                           ;; [~(symbol (str "wd:" a2)) (wdt :instance-of) ?kind]
                           ;; [?analogy (wdt :instance-of) ?kind]
                           ]]))
       (map #(let [arc (label (:isto %))]
               (str (label a1)
                    " is <" arc "> to "
                    (label a2)
                    " as "
                    (label b1)
                    " is <" arc "> to " (:analogyLabel %))))
       distinct))

(make-analogy (entity "Paris")
              (entity "France")
              (entity "Berlin"))
;;=> ("Paris is <country> to France as Berlin is <country> to Germany"
;;    "Paris is <capital of> to France as Berlin is <capital of> to Germany")

(apply make-analogy (map entity ["The Beatles" "rock and roll" "Miles Davis"]))
;;=> ("The Beatles is <genre> to rock and roll as Miles Davis is <genre> to jazz")

(defn releases-since
  "Returns any creative works published since `year`/`month` by any `entities` known to Wikidata."
  [since-year since-month entities]
  (let [ents (map #(if (re-find #"^Q[\d]+" %) % (entity %)) entities)]
    (query
     (template [:select ?workLabel ?creatorLabel ?role ?date
                :where [[?work ?role ?creator _ (wdt :publication-date) ?date]
                        :union ~(mapv #(vector '?work '?role (symbol (str "wd:" %))) ents)
                        :filter ((year ?date) >= ~since-year)
                        :filter ((month ?date) >= ~since-month)]
                :order-by (asc ?date)]))))

(->> (releases-since 2019 1 ; year and month
                     ["Kelly Link" "Stromae" "Guillermo del Toro" "Hayao Miyazaki" "Lydia Davis"
                      "Werner Herzog" "Björk" "George Saunders" "Feist" "Andrew Bird" "Sofia Coppola"])
     (map #(select-keys % [:workLabel :creatorLabel]))
     distinct)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DRAGONS

;; (query
;;  '[:select ?awdLabel ?countryLabel  (count ?p :as ?count)
;;    :where [[?p (wdt :award-received) ?awd
;;             _  (wdt :place-of-birth) ?birthplace]
;;            [?awd (wdt :instance-of) (entity "Nobel Prize")]
;;            [?birthplace (wdt :country) ?country]]
;;    :group-by ?awdLabel ?countryLabel
;;    :order-by (desc ?count)])

;;(property :derived-from)


;; (defn humanize-releases
;;   "Make the data presentable."
;;   [releases]
;;   (->> (group-by :workLabel releases)
;;        (map (fn [[work roles]]
;;               [(:date (first roles))
;;                work
;;                (str (:creatorLabel (first roles))
;;                     " ("
;;                     (->> (map :role roles)
;;                          (map label)
;;                          distinct
;;                          (interpose "/")
;;                          (apply str))
;;                     ")")]))
;;        (sort-by first)
;;        (map #(conj (rest %) (tf/unparse (tf/formatter "d MMMM, yyyy") (first %))))))



;; A somewhat complicated question about presidential precedence,
;; which involves: querying against property statements and property
;; qualifiers, plus the use of _ (a stand-in for SPARQL's semicolon
;; operator, which is says "continue this expression using the same
;; entity"):
#_(query
   '[:select ?prevLabel
     :where [[(entity "Barack Obama") (p :position-held) ?pos]
             [?pos (ps :position-held) (entity "President of the United States of America")
              _ (pq :replaces) ?prev]]])
;;=>#{{:prevLabel "George W. Bush"}}

;; which can be trivially expanded to list all US presidents and their
;; predecessors
#_(query
   '[:select ?prezLabel ?prevLabel
     :where [[?prez (p :position-held) ?pos]
             [?pos (ps :position-held) (entity "President of the United States of America")
              _ (pq :replaces) ?prev]]])
;;=>#{{:prezLabel "John Tyler", :prevLabel "William Henry Harrison"}
;;    {:prezLabel "Gerald Ford", :prevLabel "Richard Nixon"}
;;    {:prezLabel "John Adams", :prevLabel "George Washington"}
;; ...


;; airports within 20km of Paris, use "around" service
;; (query
;;  '[:select ?place ?placeLabel ?location
;;    :where [[(entity "Paris") (wdt :coordinate-location) ?parisLoc]
;;            [?place (wdt :instance-of) (entity "airport")]
;;            :service wikibase:around [[?place (wdt :coordinate-location) ?location]
;;                                      [bd:serviceParam wikibase:center ?parisLoc]
;;                                      [bd:serviceParam wikibase:radius "20"]]]])
;; [{:place "Q1894366", :location "Point(2.191667 48.774167)", :placeLabel "Villacoublay Air Base"}
;;  {:place "Q1894366", :location "Point(2.19972222 48.77305556)", :placeLabel "Villacoublay Air Base"}
;; ...

;;;; new lexical queries, which are currently broken

;; SELECT WHERE {
;;   ?lexeme wdt:P5191 ?target; wikibase:lemma ?lexemeLabel.
;;   ?target wdt:P5191* wd:L2087; wikibase:lemma ?targetLabel.
;; }

;; (query
;;  '[:select ?lexeme ?lexemeLabel ?target ?targetLabel
;;    :where [[?lexeme (literal "wdt:P5191") ?target
;;             _  (literal "wikibase:lemma") ?lexemeLabel]
;;            [?target (literal "wdt:P5191*") (literal "wd:L2087")
;;             _  (literal "wikibase:lemma") ?targetLabel]]])


