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
    (is (= (describe (entity "U2")) "Irish alternative rock band"))
    (is (= (entity "Berlin U-Bahn") "Q68646"))
    (is (= (entity "U2" :part-of (entity "Berlin U-Bahn")) "Q99697"))
    (is (= (describe (entity "U2" :part-of (entity "Berlin U-Bahn")))
           "underground line in Berlin"))))

(deftest queries
  (testing "Example queries"
    ;; who has won a nobel prize and an academy award?
    (let [nobel (entity "Nobel Prize")
          oscar (entity "Academy Awards")]
      (is (= (query
              (template
               [:select :distinct ?pLabel
                :where [[?p (wdt :award-received) / (wdt :instance-of) * (wd ~nobel)
                         _  (wdt :award-received) / (wdt :instance-of) * (wd ~oscar)]]]))
             [{:pLabel "Bob Dylan"} {:pLabel "George Bernard Shaw"}])))

    ;; all stations on the U1 line in Berlin, with lat/long
    (let [u1 (entity "U1" :part-of (entity "Berlin U-Bahn"))]
      (is (= (->> (query
                   (template
                    [:select ?stationLabel ?coord
                     :where [[?station (wdt :connecting-line) (wd ~u1)
                              _ (wdt :coordinate-location) ?coord]]]))
                  (map :stationLabel)
                  (into #{}))
             #{"Kurfürstenstraße metro station" "Möckernbrücke"
               "Berlin Warschauer Straße station"
               "Warschauer Straße subway station" "Schlesisches Tor"
               "Hallesches Tor" "Kurfürstendamm metro station" "Uhlandstraße"
               "Wittenbergplatz" "Görlitzer Bahnhof" "Gleisdreieck"
               "Nollendorfplatz metro station" "Prinzenstraße" "Kottbusser Tor"})))

    ;; Obama's predecessor
    (let [potus (entity "President of the United States of America")
          obama (entity "Barack Obama")]
      (is (= (query
              (template
               [:select ?prevLabel
                :where [[(wd ~obama) (p :position-held) ?pos]
                        [?pos (ps :position-held) (wd ~potus)
                         _ (pq :replaces) ?prev]]]))
             [{:prevLabel "George W. Bush"}])))))
