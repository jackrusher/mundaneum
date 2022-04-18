(ns mundaneum.core-test
  (:require [clojure.test         :refer :all]
            [mundaneum.query      :refer :all]
            [mundaneum.properties :refer :all]
            [backtick             :refer [template]]))

(deftest property-and-entity-tests
  (testing "Property lookups"
    (is (= (wdt :instance-of) :wdt/P31))
    (is (= (wdt :part-of) :wdt/P361)))

  (testing "Entity lookup/description"
    (is (= (entity "U2") :wd/Q396))
    (is (= (entity "Berlin U-Bahn") :wd/Q68646))
    (is (= (entity "U2" (wdt :part-of) (entity "Berlin U-Bahn")) :wd/Q99697))
    (is (= (label :wd/Q396) "U2"))
    (is (= (describe (entity "U2")) "Irish rock band"))
    (is (= (describe (entity "U2" (wdt :part-of) (entity "Berlin U-Bahn")))
           "underground line in Berlin"))))

(deftest queries
  (testing "Example queries"
    ;; all stations on the U1 line in Berlin
    (is (= (->> (template
                 {:select [?stationLabel]
                  :where [[?station
                           ~(wdt :connecting-line)
                           ~(entity "U1" (wdt :part-of) (entity "Berlin U-Bahn"))]]})
                query
                (map :stationLabel)
                (into #{}))
           #{"Kurfürstenstraße metro station"
             "Schlesisches Tor"
             "Hallesches Tor"
             "Wittenbergplatz metro station"
             "Kurfürstendamm metro station"
             "Uhlandstraße"
             "Warschauer Straße metro station"
             "Möckernbrücke U-Bahn station"
             "Görlitzer Bahnhof"
             "Kottbusser Tor station"
             "Gleisdreieck"
             "Nollendorfplatz metro station"
             "Prinzenstraße"}))))
