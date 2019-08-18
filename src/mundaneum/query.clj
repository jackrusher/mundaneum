(ns mundaneum.query
  (:require [clj-time.core        :as ct]
            [clj-time.format      :as cf]
            [backtick             :refer [template]]
            [mundaneum.properties :refer [properties]]))

(def ^:dynamic *default-language* "en")

;; Need to make it easy to specify these:
;;
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

(defprotocol Clojurizable
  (clojurize-value [this] (vector (class v) (.toString v))))

(extend-protocol Clojurizable
  org.eclipse.rdf4j.model.impl.SimpleBNode
  (clojurize-value [this] nil)
  org.eclipse.rdf4j.model.impl.SimpleIRI
  (clojurize-value [this] (.getLocalName this))
  org.eclipse.rdf4j.model.impl.SimpleLiteral
  (clojurize-value [this]
    (if (= (.toString (.getDatatype this))
           "http://www.w3.org/2001/XMLSchema#dateTime")
      (cf/parse (.getLabel this))
      (.getLabel this)))
  org.wikidata.wdtk.datamodel.json.jackson.datavalues.JacksonValueString
  (clojurize-value [this] (.getValue this))
  org.wikidata.wdtk.datamodel.json.jackson.datavalues.JacksonValueItemId
  (clojurize-value [this] (.getId this))
  org.wikidata.wdtk.datamodel.json.jackson.datavalues.JacksonValueTime
  (clojurize-value [this] (cf/parse (.getTime (.getValue this))))
  org.wikidata.wdtk.datamodel.json.jackson.datavalues.JacksonValueQuantity
  (clojurize-value [this] (.toString this)))

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
       (clojurize-results)))

(defn property
  "Helper function to look up one of the pre-spidered properties by keyword `p`."
  [p]
  (get mundaneum.properties/properties p))

(declare entity)

;; TODO should be able to parameterize the automatic label language(s)
(defn stringify-query
  "Naive conversion of datastructure `q` to a SPARQL query string... fragile af."
  [q]
  (loop [q q out ""]
    (if-let [token (first q)]
      (recur (case token
               :where    (drop 2 q)
               :optional (drop 2 q)
               :union    (drop 2 q)
               :minus    (drop 2 q)
               :filter   (drop 2 q)
               :service  (drop 3 q)
               (rest q))
             (str out
                  (cond
                    (= :select token) "SELECT "
                    (= :distinct token) "DISTINCT "
                    (= :filter token) (str " FILTER ("
                                           (stringify-query (second q))
                                           ")\n")
                    (= :where token) (str "\nWHERE {\n"
                                          (stringify-query (second q))
                                          ;; ;; always bring in the label service XXX breaks the new lexical queries!
                                          " SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE],en\" . }\n"
                                          "}")
                    (= :optional token) (str " OPTIONAL {\n"
                                          (stringify-query (second q))
                                          "}\n")
                    (= :union token) (str " { "
                                          (->> (map stringify-query (second q))
                                               (interpose " } UNION { ")
                                               (apply str))
                                          "}\n")
                    (= :minus token) (str " MINUS { "
                                          (stringify-query (second q))
                                          "}\n")
                    (= :service token) (str "\nSERVICE "
                                            (second q)
                                            " {\n"
                                            (stringify-query (nth q 2))
                                            "}")
                    (= :order-by token) "\nORDER BY "
                    (= :group-by token) "\nGROUP BY "
                    (= :limit token) "\nLIMIT "
                    (= '_ token) " ; "
                    (string? token) (str "\"" token "\"")
                    (vector? token) (str (stringify-query token) " .\n")
                    (list? token) (case (first token)
                                    ;; an override for unimplemented features, &c
                                    literal (str " " (second token))
                                    ;; XXX super gross! move to something like (en "str") form
                                    clojure.core/deref (str "@" (second token))
                                    ;; TODO one of these for each namespace
                                    entity  (if-let [e (eval token)]
                                              (str " wd:" e)
                                              (throw (Exception. (str "could not evaluate entity expression " (pr-str token)))))
                                    p       (str " p:"   (property (second token)))
                                    ps      (str " ps:"  (property (second token)))
                                    pq      (str " pq:"  (property (second token)))
                                    wd      (str " wd:"  (second token))
                                    wdt     (str " wdt:" (property (second token)))
                                    inverse (str "^" (second token))
                                    desc    (str "DESC(" (second token) ")")
                                    asc     (str "ASC("  (second token) ")")
                                    year    (str "YEAR("  (second token) ")")
                                    month   (str "YEAR("  (second token) ")")
                                    lang    (str "LANG("  (second token) ")")
                                    ;;                                    >       (str (second token) " > " (nth token 2))
                                    now     " NOW()"
                                    regex   (str "regex (" (second token) ", \"" (last token) "\")")
                                    count   (str "(COUNT("
                                                 (second token)
                                                 ") AS "
                                                 (last token)
                                                 ")")
                                    sum     (str "(SUM("
                                                 (second token)
                                                 ") AS "
                                                 (last token)
                                                 ")")
                                    (throw (Exception. (str "unknown operator in SPARQL DSL: " (pr-str (first token))))))
                    :else (str " " token " "))))
      out)))

(defn query
  "Perform the query specified in `q` against the Wikidata SPARQL endpoint."
  [q]
  (do-query wikidata (stringify-query q)))

(def entity
  "Returns a WikiData entity whose entity's label resembles `label`. One can specity `criteria` in the form of :property/entity pairs to help select the right entity."
  (memoize   
   (fn [label & criteria]
     (->> (query
           (template [:select ?item (count ?p :as ?count)
                      :where [[?item rdfs:label ~label@en
                               _ ?p ?whatever]
                              ;; stitch in criteria, if supplied
                              ~@(mapv (fn [[p e]]
                                        (template
                                         [?item
                                          ~(symbol (str "wdt:" (property p)))
                                          ~(symbol (str "wd:" e))]))
                                      (partition 2 criteria))
                              ;; no :instance-of / :subclass-of wikidata properties
                              :minus [?item wdt:P31 / wdt:P279 + wd:Q18616576]]
                      ;; choose the entity with the most properties
                      :group-by ?item
                      :order-by (desc ?count)
                      :limit 1]))
          first
          :item))))

;; TODO filter out Wiki disambiguation pages?
;;:instance-of wd:Q4167410

(def describe
  "Returns the description of the entity with `id`."
  (memoize
   (fn [id]
     (->> (query
           (template [:select ?description
                      :where [[~(symbol (str "wd:" id)) schema:description ?description]
                              :filter ((lang ?description) = ~*default-language*)]]))
          first
          :description))))

(def label
  "Returns the description of the entity with `id`."
  (memoize
   (fn [id]
     (->> (query
           (template [:select *
                      :where [[~(symbol (str "wd:" id)) rdfs:label ?label]
                              :filter ((lang ?label) = ~*default-language*)]]))
          first
          :label))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO FILTER expressions
;; TODO BIND
;; TODO ASK
