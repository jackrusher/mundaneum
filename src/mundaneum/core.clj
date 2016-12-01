(ns mundaneum.core
  (:require [mundaneum.document :as d]
            [mundaneum.query    :refer [query entity prop]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WIKIDATA API (pretty rough to use for anything interesting)

(->> (d/get-entity-document "Q76")  ;; Barack Obama
     (d/find-statement-group "P39") ;; position(s) held
     (d/find-statement "Q11696")    ;; POTUS
     (d/find-claim "P1365")         ;; "replaced"
     d/get-value-id)
;;=> "Q207"

;; oy, what's that then?
(d/get-label (d/get-entity-document "Q207"))
;;=> "George W. Bush"

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; QUERY 

;; what are some works authored by James Joyce?
(query '[:find ?work ?workLabel
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
 '[:find ?biggerLabel ?smallerLabel
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
 '[:find ?pLabel
   :where [[?p (prop :award-received) / (prop :subclass-of) * (entity "Nobel Prize")]
           [?p (prop :award-received) / (prop :subclass-of) * (entity "Academy Awards")]]])
;;=> #{{:pLabel "Bob Dylan"} {:pLabel "George Bernard Shaw"}}

;; notable murders of the ancient world, with date and location
(query
 '[:find ?killedLabel ?killerLabel ?locationLabel ?when
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

;; discoveries/inventions grouped by person using clojure, uncomment
;; the second part of the :where clause to specify only female
;; inventor/discovers (you'll need to increase the limit, as there
;; appear to be many highly productive lady astrophysicists
(->>
 (query
  '[:find ?whoLabel ?thingLabel
    :where [[?thing (prop :discoverer-or-inventor) ?who]
            ;[?who (prop :sex-or-gender) (entity "female")]
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
