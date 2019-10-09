(ns mundaneum.core-test
  (:require [clojure.test    :refer :all]
            [mundaneum.query :refer :all]
            [backtick        :refer [template]]))

(deftest property-and-entity-tests
  (testing "Property lookups"
    (is (= (property :instance-of) "P31"))
    (is (= (property :part-of) "P361")))

  (testing "Entity lookup/description"
    (is (= (entity "U2") "Q396"))
    (is (= (describe (entity "U2")) "Irish rock band"))
    (is (= (entity "Berlin U-Bahn") "Q68646"))
    (is (= (entity "U2" :part-of (entity "Berlin U-Bahn")) "Q99697"))
    (is (= (describe (entity "U2" :part-of (entity "Berlin U-Bahn")))
           "underground line in Berlin"))))

(deftest queries
  (testing "Example queries"
    ;; people born in an administrative territory of ancient Rome
    (is (= (->> (query
                 '[:select ?itemLabel
                   :where [:union [[[?item (wdt :place-of-birth) ?pob]
                                    [?pob (wdt :located-in-the-administrative-territorial-entity) * (entity "ancient Rome")]]]]
                   :limit 10])
                (map :itemLabel)
                (into #{}))
           #{"Marcus Furius Camillus" "Avianus" "Porcia Catonis" "Faustina the Elder" "Hippolytus" "Sylvester I" "Lucius Caecilius Metellus Denter" "Lucius Junius Brutus" "Gaius Valarius Sabinus" "Publius Petronius Turpilianus"}))

    ;; all stations on the U1 line in Berlin, with lat/long
    (let [u1 (entity "U1" :part-of (entity "Berlin U-Bahn"))]
      (is (= (->> (query
                   (template
                    [:select ?stationLabel ?coord
                     :where [[?station (wdt :connecting-line) (wd ~u1)
                              _ (wdt :coordinate-location) ?coord]]]))
                  (map :stationLabel)
                  (into #{}))
             #{"Kurfürstenstraße metro station" "Möckernbrücke" "Uhlandstraße metro station"
               "Schlesisches Tor" "Hallesches Tor" "Wittenbergplatz metro station"
               "Kurfürstendamm metro station" "Warschauer Straße metro station" "Görlitzer Bahnhof"
               "Kottbusser Tor station" "Gleisdreieck" "Nollendorfplatz metro station" "Prinzenstraße"})))

    ;; airports within 100km of Paris
    (is (= (->>
            (query
             '[:select :distinct ?placeLabel 
               :where [[(entity "Paris") (wdt :coordinate-location) ?parisLoc]
                       [?place (wdt :instance-of) (entity "airport")]
                       :service wikibase:around [[?place (wdt :coordinate-location) ?location]
                                                 [bd:serviceParam wikibase:center ?parisLoc]
                                                 [bd:serviceParam wikibase:radius "100"]]]])
            (map :placeLabel)
            (into #{}))
           #{"Paris-Charles de Gaulle Airport" "Beauvais-Tillé Airport" "Pontoise – Cormeilles Aerodrome" "Paris–Le Bourget Airport" "Melun Villaroche Aerodrome"}))

    ;; Lexicographic query 
    (is (= (query
            '[:select :distinct ?ancestorLemma ?ancestorLangLabel
              :where [[?lexeme (literal "wikibase:lemma") "Quark"@de] ; lexeme for DE word "Quark"
                      [?lexeme (literal "wdt:P5191") + ?ancestor] ; P5191 = derived-from
                      [?ancestor (literal "wikibase:lemma") ?ancestorLemma ; ancestor lemma and language
                       _ (literal "dct:language") ?ancestorLang]]
              :limit 20])
           [{:ancestorLangLabel "Polish", :ancestorLemma "twaróg"}
            {:ancestorLangLabel "Proto-Slavic", :ancestorLemma "*tvarogъ"}
            {:ancestorLangLabel "Proto-Slavic", :ancestorLemma "*tvoriti"}]))))
