(ns mundaneum.query
  (:require [clojure.data.json :as json]
            [mundaneum.properties :refer [wdt wdt->readable]]
            [com.yetanalytics.flint :as f]
            [hato.client :as http]
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

;; TODO should label service be optional?
(defn query
  ([sparql-form]
   (query {} sparql-form))
  ([opts sparql-form]
   (-> sparql-form
       clean-up-symbols-and-seqs
       (update :prefixes merge prefixes)
       (update :where conj [:service :wikibase/label
                            [[:bd/serviceParam :wikibase/language (str "[AUTO_LANGUAGE]," (name (default-language)))]]])
       f/format-query
       do-query)))

(def entity*
  "Memoized implementation of language-aware entity lookup."
  (memoize
   (fn [lang label criteria]
     (-> `{:select [?item ?sitelinks]
           :where  [[?item :rdfs/label {~lang ~label}]
                    [?item :wikibase/sitelinks ?sitelinks]
                    ~@(mapv (fn [[p e]] `[?item ~p ~e])
                            (partition 2 criteria))
                    ;; no :instance-of / :subclass-of wikidata properties
                    ;; and no disambiguation pages
                    [:minus [[?item (cat (* :wdt/P31) (+ :wdt/P279)) :wd/Q18616576]
                             [?item :wdt/P31 :wd/Q4167410]]]]
           ;; choose the entity with the most sitelinks on Wikipedia 
           :order-by [(desc ?sitelinks)]
           :limit 1}
         query
         first
         :item))))

(defn entity
  "Return a keyword like :wd/Q42 for the most popular WikiData entity that matches `label`."
  [label & criteria]
  (let [[lang label'] (if (map? label)
                        (first label)
                        [(default-language) label])]
    (entity* lang label' criteria)))

(defn wdt->wd [arc]
  (let [prefix (namespace arc)
        id (name arc)]
    (if (#{"p" "ps" "wdt"} prefix)
      (keyword "wd" id)
      arc)))

(def label*
  "Memoized implementation of language-aware label lookup."
  (memoize
   (fn [lang id]
     (let [rdfs-label (->> `{:select [?label]
                             :where  [[~(wdt->wd id) :rdfs/label ?label]
                                      [:filter (= (lang ?label) ~(name lang))]]}
                           query
                           first
                           :label)]
       (if rdfs-label rdfs-label id)))))

(defn label
  "Returns the label of the entity with `id`. If `lang` is specified it, overrides `*default-language*`."
  ([id] (label* (default-language) id))
  ([lang id] (label* lang id)))

(def describe*
  "Memoized implementation of language-aware description lookup."
  (memoize
   (fn [lang id]
     (->> `{:select [?description]
            :where [[~(wdt->wd id) :schema/description ?description]
                    [:filter (= (lang ?description) ~(name lang))]]}
          query
          first
          :description))))

(defn describe
  "Returns the description of the entity with `id`. If `lang` is specified it, overrides `*default-language*`."
  ([id] (describe* (default-language) id))
  ([lang id] (describe* lang id)))

(defn search
  ([text]
   (search (default-language) text))
  ([lang text]
   (->> (-> (http/get "https://www.wikidata.org/w/api.php"
                      {:query-params {:action "wbsearchentities"
                                      :search text
                                      :language (name lang)
                                      :uselang (name lang)
                                      :type "item"
                                      :format "json"}})
            :body
            (json/read-str :key-fn keyword)
            :search)
        (map (fn [{:keys [:description :id :display] :as vs}]
               {:id (keyword "wd" id)
                :description description
                :label (get-in display [:label :value])})))))

;; gets everything for the entity; :claims key has the properties
(defn entity-data [item]
  (let [item-id (name item)]
    ((keyword item-id)
     (-> (http/get (str "https://www.wikidata.org/wiki/Special:EntityData/" item-id ".json")
                   {:query-params {:format "json"}})
         :body
         (json/read-str :key-fn keyword)
         :entities))))

(defn clojurize-claim
  "Convert the values in `result` to Clojure types."
  [{:keys [datatype datavalue] :as snak}]
  (condp = datatype
    "monolingualtext" {(-> datavalue :value :language keyword) (-> datavalue :value :text)} 
    "wikibase-entityid" (keyword (str "wd/" (-> datavalue :id)))
    "wikibase-item" (keyword (str "wd/" (-> datavalue :value :id)))
    "commonsMedia" (-> datavalue :value)
    "string" (-> datavalue :value)
    "external-id" (-> datavalue :value)
    "time" (-> datavalue :value)
    "url" (-> datavalue :value)
    "quantity" {:amount (-> datavalue :value :amount)
                :units (-> datavalue :value :unit)}
    [datatype datavalue]))

(defn clojurize-claims [e-data]
  (reduce
   (fn [m [prop snaks]]
     (assoc m (wdt->readable (->> prop name (str "wdt/") keyword))
            (mapv (comp clojurize-claim :mainsnak) snaks)))
   {}
   (:claims e-data)))

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

