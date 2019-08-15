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

    ;; administrative districts of Ireland
    (is (= (->> (query
                 '[:select ?biggerLabel ?smallerLabel
                   :where [[?bigger (wdt :contains-administrative-territorial-entity) ?smaller]
                           :filter [?bigger = (entity "Ireland")]]])
                (map :smallerLabel)
                (into #{}))
           #{"County Cork" "County Laois" "Leinster" "Ulster" "County Waterford" "County Meath"
             "County Longford" "County Tipperary" "County Donegal" "County Louth" "Munster"
             "County Clare" "Connacht" "County Cavan" "County Limerick" "County Leitrim" "County Offaly"
             "County Westmeath" "County Wicklow" "County Kerry" "County Wexford" "County Kilkenny"
             "County Carlow" "County Dublin" "County Roscommon" "County Galway" "County Monaghan"
             "County Sligo" "County Mayo" "County Kildare"}))))
