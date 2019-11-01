(ns mundaneum.examples
  (:require [mundaneum.query    :refer [describe entity label property query stringify-query *default-language*]]
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
 '[:select ?areaLabel
   :where [[(entity "Ireland") (wdt :contains-administrative-territorial-entity) ?area]]
   :limit 50])

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
   :where [[?person (wdt :eye-color) ?eyeColor]]
   :group-by ?eyeColorLabel
   :order-by (desc ?count)])

;; U7 stations in Berlin w/ geo coords
(query (template
        [:select ?stationLabel ?coord
         :where [[?station (wdt :connecting-line) (entity "U7" :part-of ~(entity "Berlin U-Bahn"))
                  _ (wdt :coordinate-location) ?coord]]]))

;; born in ancient Rome or territories thereof
(->> (query
      '[:select ?itemLabel
        :where [:union [[[?item (wdt :place-of-birth) ?pob]
                         [?pob (wdt :located-in-the-administrative-territorial-entity) * (entity "ancient Rome")]]]]
        :limit 10])
     (map :itemLabel)
     (into #{}))
;;=> #{"Marcus Furius Camillus" "Avianus" "Porcia Catonis" "Faustina the Elder" "Hippolytus" "Sylvester I" "Lucius Caecilius Metellus Denter" "Lucius Junius Brutus" "Gaius Valarius Sabinus" "Publius Petronius Turpilianus"}

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
(query '[:select ?englishName ?germanName
         :where [[?item (wdt :parent-taxon) (entity "Apodiformes")
                  _ rdfs:label ?germanName
                  _ rdfs:label ?englishName]
                 :filter ((lang ?germanName) = "de")
                 :filter ((lang ?englishName) = "en")]
         :limit 10])
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
                           [~(symbol (str "wd:" b1)) ?isto ?analogy]]]))
                           ;; tightens analogies by requiring that a2/b2 be of the same kind,
                           ;; but loses some interesting loose analogies:
                           ;; [~(symbol (str "wd:" a2)) (wdt :instance-of) ?kind]
                           ;; [?analogy (wdt :instance-of) ?kind]

       (map #(let [arc (label (:isto %))]
               (str (label a1)
                    " is <" arc "> to "
                    (label a2)
                    " as "
                    (label b1)
                    " is <" arc "> to " (:analogyLabel %))))
       distinct))

(make-analogy (entity "Paris") (entity "France") (entity "Berlin"))
;;=> ("Paris is <country> to France as Berlin is <country> to Germany"
;;    "Paris is <capital of> to France as Berlin is <capital of> to Germany")

(apply make-analogy (map entity ["The Beatles" "rock and roll" "Miles Davis"]))
;;=> ("The Beatles is <genre> to rock and roll as Miles Davis is <genre> to jazz")

;; airports within 100km of Paris, use "around" service
(->>
 (query
  '[:select :distinct ?placeLabel 
    :where [[(entity "Paris") (wdt :coordinate-location) ?parisLoc]
            [?place (wdt :instance-of) (entity "airport")]
            :service wikibase:around [[?place (wdt :coordinate-location) ?location]
                                      [bd:serviceParam wikibase:center ?parisLoc]
                                      [bd:serviceParam wikibase:radius "100"]]]])
 (map :placeLabel)
 (into #{}))

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


;; How about something a bit more serious? Here we find drug-gene
;; product interactions and diseases for which these might be
;; candidates.
(->> (query
      '[:select ?drugLabel ?geneLabel ?diseaseLabel
        :where [[?drug (wdt :physically-interacts-with) ?gene_product]
                [?gene_product (wdt :encoded-by) ?gene]
                [?gene (wdt :genetic-association) ?disease]]
        :limit 100])
     (group-by :diseaseLabel)
     (mapv (fn [[k vs]] [k (mapv #(dissoc % :diseaseLabel) vs)]))
     (into {}))
;;=>
;; {"Parkinson disease"
;;  [{:drugLabel "SB-203580", :geneLabel "GAK"}],
;;  "obesity"
;;  [{:drugLabel "allopurinol", :geneLabel "XDH"}
;;   {:drugLabel "estriol", :geneLabel "ESR1"}
;;   {:drugLabel "tamoxifen", :geneLabel "ESR1"}
;;   {:drugLabel "17β-estradiol", :geneLabel "ESR1"}
;;    "malaria" [{:drugLabel "benserazide", :geneLabel "DDC"}],
;;  "systemic lupus erythematosus"
;;  [{:drugLabel "Hypothetical protein CT_814", :geneLabel "DDA1"}
;;   {:drugLabel "ibrutinib", :geneLabel "BLK"}],
;;  "Crohn's disease"
;;  [{:drugLabel "momelotinib", :geneLabel "JAK2"}
;;   {:drugLabel "pacritinib", :geneLabel "JAK2"}],
;;  "multiple sclerosis"
;;  [{:drugLabel "cabozantinib", :geneLabel "MET"}
;;   {:drugLabel "crizotinib", :geneLabel "MET"}
;;   {:drugLabel "tivantinib", :geneLabel "MET"}],
;;  "ulcerative colitis"
;;  [{:drugLabel "bumetanide", :geneLabel "GPR35"}
;;   {:drugLabel "furosemide", :geneLabel "GPR35"}],
;;  "hepatitis B"
;;  [{:drugLabel "L-aspartic Acid", :geneLabel "GRIN2A"}
;;   {:drugLabel "ketamine", :geneLabel "GRIN2A"}]},

(query
  '[:select ?drugLabel ?geneLabel ?diseaseLabel
    :where [[?drug (wdt :physically-interacts-with) ?gene_product]
            [?gene_product (wdt :encoded-by) ?gene]
            [?gene (wdt :genetic-association) ?disease]]
    :limit 10])
;;=>
;; [{:drugLabel "Hypothetical protein CTL0156", :geneLabel "TP53", :diseaseLabel "basal-cell carcinoma"}
;;  {:drugLabel "Hypothetical protein CT_788", :geneLabel "TP53", :diseaseLabel "basal-cell carcinoma"}
;;  {:drugLabel "Hypothetical protein CTL0156", :geneLabel "TP53", :diseaseLabel "head and neck squamous cell carcinoma"}
;;  {:drugLabel "Hypothetical protein CT_788", :geneLabel "TP53", :diseaseLabel "head and neck squamous cell carcinoma"}
;;  {:drugLabel "everolimus", :geneLabel "MTOR", :diseaseLabel "macrocephaly-intellectual disability-neurodevelopmental disorder-small thorax syndrome"}
;;  {:drugLabel "ridaforolimus", :geneLabel "MTOR", :diseaseLabel "macrocephaly-intellectual disability-neurodevelopmental disorder-small thorax syndrome"}
;;  {:drugLabel "dactolisib", :geneLabel "MTOR", :diseaseLabel "macrocephaly-intellectual disability-neurodevelopmental disorder-small thorax syndrome"}
;;  {:drugLabel "temsirolimus", :geneLabel "MTOR", :diseaseLabel "macrocephaly-intellectual disability-neurodevelopmental disorder-small thorax syndrome"}
;;  {:drugLabel "AIDA", :geneLabel "GRM1", :diseaseLabel "autosomal recessive spinocerebellar ataxia 13"}
;;  {:drugLabel "CPCCOEt", :geneLabel "GRM1", :diseaseLabel "autosomal recessive spinocerebellar ataxia 13"}]


;; illustrate the use of :values
(def inchikeys
  #{"QFJCIRLUMZQUOT-HPLJOQBZSA-N"
    "MTCJZZBQNCXKAP-KSYZLYKTSA-N"
    "YNCMLFHHXWETLD-UHFFFAOYSA-N"})

(query
    '[:select ?compoundLabel
      :where [[?compound (wdt :InChIKey) ?inchi]
              :values ?inchi inchikeys]])

;[{:compoundLabel "pyocyanine"} {:compoundLabel "sirolimus"} {:compoundLabel "formycin B"}]

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Recently added lexical queries

;; A recursive query using property paths
;; https://en.wikibooks.org/wiki/SPARQL/Property_paths
(query
 '[:select :distinct ?ancestorLemma ?ancestorLangLabel
   :where [[?lexeme (literal "wikibase:lemma") "Quark"@de] ; lexeme for DE word "Quark"
           [?lexeme (literal "wdt:P5191") + ?ancestor] ; P5191 = derived-from, too new to be in properties list?!
           [?ancestor (literal "wikibase:lemma") ?ancestorLemma ; get each ancestor lemma and language
            _ (literal "dct:language") ?ancestorLang]]
   :limit 20])
;;=>
;; [{:xLangLabel "Polish", :xLemma "twaróg"}
;;  {:xLangLabel "Proto-Slavic", :xLemma "*tvarogъ"}
;;  {:xLangLabel "Proto-Slavic", :xLemma "*tvoriti"}]

;; Note the use of (literal ...) for cases were we do not yet support
;; the namespace/syntax required for the query. This allows for
;; greater access to the SPARQL endpoint's power while progressively
;; improving our DSL.

;; German-language media representation in Wikidata
(mapv
 (fn [zeitung]
   (assoc (first (query
                  (template
                   [:select (count ?ref :as ?mentions)
                    :where [[?statement (literal "prov:wasDerivedFrom") ?ref]
                            [?ref (literal pr:P248) (entity ~zeitung)]]])))
          :zeitung zeitung))
 ["Bild" "Süddeutsche Zeitung" "Frankfurter Allgemeine Zeitung" "Die Zeit" "Die Welt" "Die Tageszeitung"])
;;=>
;; [{:mentions "57", :zeitung "Süddeutsche Zeitung"}
;;  {:mentions "19", :zeitung "Frankfurter Allgemeine Zeitung"}
;;  {:mentions "15", :zeitung "Die Zeit"}
;;  {:mentions "8", :zeitung "Die Welt"}
;;  {:mentions "3", :zeitung "Die Tageszeitung"}
;;  {:mentions "1", :zeitung "Bild"}]

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; multiple language support (work ongoing)

;; lookup an entity using Thai as the default language
(binding [mundaneum.query/*default-language* "th"]
 (entity "ระยอง"))
;; => "Q395325"

;; also works for describe
(binding [mundaneum.query/*default-language* "th"]
  (describe (entity "ระยอง")))
;;=> "หน้าแก้ความกำกวมวิกิมีเดีย"
