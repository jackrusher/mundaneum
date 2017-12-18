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
      (is (= (query
              (template
               [:select ?stationLabel ?coord
                :where [[?station (wdt :connecting-line) (wd ~u1)
                         _ (wdt :coordinate-location) ?coord]]]))
             [{:coord "Point(13.325555555 52.5025)",      :stationLabel "Uhlandstraße"}
              {:coord "Point(13.353888888 52.499166666)", :stationLabel "Nollendorfplatz"}
              {:coord "Point(13.428 52.4992)",            :stationLabel "Görlitzer Bahnhof"}
              {:coord "Point(13.441666666 52.500833333)", :stationLabel "Schlesisches Tor"}
              {:coord "Point(13.391111111 52.497777777)", :stationLabel "Hallesches Tor"}
              {:coord "Point(13.332777777 52.504166666)", :stationLabel "Kurfürstendamm"}
              {:coord "Point(13.375277777 52.498333333)", :stationLabel "Gleisdreieck"}
              {:coord "Point(13.406111111 52.498333333)", :stationLabel "Prinzenstraße"}
              {:coord "Point(13.361944444 52.5)",         :stationLabel "Kurfürstenstraße"}
              {:coord "Point(13.343055555 52.501944444)", :stationLabel "Wittenbergplatz"}
              {:coord "Point(13.382777777 52.499166666)", :stationLabel "Möckernbrücke"}
              {:coord "Point(13.418055555 52.499166666)", :stationLabel "Kottbusser Tor"}
              {:coord "Point(13.449047222 52.505083333)", :stationLabel "Warschauer Straße subway station"}])))

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
