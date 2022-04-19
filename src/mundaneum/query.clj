(ns mundaneum.query
  (:require [clojure.data.json :as json]
            [com.yetanalytics.flint :as f]
            [hato.client :as http]
            [mundaneum.properties :refer [wdt]]
            [tick.core :as tick]))

;; TODO several of the downstream functions should probably have two
;; arities so one can do things like (entity :es "churro").
(def ^:dynamic *default-language* (atom :en))

(defn default-language
  "Returns the unwrapped value of the in-scope binding of `*default-language*`, which can be an atom containing a keyword representing the current default language (like `:en`) or an atom containing such a keyword."
  []
  (if (instance? clojure.lang.IDeref *default-language*)
    (deref *default-language*)
    *default-language*))

(def prefixes
  "RDF prefixes automatically supported by the WikiData query service."
  {:bd "<http://www.bigdata.com/rdf#>"
   :cc "<http://creativecommons.org/ns#>"
   :dct "<http://purl.org/dc/terms/>"
   :geo "<http://www.opengis.net/ont/geosparql#>"
   :hint "<http://www.bigdata.com/queryHints#>"
   :ontolex "<http://www.w3.org/ns/lemon/ontolex#>"
   :owl "<http://www.w3.org/2002/07/owl#>"
   :p "<http://www.wikidata.org/prop/>"
   :pq "<http://www.wikidata.org/prop/qualifier/>"
   :pqn "<http://www.wikidata.org/prop/qualifier/value-normalized/>"
   :pqv "<http://www.wikidata.org/prop/qualifier/value/>"
   :pr "<http://www.wikidata.org/prop/reference/>"
   :prn "<http://www.wikidata.org/prop/reference/value-normalized/>"
   :prov "<http://www.w3.org/ns/prov#>"
   :prv "<http://www.wikidata.org/prop/reference/value/>"
   :ps "<http://www.wikidata.org/prop/statement/>"
   :psn "<http://www.wikidata.org/prop/statement/value-normalized/>"
   :psv "<http://www.wikidata.org/prop/statement/value/>"
   :rdf "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
   :rdfs "<http://www.w3.org/2000/01/rdf-schema#>"
   :schema "<http://schema.org/>"
   :skos "<http://www.w3.org/2004/02/skos/core#>"
   :wd "<http://www.wikidata.org/entity/>"
   :wdata "<http://www.wikidata.org/wiki/Special:EntityData/>"
   :wdno "<http://www.wikidata.org/prop/novalue/>"
   :wdref "<http://www.wikidata.org/reference/>"
   :wds "<http://www.wikidata.org/entity/statement/>"
   :wdt "<http://www.wikidata.org/prop/direct/>"
   :wdtn "<http://www.wikidata.org/prop/direct-normalized/>"
   :wdv "<http://www.wikidata.org/value/>"
   :wikibase "<http://wikiba.se/ontology#>"
   :xsd "<http://www.w3.org/2001/XMLSchema#>"})

(def uri->prefix
  "Reverse lookup table from RDF namespace url to prefix name. Used by `uri->keyword`."
  (reduce (fn [m [k v]]
            (assoc m
                   (subs v 1 (dec (count v)))
                   (str (name k))))
          {}
          prefixes))

(defn uri->keyword
  "Use regex `pattern` to extract the base and final portions of `uri` and convert them into a namespaced keyword. Returns nil if the pattern match is not successful."
  [pattern uri]
    (let [[_ base-uri trimmed-value] (re-find pattern uri)]
      (when-let [prefix (uri->prefix base-uri)]
        (when trimmed-value
          (keyword (uri->prefix base-uri) trimmed-value)))))

(defn clojurize-values
  "Convert the values in `result` to Clojure types."
  [result]
  (into {} (map (fn [[k {:keys [type value datatype] :as v}]]
                  [k (condp = type
                       "uri" (or (uri->keyword #"(.*#)(.*)$" value)
                                 (uri->keyword #"(.*/)([^/]*)$" value)
                                 (:value v))
                       "literal" (condp = datatype
                                   "http://www.w3.org/2001/XMLSchema#decimal" (Float/parseFloat value)
                                   "http://www.w3.org/2001/XMLSchema#integer" (Integer/parseInt value)
                                   "http://www.w3.org/2001/XMLSchema#dateTime" (tick/instant value)
                                   nil value ; no datatype, return literal as is
                                   v) ; unknown datatype, return whole value map
                       v)]) ; unknown value type, return whole value map
                result)))

(defn do-query
  "Query the WikiData endpoint with the SPARQL query in `sparql-text` and convert the return into Clojure data structures."
  [sparql-text]
  (mapv clojurize-values
        (-> (http/get "https://query.wikidata.org/sparql"
                      {:query-params {:query sparql-text
                                      :format "json"}})
            :body
            (json/read-str :key-fn keyword)
            :results
            :bindings)))

(defn clean-up-symbols-and-seqs
  "Remove the namespace portion of namespaced symbols in `sparql-form`. We need to do this because clojure's backtick automatically interns symbols in the current namespace, but the SPARQL DSL wants unnamespaced symbols. Likewise, convert LazySeqs to proper Lists for the benefit of Flint's error checking."
  [sparql-form]
  (clojure.walk/postwalk
   (fn [e]
     (cond (symbol? e) (symbol (name e))
           (seq? e) (apply list e)
           :else e))
   sparql-form))

;; TODO should label service be optional? it should definitely use default-language
(defn query
  ([sparql-form]
   (query sparql-form {}))
  ([sparql-form opts]
   (-> sparql-form
       clean-up-symbols-and-seqs
       (update :prefixes merge prefixes)
       (update :where conj [:service :wikibase/label
                            [[:bd/serviceParam :wikibase/language (str "[AUTO_LANGUAGE]," (name (default-language)))]]])
       f/format-query
       do-query)))

;; TODO memoize by language, maybe switch to using:
;; `wikibase:sitelinks ?sitelinks` for notoriety
(defn entity
  "Return a keyword like :wd/Q42 for the WikiData entity that best matches `label`."
  [label & criteria]
  (-> `{:select [?item ?sitelinks]
        :where  [[?item :rdfs/label {~(default-language) ~label}] ; XXX add default lang support here
                 [?item :wikibase/sitelinks ?sitelinks]
                 ~@(mapv (fn [[p e]] `[?item ~p ~e])
                         (partition 2 criteria))
                 ;; no :instance-of / :subclass-of wikidata properties
                 [:minus [[?item (cat (* :wdt/P31) (+ :wdt/P279)) :wd/Q18616576]]]]
        ;; choose the entity with the most properties
        :order-by [(desc ?sitelinks)]
        :limit 1}
      query
      first
      clojurize-values
      :item))

(defn wdt->wd [arc]
  (let [prefix (namespace arc)
        id (name arc)]
    (if (= prefix "wdt")
      (keyword "wd" id)
      arc)))

;; TODO memoize by language
(defn label
  "Returns the label of the entity with `id`."
  [id]
  (->> `{:select [?label]
         :where  [[~(wdt->wd id) :rdfs/label ?label]
                  [:filter (= (lang ?label) ~(name (default-language)))]]}
       query
       first
       :label))

;; TODO memoize by language
(defn describe
  "Returns the description of the entity with `id`."
  [id] 
  (->> `{:select [?description]
         :where [[~(wdt->wd id) :schema/description ?description]
                 [:filter (= (lang ?description)  ~(name (default-language)))]]}
       query
       first
       :description))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; wdno = no value!
#_(query '{:select [?human ?humanLabel]
         :where [{?human {:wdt/P31 #{:wd/Q5}
                          :rdf/type #{:wdno/P40}}}]
         :limit 20})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO places can be a 2D Point() for lat/lon, or have an entity ID
;; prepended to indicate that the location is not on Earth. Not sure
;; the nicest way to return that info.
#_{:place :wd/Q25908808,
  :loc
  {:datatype "http://www.opengis.net/ont/geosparql#wktLiteral",
   :type "literal",
   :value
   "<http://www.wikidata.org/entity/Q3123> Point(-351.1 -44.87)"}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; more advanced query features

#_(def query-2
  '{:prefixes {:dc  "<http://purl.org/dc/elements/1.1/>"
               :xsd "<http://www.w3.org/2001/XMLSchema#>"}
    :select   [?title]
    :from     ["<http://my-anime-rdf-graph.com>"]
    :where    [[:union [{_b1 {:dc/title     #{{:en "Attack on Titan"}}
                              :dc/publisher #{?publisher}}}]
                       [{_b2 {:dc/title     #{{:jp "進撃の巨人"}}
                              :dc/publisher #{?publisher}}}]]
               {?work {:dc/publisher #{?publisher}
                       :dc/title     #{?title}
                       :dc/date      #{?date}}}
               [:filter (<= #inst "2010-01-01T00:00:00Z" ?date)]]})

