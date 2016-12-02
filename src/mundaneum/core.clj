(ns mundaneum.core
  (:require [mundaneum.document :as d]
            [mundaneum.query :refer [query entity prop statement qualifier]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WIKIDATA API (pretty rough to use for anything interesting)

(->> (d/get-entity-document "Q76")  ;; Barack Obama
     (d/find-statement-group "P39") ;; position(s) held
     (d/find-statement "Q11696")    ;; POTUS
     (d/find-claim "P1365")         ;; "replaces"
     d/get-value-id                 ;; => "Q207"
     d/get-entity-document
     d/get-label)
;;=> "George W. Bush"

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; QUERY

;; what are some works authored by James Joyce?
(query '[:select ?work ?workLabel
         :where [[?work (prop :author) (entity "James Joyce")]]
         :limit 10])
;; #{{:work "Q864141", :workLabel "Eveline"}
;;   {:work "Q861185", :workLabel "A Little Cloud"}
;;   {:work "Q459592", :workLabel "Dubliners"}
;;   {:work "Q682681", :workLabel "Giacomo Joyce"}
;;   {:work "Q764318", :workLabel "Two Gallants"}
;;   {:work "Q429967", :workLabel "Chamber Music"}
;;   {:work "Q465360", :workLabel "A Portrait of the Artist as a Young Man"}
;;   {:work "Q6511", :workLabel "Ulysses"}
;;   {:work "Q866956", :workLabel "An Encounter"}
;;   {:work "Q6507", :workLabel "Finnegans Wake"}} 

;; the parts of various countries, ignoring Canada (sorry, Canada)
(query
 '[:select ?biggerLabel ?smallerLabel
   :where [[?bigger (prop :instance-of) (entity "country")]
           [?bigger (prop :contains-administrative-territorial-entity) ?smaller]
           :filter [?bigger != (entity "Canada")]]
   :limit 10])
;;=>
;; #{{:biggerLabel "Norway", :smallerLabel "Østfold"}
;;   {:biggerLabel "Norway", :smallerLabel "Troms"}
;;   {:biggerLabel "Japan", :smallerLabel "Nara Prefecture"}
;;   {:biggerLabel "Japan", :smallerLabel "Fukuoka Prefecture"}
;;   {:biggerLabel "Japan", :smallerLabel "Aomori Prefecture"}
;;   {:biggerLabel "Japan", :smallerLabel "Wakayama Prefecture"}
;;   {:biggerLabel "Ireland", :smallerLabel "County Wicklow"}
;;   {:biggerLabel "Japan", :smallerLabel "Fukui Prefecture"}
;;   {:biggerLabel "Japan", :smallerLabel "Toyama Prefecture"}
;;   {:biggerLabel "Hungary", :smallerLabel "Hajdú-Bihar County"}
;;   {:biggerLabel "Hungary", :smallerLabel "Bács-Kiskun County"}
;;   ...
;;   }

;; the only people to win both an academy award and a nobel prize
(query
 '[:select ?pLabel
   :where [[?p (prop :award-received) / (prop :subclass-of) * (entity "Nobel Prize")]
           [?p (prop :award-received) / (prop :subclass-of) * (entity "Academy Awards")]]])
;;=> #{{:pLabel "Bob Dylan"} {:pLabel "George Bernard Shaw"}}

;; notable murders of the ancient world, with date and location
(query
 '[:select ?killedLabel ?killerLabel ?locationLabel ?when
   :where [[?killed (prop :killed-by) ?killer] 
           [?killed (prop :date-of-death) ?when] 
           [?killed (prop :place-of-death) ?location]]
   :order-by (asc ?when)
   :limit 5])
;;=>
;; #{{:when
;;    #object[org.joda.time.DateTime 0x61b1428a "-0474-01-01T00:00:00.000Z"],
;;    :killedLabel "Xerxes",
;;    :killerLabel "Artabanus of Persia",
;;    :locationLabel "Persia"}
;;   {:when
;;    #object[org.joda.time.DateTime 0x2c4daccc "-0490-01-01T00:00:00.000Z"],
;;    :killedLabel "Eurybates",
;;    :killerLabel "Sophanes",
;;    :locationLabel "Aegina"}
;;   {:when
;;    #object[org.joda.time.DateTime 0xfa38908 "-0402-01-01T00:00:00.000Z"],
;;    :killedLabel "Polemarchus",
;;    :killerLabel "Thirty Tyrants",
;;    :locationLabel "Athens"}
;;   {:when
;;    #object[org.joda.time.DateTime 0x3a3a111e "-0335-01-01T00:00:00.000Z"],
;;    :killedLabel "Philip II of Macedon",
;;    :killerLabel "Pausanias of Orestis",
;;    :locationLabel "Vergina"}
;;   {:when
;;    #object[org.joda.time.DateTime 0x240cb2db "-0479-01-01T00:00:00.000Z"],
;;    :killedLabel "Ephialtes of Trachis",
;;    :killerLabel "Athénade",
;;    :locationLabel "Thessaly"}}

;; discoveries/inventions grouped by person on the clojure side,
;; uncomment the second part of the :where clause to specify only
;; female inventor/discovers (you'll need to increase the limit, as
;; there appear to be many highly productive lady astronomers
(->> (query
      '[:select ?whoLabel ?thingLabel
        :where [[?thing (prop :discoverer-or-inventor) ?who]
                ;;[?who (prop :sex-or-gender) (entity "female")]
                ]
        :limit 100])
 (group-by :whoLabel)
 (reduce #(assoc %1 (first %2) (mapv :thingLabel (second %2))) {}))
;;=>
;; {"The Guardian" ["Panama Papers"],
;;  "Enrico Fermi"
;;  ["Fermi resonance"
;;   "Metropolis–Hastings algorithm"
;;   "Monte Carlo method"
;;   "Fermi–Walker transport"],
;;  "Channel Tunnel" ["Ludovic Breton"],
;;  "Napoleon" ["Napoleon's theorem"],
;;  "Abd al-Rahman al-Sufi" ["Brocchi's Cluster"]}
;; ...

;; eye color popularity, grouping and counting as part of the query
(query
 '[:select ?eyeColorLabel (count ?person :as ?count)
   :where [[?person (prop :eye-color) ?eyeColor] ]
   :group-by ?eyeColorLabel])
;;=>
;; #{{:eyeColorLabel "yellow", :count "29"} {:eyeColorLabel "red", :count "12"}
;;   {:eyeColorLabel "black", :count "145"}
;;   {:eyeColorLabel "purple", :count "1"}
;;   {:eyeColorLabel "blue-green", :count "19"}
;;   {:eyeColorLabel "brown", :count "300"}
;;   {:eyeColorLabel "blue", :count "342"}
;;   {:eyeColorLabel "hazel", :count "75"}
;;   {:eyeColorLabel "green", :count "216"}
;;   {:eyeColorLabel "dark brown", :count "94"}
;;   {:eyeColorLabel "amber", :count "13"} {:eyeColorLabel "grey", :count "11"}}

;; airports within 20km of Paris, use "around" service
(query
 '[:select ?place ?placeLabel ?location
   :where [[(entity "Paris") (prop :coordinate-location) ?parisLoc]
           [?place (prop :instance-of) / (prop :subclass-of) * (entity "airport")]
           :service wikibase:around [[?place (prop :coordinate-location) ?location]
                                     [bd:serviceParam wikibase:center ?parisLoc]
                                     [bd:serviceParam wikibase:radius "20"]]]])
;; #{{:place "Q2875445",
;;    :location "Point(2.60611 48.8967)",
;;    :placeLabel "Chelles Le Pin Airport"}
;;   {:place "Q738719",
;;    :location "Point(2.441388888 48.969444444)",
;;    :placeLabel "Paris–Le Bourget Airport"}
;;   {:place "Q7103340",
;;    :location "Point(2.362778 48.723333)",
;;    :placeLabel "Orly Air Base"}
;;   {:place "Q223416",
;;    :location "Point(2.362778 48.723333)",
;;    :placeLabel "Orly Airport"}
;;   {:place "Q1894366",
;;    :location "Point(2.191667 48.774167)",
;;    :placeLabel "Villacoublay Air Base"}
;;   {:place "Q1894366",
;;    :location "Point(2.19972222 48.77305556)",
;;    :placeLabel "Villacoublay Air Base"}
;;   {:place "Q22975841",
;;    :location "Point(2.4405591 48.9511837)",
;;    :placeLabel "Q22975841"}
;;   {:place "Q2875445",
;;    :location "Point(2.6075 48.897778)",
;;    :placeLabel "Chelles Le Pin Airport"}}

;; U1 stations in Berlin w/ geo coords
(query
 '[:select ?stationLabel ?coord
   :where [[?station
            (prop :connecting-line)
            (entity "U1" :part-of "Berlin U-Bahn") \;
            (prop :coordinate-location) ?coord]]])
;;=>
;; #{{:coord "Point(13.382777777 52.499166666)",
;;    :stationLabel "Möckernbrücke"}
;;   {:coord "Point(13.343055555 52.501944444)",
;;    :stationLabel "Wittenbergplatz"}
;;   {:coord "Point(13.332777777 52.504166666)",
;;    :stationLabel "Kurfürstendamm"}
;;   {:coord "Point(13.361944444 52.5)",
;;    :stationLabel "Kurfürstenstraße"}
;;   {:coord "Point(13.325555555 52.5025)", :stationLabel "Uhlandstraße"}
;;   {:coord "Point(13.441666666 52.500833333)",
;;    :stationLabel "Schlesisches Tor"}
;;   {:coord "Point(13.353888888 52.499166666)",
;;    :stationLabel "Nollendorfplatz"}
;;   {:coord "Point(13.375277777 52.498333333)",
;;    :stationLabel "Gleisdreieck"}
;;   {:coord "Point(13.428 52.4992)", :stationLabel "Görlitzer Bahnhof"}
;;   {:coord "Point(13.406111111 52.498333333)",
;;    :stationLabel "Prinzenstraße"}
;;   {:coord "Point(13.391111111 52.497777777)",
;;    :stationLabel "Hallesches Tor"}
;;   {:coord "Point(13.418055555 52.499166666)",
;;    :stationLabel "Kottbusser Tor"}
;;   {:coord "Point(13.449047222 52.505083333)",
;;    :stationLabel "Warschauer Straße subway station"}}


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO better blank node handling, graceful way to distinguish
;; between direct property wdt:Pxxx and regular property p:Pxxx nodes.

;; (mundaneum.query/stringify-query ; query
;;  '[:select ?predLabel
;;    :where [[(entity "Barack Obama")
;;             (prop :position-held)
;;             \[
;;             (statement :position-held)
;;             (entity "President of the United States of America")
;;             \;
;;             (qualifier :replaces)
;;             ?pred \]]]])

;; ... is correct but for wdt:P39 in place of p:P39 in the output:
;;
;; SELECT ?whomLabel
;; WHERE {
;;   wd:Q76 p:P39 [ps:P39 wd:Q11696; pq:P1365 ?whom].
;;   SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
;; }
;; LIMIT 10
