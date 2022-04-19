(ns mundaneum.examples
  (:require [mundaneum.query      :refer [describe entity label query *default-language*]]
            [mundaneum.properties :refer [wdt]]))

;; To understand what's happening here, it would be a good idea to
;; read through this document:

;; https://m.wikidata.org/wiki/Wikidata:SPARQL_query_service/queries

;; The first challenge when using Wikidata is finding the right IDs
;; for the properties and entities you must use to phrase your
;; question properly. We have functions to help:

(entity "U2")   ;=> :wd/Q396

;; Now we know the ID for U2... or do we? Which U2 is it, really?

(describe (entity "U2"))   ;=> "Irish rock band"

(entity "U2" (wdt :part-of) (entity "Berlin U-Bahn"))   ;=> :wd/Q99697

(describe (entity "U2" (wdt :part-of) (entity "Berlin U-Bahn")))   ;=> "underground line in Berlin"

;; We also have a function to turn a human-readable keyword into a
;; namespaced keyword containing a prefix/property pair:
(wdt :instance-of)
;;=> :wdt/P31

;; ... which we use in the `query` function to make it easier to write
;; queries from the REPL. For example, we can ask which things contain
;; an administrative territory and get a list of the pairs filtered to
;; the situation where the container is Ireland, and we can specify
;; that we want those labels in the Irish language.

(binding [*default-language* :ga]
  (->> (query `{:select [?areaLabel]
                :where [[~(entity "Republic of Ireland")
                         ~(wdt :contains-administrative-territorial-entity)
                         ?area]]
                :limit 50})
       (map :areaLabel)))
#_("Contae Laoise"
   "Cúige Uladh"
   "Contae Liatroma"
   "Cúige Mumhan"
   "Cúige Laighean"
   "Contae Chorcaí"
   "Cúige Chonnacht"
   "Contae na Gaillimhe"
   "Contae Chill Dara"
   "Contae Bhaile Átha Cliath"
   "Contae Luimnigh"
   "Contae Mhaigh Eo"
   "Contae Shligigh"
   "Contae Dhún na nGall"
   "Contae Ros Comáin"
   "Contae Chill Chainnigh"
   "Contae an Chláir"
   "Contae Cheatharlach"
   "Contae Chill Mhantáin"
   "Contae na hIarmhí"
   "Contae Lú"
   "Contae na Mí"
   "Contae Uíbh Fhailí"
   "Contae Chiarraí"
   "Contae Phort Láirge"
   "Contae Loch Garman"
   "Contae Thiobraid Árann"
   "Contae Mhuineacháin"
   "Contae an Longfoirt"
   "Contae an Chabháin")

;; Discoveries/inventions grouped by person on the clojure side,
(->> (query
      `{:select [?thingLabel ?whomLabel]
        :where [[?thing ~(wdt :discoverer-or-inventor) ?whom]]
        :limit 100})
     (group-by :whomLabel)
     (reduce #(assoc %1 (first %2) (mapv :thingLabel (second %2))) {}))

;; eye color popularity, grouping and counting as part of the query
(query `{:select [?eyeColorLabel [(count ?person) ?count]]
         :where [[?person ~(wdt :eye-color) ?eyeColor]]
         :group-by [?eyeColorLabel]
         :order-by [(desc ?count)]})
 
;; U7 stations in Berlin w/ geo coords
(query `{:select [?stationLabel ?coord]
         :where [[?station ~(wdt :connecting-line) ~(entity "U7" (wdt :part-of) (entity "Berlin U-Bahn"))]
                 [?station ~(wdt :coordinate-location) ?coord]]})

;; born in ancient Rome or territories thereof
(->> (query `{:select [?itemLabel]
              :where [[?item ~(wdt :place-of-birth) ?pob]
                      [?pob (* ~(wdt :located-in-the-administrative-territorial-entity)) ~(entity "Ancient Rome")]]
              :limit 10})
     (map :itemLabel)
     (into #{}))
;;=> #{"Marcus Furius Camillus" "Avianus" "Porcia Catonis" "Faustina the Elder" "Hippolytus" "Sylvester I" "Lucius Caecilius Metellus Denter" "Lucius Junius Brutus" "Gaius Valarius Sabinus" "Publius Petronius Turpilianus"}

;; A query to show what places in Germany have names that end in
;; -ow/-itz (indicating that they were historically Slavic). Notice
;; that the `:where` clause is written differently here, using nexted
;; maps and sets. This is a shorthand form for when you have many
;; constraints on a single entity.
(query `{:select *
         :where [{?ort {(cat ~(wdt :instance-of) (* ~(wdt :subclass-of))) #{~(entity "human settlement")}
                        ~(wdt :country) #{~(entity "Germany")}
                        :rdfs/label #{?name}
                        ~(wdt :coordinate-location) #{?wo}}}
                 [:filter (= (lang ?name) "de")]
                 [:filter (regex ?name "(ow|itz)$")]]
         :limit 5})
;;=>
#_[{:ort :wd/Q6448,
    :wo
    {:datatype "http://www.opengis.net/ont/geosparql#wktLiteral",
     :type "literal",
     :value "Point(13.716666666 50.993055555)"},
    :name "Bannewitz"}
   {:ort :wd/Q8957,
    :wo
    {:datatype "http://www.opengis.net/ont/geosparql#wktLiteral",
     :type "literal",
     :value "Point(13.7425 51.022777777)"},
    :name "Zschertnitz"}
   {:ort :wd/Q160831,
    :wo
    {:datatype "http://www.opengis.net/ont/geosparql#wktLiteral",
     :type "literal",
     :value "Point(14.4097 51.3025)"},
    :name "Oppitz"}
   {:ort :wd/Q160870,
    :wo
    {:datatype "http://www.opengis.net/ont/geosparql#wktLiteral",
     :type "literal",
     :value "Point(14.3189 51.2331)"},
    :name "Pannewitz"}
   {:ort :wd/Q160880,
    :wo
    {:datatype "http://www.opengis.net/ont/geosparql#wktLiteral",
     :type "literal",
     :value "Point(14.570833333 51.161111111)"},
    :name "Pommritz"}]

;; WikiData is multilingual! Here's a query to list species of Swift
;; (the bird) with their English and German (and often Latin) names
(query `{:select [?englishName ?germanName]
         :where [[?bird ~(wdt :parent-taxon) ~(entity "Apodiformes")]
                 [?bird :rdfs/label ?germanName]
                 [?bird :rdfs/label ?englishName]
                 [:filter (= (lang ?germanName) "de")]
                 [:filter (= (lang ?englishName) "en")]]
         :limit 10})
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
  (->> (query `{:select [?isto ?analogyLabel]
                :where [[~a1 ?isto ~a2]
                        [~b1 ?isto ?analogy]]})
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

(apply make-analogy (map entity ["The Beatles" "rock and roll" "Duke Ellington"]))
;;=> ("The Beatles is <genre> to rock and roll as Miles Davis is <genre> to jazz")

;; Airports within 150km of Paris, use "around" service
(->> `{:select-distinct [?placeLabel] 
       :where [[~(entity "Paris") ~(wdt :coordinate-location) ?parisLoc]
               [?place ~(wdt :instance-of) ~(entity "airport")]
               [:service :wikibase/around
                [[?place ~(wdt :coordinate-location) ?location]
                 [:bd/serviceParam :wikibase/center ?parisLoc]
                 [:bd/serviceParam :wikibase/radius "150"]]]]}
     query
     (map :placeLabel)
     (into #{}))

(defn releases-since
  "Returns any creative works published since `year`/`month` by any `entities` known to Wikidata."
  [since-year since-month entities]
  (query `{:select [?workLabel ?creatorLabel ?role ?date]
           :where [[?work ?role ?creator]
                   [?work ~(wdt :publication-date) ?date]
                   [:union ~@(mapv #(vector ['?work '?role %])
                                   (keep entity entities))]
                   [:filter (>= (year ?date) ~since-year)]
                   [:filter (>= (month ?date) ~since-month)]]
           :order-by [(asc ?date)]}))

(->> (releases-since 2019 1 ; year and month
                     ["Kelly Link" "Stromae" "Guillermo del Toro" "Hayao Miyazaki" "Lydia Davis"
                      "Werner Herzog" "Björk" "George Saunders" "Feist" "Andrew Bird" "Sofia Coppola"])
     (map #(select-keys % [:workLabel :creatorLabel]))
     distinct
     (group-by :workLabel))

;; How about something a bit more serious? Here we find drug-gene
;; product interactions and diseases for which these might be
;; candidates.
(->> (query `{:select [?drugLabel ?geneLabel ?diseaseLabel]
              :where [[?drug ~(wdt :physically-interacts-with) ?gene_product]
                      [?gene_product ~(wdt :encoded-by) ?gene]
                      [?gene ~(wdt :genetic-association) ?disease]]
              :limit 100})
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Recently added lexical queries

;; Ancestors to the English word Quark (the food). Notice that we can
;; also just use namespaced keywords for IDs when translating a query
;; from a WikiData example.
(query '{:select-distinct [?ancestorLemma ?ancestorLangLabel]
         :where [{?lexeme {:wikibase/lemma #{{:de "Quark"}}
                           (+ :wdt/P5191) #{?ancestor}}}
                 {?ancestor {:wikibase/lemma #{?ancestorLemma}
                             :dct/language #{?ancestorLang}}}]
         :limit 20})
;;=>
#_[{:ancestorLangLabel "Polish", :ancestorLemma "twaróg"}
   {:ancestorLangLabel "German", :ancestorLemma "Quark"}
   {:ancestorLangLabel "Proto-Slavic", :ancestorLemma "*tvarogъ"}
   {:ancestorLangLabel "Proto-Slavic", :ancestorLemma "*tvoriti"}
   {:ancestorLangLabel "English", :ancestorLemma "quark"}]

;; lemmatize a word using WikiData
(query '{:select-distinct [?l ?word ?lemma ?form]
         :values {?word [{:en "bought"}]}
         :where [{?l {:rdf/type #{:ontolex/LexicalEntry}
                      :dct/language #{:wd/Q1860}
                      :wikibase/lemma #{?lemma}
                      :ontolex/lexicalForm #{?form}}}
                 [?form :ontolex/representation ?word]]
         :limit 20})
;; => [{:word "bought", :form :wd/L3873-F3, :l :wd/L3873, :lemma "buy"}]

;; German-language media representation in Wikidata
(mapv
 (fn [zeitung]
   (assoc (first (query `{:select [[(count ?ref) ?mentions]]
                          :where [[?statement :prov/wasDerivedFrom ?ref]
                                  [?ref :pr/P248 ~(entity zeitung)]]}))
          :zeitung zeitung))
 ["Bild" "Süddeutsche Zeitung" "Frankfurter Allgemeine Zeitung" "Die Zeit" "Die Welt" "Die Tageszeitung"])
;;=>
#_[{:mentions 115, :zeitung "Süddeutsche Zeitung"}
   {:mentions 43, :zeitung "Frankfurter Allgemeine Zeitung"}
   {:mentions 34, :zeitung "Die Welt"}
   {:mentions 25, :zeitung "Die Zeit"}
   {:mentions 23, :zeitung "Die Tageszeitung"}
   {:mentions 10, :zeitung "Bild"}]

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; FEDERATED

;; This query combines data from WikiData and WikiPathways to show
;; interaction types for WP716 (Vitamin A and carotenoid metabolism in
;; Homo sapiens).
;;
;; https://www.wikipathways.org/index.php/Pathway:WP716

(->> `{:prefixes {:dc "<http://purl.org/dc/elements/1.1/>"
                  :wp "<http://vocabularies.wikipathways.org/wp#>"}
       :select-distinct [?interaction_type]
       :where [[:values {?wpid ["WP716"]}]
               [?item :wdt/P2410 ?wpid]
               [?item :wdt/P2888 ?source_pathway]
               [:service "<http://sparql.wikipathways.org/sparql>"
                [[?wp_pathway :dc/identifier ?source_pathway]
                 {?s {:dct/isPartOf #{?wp_pathway ?interaction}}}
                 [?interaction :rdf/type :wp/Interaction]
                 [?interaction :rdf/type ?interaction_type]]]]
       :limit 20}
     query
     (map :interaction_type))
;;=>
#_("http://vocabularies.wikipathways.org/wp#Catalysis"
   "http://vocabularies.wikipathways.org/wp#TranscriptionTranslation"
   "http://vocabularies.wikipathways.org/wp#Stimulation"
   "http://vocabularies.wikipathways.org/wp#DirectedInteraction"
   "http://vocabularies.wikipathways.org/wp#Interaction"
   "http://vocabularies.wikipathways.org/wp#Inhibition"
   "http://vocabularies.wikipathways.org/wp#Conversion")

;; All known metabolites involved in Melatonin metabolism.
(->> `{:prefixes {:dc "<http://purl.org/dc/elements/1.1/>"
                  :wp "<http://vocabularies.wikipathways.org/wp#>"}
       :select [?metaboliteName]
       :where [[:values {?item [~(entity "Melatonin metabolism and effects")]}]
               [?item ~(wdt :WikiPathways-ID) ?wpid]
               [?item ~(wdt :exact-match) ?source_pathway]
               [:service "<http://sparql.wikipathways.org/sparql>"
                [[?wp_pathway :dc/identifier ?source_pathway]
                 {?metabolite {a             #{:wp/Metabolite} 
                               :dct/isPartOf #{?wp_pathway}
                               :rdfs/label    #{?metaboliteName}}}]]]
       :limit 20}
     query
     (map :metaboliteName))
#_("Cyclic AMP"
   "cAMP"
   "CAMP"
   "Serotonin"
   "5HT"
   "5HT [extracellular region]"
   "5HT [clathrin-sculpted monoamine transport vesicle lumen]"
   "LPS"
   "Lipopolysaccharide"
   "Melatonin"
   "MLT"
   "N-Acetylserotonin"
   "Ac5HT"
   "5-Hydroxyindoleacetic acid"
   "HIAA"
   "Bufotenin"
   "5HT-N-CH3"
   "5-Methoxytryptamine"
   "6-Hydroxymelatonin"
   "Noradrenaline")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; multiple language support

;; lookup an entity using Thai as the default language, then get the
;; English label for it.
(let [thai-name "กรุงเทพมหานคร"
      id (binding [*default-language* :th]
           (entity thai-name))]
  (str thai-name " is called " (label id) " in English."))
;;=> "กรุงเทพมหานคร is called Bangkok in English."

