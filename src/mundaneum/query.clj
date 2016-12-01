(ns mundaneum.query
  (:require [clj-time.core        :as ct]
            [clj-time.format      :as cf]
            [backtick             :refer [template]]
            [mundaneum.document   :refer [entity-document-by-title
                                          get-document-id]]
            [mundaneum.properties :refer [properties]]))

;; PREFIX wd: <http://www.wikidata.org/entity/>
;; PREFIX wds: <http://www.wikidata.org/entity/statement/>
;; PREFIX wdt: <http://www.wikidata.org/prop/direct/>
;; PREFIX wdv: <http://www.wikidata.org/value/>
;; PREFIX wikibase: <http://wikiba.se/ontology#>
;; PREFIX p: <http://www.wikidata.org/prop/>
;; PREFIX ps: <http://www.wikidata.org/prop/statement/>
;; PREFIX pq: <http://www.wikidata.org/prop/qualifier/>
;; PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
;; PREFIX bd: <http://www.bigdata.com/rdf#>

(def wikidata
  (.getConnection
   (doto (org.eclipse.rdf4j.repository.sparql.SPARQLRepository.
          "https://query.wikidata.org/sparql")
     (.initialize))))

(defn clojurize-value [v]
  (let [vc (class v)]
    (cond (= vc org.eclipse.rdf4j.model.impl.SimpleBNode) nil
          (= vc org.eclipse.rdf4j.model.impl.SimpleIRI)  (.getLocalName v)
          (= vc org.eclipse.rdf4j.model.impl.SimpleLiteral)
          (if (= (.toString (.getDatatype v))
                 "http://www.w3.org/2001/XMLSchema#dateTime")
            (cf/parse (.getLabel v))
            (.getLabel v))
          :else [(class v) (.toString v)])))

(defn clojurize-results [results]
  (mapv (fn [bindings]
          (reduce #(assoc %1
                          (keyword %2)
                          (clojurize-value (.getValue bindings %2)))
                  {}
                  (.getBindingNames bindings)))
        results))

(defn do-query [conn sparql-string]
  (->> (.prepareTupleQuery conn
                           org.eclipse.rdf4j.query.QueryLanguage/SPARQL
                           sparql-string)
       (.evaluate)
       (org.eclipse.rdf4j.query.QueryResults/asList)
       (clojurize-results)
       (into #{})))

(declare entity)
(declare prop)

(defn stringify-query
  "Naive conversion of datastructure `q` to a SPARQL query string... fragile af."
  [q]
  (loop [q q out ""]
    (if-let [token (first q)]
      (recur (case token
               :where   (drop 2 q)
               :filter  (drop 2 q)
               :service (drop 3 q)
               (rest q))
             (str out
                  (cond
                    (= :find token) "SELECT "
                    (= :filter token) (str " FILTER ("
                                           (stringify-query (first (rest q)))
                                           ")\n")
                    (= :where token) (str "\nWHERE {\n"
                                          (stringify-query (first (rest q)))
                                          ;; always bring in the label service
                                          " SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" . }\n"
                                          "}")
                    (= :service token) (str "\nSERVICE "
                                            (first (rest q))
                                            " {\n"
                                            (stringify-query (second (rest q)))
                                            "}")
                    (= :order-by token) "\nORDER BY "
                    (= :group-by token) "\nGROUP BY "
                    (= :limit token) "\nLIMIT "
                    (string? token) (str "\"" token "\"")
                    (vector? token) (str (stringify-query token) " .\n")
                    (list? token) (case (first token)
                                    ;; XXX super gross!
                                    clojure.core/deref (str "@" (second token))
                                    prop   (prop (second token))
                                    entity (apply entity (rest token))
                                    desc   (str "DESC(" (second token) ")")
                                    asc    (str "ASC(" (second token) ")")
                                    count  (str "(COUNT("
                                                (second token)
                                                ") AS "
                                                (last token)
                                                ")")
                                    "UNKNOWN OPERATOR ERROR")
                    :else (str " " token " "))))
      out)))
;; TODO add parametric language choices!
;; bd:serviceParam wikibase:language \"en,fr,de,he,el,fi,no,ja\" .

(defn query [q]
  (do-query wikidata (stringify-query q)))

(defn prop
  "Helper function to look up one of the pre-spidered properties by keyword `p`."
  [p]
  (str " wdt:" (first (get mundaneum.properties/properties p))))

(def entity
  "This is a helper function that tries to guess the right WikiData entity ID from a string resembling that entity's `label`. This is harder than it sounds, and this will sometimes return the wrong version of 'human' or 'person' to do you any good in a query. There is endless room for improvement in the heuristic here employed."
  (memoize   
   (fn [label & criteria]
     (->> (query (template
                  [:find ?item
                   :where [[?item rdfs:label ~label@en]
                           ~@(mapv (fn [[p e]]
                                     (template
                                      [?item
                                       ~(symbol (prop p))
                                       ~(symbol (entity e))]))
                                   (partition 2 criteria))]
                   :limit 10]))
          (map :item)
          (filter #(re-find #"^Q[0-9]*$" %))
          first
          (str " wd:")))))

     ;; ;; exact title match? go with it.
     ;; (if-let [doc (entity-document-by-title label)] 
     ;;   (str " wd:" (get-document-id doc))
     ;;   ;; no match? find things with that label, filter to entities, take the first 😹
     ;;   )
