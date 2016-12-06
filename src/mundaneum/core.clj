(ns mundaneum.core
  (:require [mundaneum.document :as d]
            [mundaneum.query    :refer [query entity property]]
            [backtick           :refer [template]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WIKIDATA API (pretty rough to use for anything interesting)

(->> (d/entity-document "Q76")  ;; Barack Obama
     (d/statement-group-by-id "P39") ;; position(s) held
     (d/statement-by-id "Q11696")    ;; POTUS
     (d/claim-by-id "P1365")         ;; "replaces"
     d/value-id                 ;; => "Q207"
     d/id->label)
;;=> "George W. Bush"

;; easier with helper functions
(->> (d/entity-document (entity "Barack Obama"))
     (d/statement-group-by-id (property :position-held))
     (d/statement-by-id (entity "President of the United States of America"))
     (d/claim-by-id (property :replaces))
     d/value-id    ;=> "Q207"
     d/id->label)
;;=> "George W. Bush"

;; It's also nice to be able to test whether the entity that you've
;; requested is the one you expected, like so:

(d/describe (entity "Barack Obama"))
;;"44th President of the United States of America"

;; ... and if it isn't, you can refine the request by adding extra
;; criteria to your call to entity:

(d/describe (entity "U1"))
;;=> "Wikipedia disambiguation page"

(d/describe (entity "U1" :part-of (entity "Berlin U-Bahn")))
;;=> "rapid transit line in Berlin, Germany"

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; QUERY

;; the same question about presidential precedence posed in SPARQL,
;; which is actually a bit complicated because it involves: querying
;; against property statements and property qualifiers, plus the use
;; of the _ (a stand-in for SPARQL's semicolon, which is used to say
;; continue this expression using the same entity):
(query
 '[:select ?prevLabel
   :where [[(entity "Barack Obama") (p :position-held) ?pos]
           [?pos (ps :position-held) (entity "President of the United States of America")
            _ (pq :replaces) ?prev]]])
;;=>#{{:prevLabel "George W. Bush"}}

;; which can be trivially expanded to list all US presidents and their
;; predecessors
(query
 '[:select ?prezLabel ?prevLabel
   :where [[?prez (p :position-held) ?pos]
           [?pos (ps :position-held) (entity "President of the United States of America")
            _ (pq :replaces) ?prev]]])
;;=>#{{:prezLabel "John Tyler", :prevLabel "William Henry Harrison"}
;;    {:prezLabel "Gerald Ford", :prevLabel "Richard Nixon"}
;;    {:prezLabel "John Adams", :prevLabel "George Washington"}
;; ...  

;; the parts of various countries, ignoring Canada (sorry, Canada)
(query
 '[:select ?biggerLabel ?smallerLabel
   :where [[?bigger (wdt :instance-of) (entity "country")]
           [?bigger (wdt :contains-administrative-territorial-entity) ?smaller]
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
;;
;; (note the _, which translates to SPARQL's ; which means "use the
;; same subject as before")
(query
 '[:select :distinct ?pLabel
   :where [[?p (wdt :award-received) / (wdt :subclass-of) * (entity "Nobel Prize")
            _ (wdt :award-received) / (wdt :subclass-of) * (entity "Academy Awards")]]])
;;=> #{{:pLabel "Bob Dylan"} {:pLabel "George Bernard Shaw"}}

;; notable murders of the ancient world, with date and location
(query
 '[:select ?killedLabel ?killerLabel ?locationLabel ?when
   :where [[?killed (wdt :killed-by) ?killer] 
           [?killed (wdt :date-of-death) ?when] 
           [?killed (wdt :place-of-death) ?location]]
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
;; female inventor/discovers
(->> (query
      '[:select ?thingLabel ?whomLabel
        :where [[?thing (wdt :discoverer-or-inventor) ?whom
;;                 _ (wdt :sex-or-gender) (entity "female")
                 ]]
        :limit 100])
 (group-by :whomLabel)
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
   :where [[?person (wdt :eye-color) ?eyeColor] ]
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
   :where [[(entity "Paris") (wdt :coordinate-location) ?parisLoc]
           [?place (wdt :instance-of) / (wdt :subclass-of) * (entity "airport")]
           :service wikibase:around [[?place (wdt :coordinate-location) ?location]
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
(query (template
        [:select ?stationLabel ?coord
         :where [[?station (wdt :connecting-line) (entity "U1" :part-of ~(entity "Berlin U-Bahn"))
                  _ (wdt :coordinate-location) ?coord]]]))
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

;; born in Scotland or territories thereof
(query
 '[:select ?itemLabel ?pobLabel
   :where [:union [[?item (wdt :place-of-birth) (entity "Scotland")]
                   [[?item (wdt :place-of-birth) ?pob]
                    [?pob (wdt :located-in-the-administrative-territorial-entity) * (entity "Scotland")]]]]
   :limit 10])
;; #{{:item "Q110974", :itemLabel "James Black"}
;;   {:item "Q45864", :itemLabel "John McAfee"}
;;   {:item "Q8755", :itemLabel "Colin Maclaurin"}
;;   {:item "Q90317", :itemLabel "Iain Dilthey"}
;;   {:item "Q122479", :itemLabel "Malcolm IV of Scotland"}
;;   {:item "Q68508", :itemLabel "Malcolm III of Scotland"}
;;   {:item "Q81960", :itemLabel "Robert Burns"}
;;   {:item "Q26326", :itemLabel "Duncan I of Scotland"}
;;   {:item "Q132399", :itemLabel "James Inglis"}
;;   {:item "Q172832", :itemLabel "David Coulthard"}}

(defn make-analogy
  "Return known analogies for the form `a1` is to `a2` as `b1` is to ???"
  [a1 a2 b1]
  (->> (query
        (template [:select ?isto ?analogyLabel
                   :where [[~(symbol (str "wd:" a1)) ?isto ~(symbol (str "wd:" a2))]
                           [~(symbol (str "wd:" b1)) ?isto ?analogy]
                           ;; tightens analogies by requiring that a1/b2 be of the same kind,
                           ;; but loses some interesting loose analogies:
                           ;; [~(symbol (str "wd:" a2)) (wdt :instance-of) ?kind]
                           ;; [?analogy (wdt :instance-of) ?kind]
                           ]]))
       (map #(let [arc (d/id->label (:isto %))]
               (str (d/id->label a1)
                    " <" arc "> "
                    (d/id->label a2)
                    " as "
                    (d/id->label b1)
                    " <" arc "> " (:analogyLabel %))))
       distinct))

(apply make-analogy (map entity ["The Beatles" "rock and roll" "Miles Davis"]))
("The Beatles <genre> rock and roll as Miles Davis <genre> jazz.")

(apply make-analogy (map entity ["Lambic" "beer" "red wine"]))
("Lambic <subclass of> beer as red wine <subclass of> wine")

(apply make-analogy (map entity ["Berlin" "Germany" "Paris"]))
("Berlin <country> Germany as Paris <country> France"
 "Berlin <located in the administrative territorial entity> Germany as Paris <located in the administrative territorial entity> Île-de-France"
 "Berlin <capital of> Germany as Paris <capital of> France")

(make-analogy (entity "Daft Punk")
              (entity "Paris")
              ;; clarify the jape we mean
              (entity "Jape" :instance-of (entity "band")))
("Daft Punk <location of formation> Paris as Jape <location of formation> Dublin")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO OPTIONAL
;; TODO BIND
;; TODO MINUS

;; TODO ASK

;; TODO blank nodes, for things like:
;;
;; ?film movie:actor [ a movie:actor ;
;;                     movie:actor_name ?actorName ].
