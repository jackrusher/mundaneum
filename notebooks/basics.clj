^{:nextjournal.clerk/visibility :hide-ns}
(ns mundaneum.basics
  (:require [mundaneum.query :refer [search entity entity-data clojurize-claims describe label query *default-language*]]
            [mundaneum.properties :refer [wdt]]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]))

^{::clerk/visibility {:code :hide :result :hide}}
(comment
  
  ;; This is how we start Clerk and tell it to serve this notebook at
  ;; `localhost:7777`. Assuming you've started your repl with the
  ;; `:dev` alias, evaluate these two forms, point your browser there,
  ;; then read on. :)
  (clerk/serve! {:watch-paths ["notebooks"]})
  (clerk/show! "notebooks/basics.clj")

  )

;; # Mundaneum Basics

;; Mundaneum is a wrapper over the
;; [Wikidata](https://www.wikidata.org/) semantic database, which is
;; itself attempt to bring all the knowledge in Wikipedia to the
;; semantic web. Mundaneum tries to bring a very interative, human
;; experience to working with Wikidata from the REPL, but also
;; provides a useful substrate on which to build real applications.

;; If this is your first time working with Wikidata, it might be
;; helpful to read through [this
;; introduction](https://m.wikidata.org/wiki/Wikidata:SPARQL_query_service/queries)
;; in order to get a feel for how things work when using their
;; [SPARQL](https://en.wikipedia.org/wiki/SPARQL) query portal. Many
;; of the examples used here will have been translated from, or
;; inspired by, examples there.

;; ## SPARQL through the lens of Wikidata

;; Wikidata uses a
;; [TripleStore](https://en.wikipedia.org/wiki/Triplestore) to encode
;; their data. This means that everything in the database is stored as
;; a logical assertion in the form of a (_subject_, _predicate_,
;; _object_) triple. In practice, triples look something like 
;; `("Paul Otlet" "born in year" "1868")`.

;; An extremely simple Wikidata SPARQL query might look like this:

;; ```sparql
;; SELECT * WHERE {
;;   wd:Q451 ?p ?o
;; }
;; ```

;; We would read this as "give me all (_predicate_, _object_) pairs
;; for the _subject_ (which they call _entity_) with Wikidata ID
;; `wd:Q451`". Note that entities in Wikidata have numerical IDs like
;; this because they refer to language neutral _concepts_ rather than
;; to _words_ in a given language. Happily, every entity has labels
;; and descriptions associated with it in a variety of languages.

;; To translate this query into our Clojure DSL and execute it, we use
;; a Datomic-like datalog syntax and convert all ID literals from
;; their SPARQL shape (like `wd:Q451`) to Clojure namespaced keywords
;; (like `:wd/Q451`), which gives us:

(query `{:select *
         :where [[:wd/Q451 ?p ?o]]})

;; As you can see, some of the `:o`s are constants, like strings or
;; numbers, but others are namespaced keywords representing other
;; entities in the database. For example, the `:p` values are all IDs
;; that represent predicates (called _properties_) of different
;; kinds. For example, `:wdt/P1705` means "native label", which is the
;; label for that entity in its native language. We can query for that
;; one property like this:

(query `{:select *
         :where [[:wd/Q451 :wdt/P1705 ?o]]})

;; ## Entity and Property helper functions

;; As is probably obvious from the previous examples, the first
;; challenge when querying Wikidata is finding the right IDs for
;; entities and properties. Mundaneum provides some functions to help
;; with this process, like the `entity` function to convert a string
;; containing the label of an entity into a namespaced keyword we can
;; use in queries:

(entity "Mundaneum")

;; Using entity lookups, we could have written our last query like
;; this:

(query `{:select *
         :where [[~(entity "Mundaneum") :wdt/P1705 ?o]]})

;; There's another helper function called `wdt` that's useful for
;; finding properties. So, for example, we can get the ID for the
;; "native label" property like this:

(wdt :native-label)

;; Which gives us a somewhat easier to read query:

(query `{:select *
         :where [[~(entity "Mundaneum") ~(wdt :native-label) ?o]]})

;; One thing to keep in mind with the `entity` function is that
;; sometimes the default result isn't the one we want.

(entity "U2")

;; Now we know that the Wikidata ID for U2 is `:wd/Q396`... or do we?
;; Which U2 does that ID represent? We can use `describe` to get a
;; natural description of the entity to make sure:

(describe (entity "U2"))

;; But that isn't the "U2" I had in mind at all! Fortunately, the
;; `entity` function can also take property/entity pairs as additional
;; criteria to direct it to the right entity. Here we look for the
;; "U2" that's `:part-of` the "Berlin U-Bahn" (an underground public
;; transit system):

(entity "U2" (wdt :transport-network) (entity "Berlin U-Bahn"))

;; And, once more, we can check to make sure it's right one:

(describe (entity "U2" (wdt :transport-network) (entity "Berlin U-Bahn")))

;; To make it easier to find the right entity without doing a series
;; of probes using the `entity` function, Mundaneum supplies a
;; `search` function that performs text search for entities using
;; their labels and aliases:

^{::clerk/viewer clerk/table}
(search "Alien")

;; And, for completeness, there's a helper function to pull all the
;; data for a given entity. For example, plugging in the ID from the
;; film Ridley Scott film Alien we get:

(def alien
  (entity-data :wd/Q103569))

;; This nested map structure has many keys, the most interesting of
;; which are `:labels`, `:aliases`, `:descriptions` (all of each for
;; every language), and `:claims`.

;; The `:claims` key contains a map where the keys are properties and
;; the values are a list of asserted values for that property, along
;; with the source of that "claim". This somewhat journalistic
;; approach allows Wikidata to record multiple competing claims about
;; the world.

;; Because it's often useful to automatically convert the claims to a
;; form more like what queries return, there's a helper function for
;; that:

(def alien-claims
  (clojurize-claims alien))

;; The keys in the resulting map are human-readable property keywords
;; suitable to pass to the `wdt` function, while each value is a
;; vector containing the possible values for that property. Keep in
;; mind that multiple values might indicate that the cardinality of
;; the answer is greater than one (a film that won multiple awards,
;; say), or it could mean that the cardinality is one but the true
;; value is contested.

;; An an example of the former case, here are all the awards the film
;; received:

^{::clerk/viewer clerk/table}
(mapv #(hash-map :award (label %)) (:award-received alien-claims))

;; Note that if you are only trying to find out something like this,
;; you would probably be better off using a query than pulling all the
;; facts for the entity:

^{::clerk/viewer clerk/table}
(query `{:select [?awardLabel]
         :where [[:wd/Q103569 ~(wdt :award-received) ?award]]})

;; ## Using multiple properties

;; Now that we know the shape of queries and how to look up entities
;; and properties, let's have a look at a few examples using multiple
;; properties in a query.

;; This query tries to match any entity `?person` whose
;; `:writing-language` was "Ancient Greek" and is recorded as having
;; an `:occupation` of "philosopher". Note that we are selecting
;; `?personLabel` rather than `?person` here, which is a bit of magic
;; that lets us ask for the human readable label for the entity
;; `?person` by appending `Label` to the end of that logic
;; variable. We're also `:limit`ing our result set to 20 people.

^{::clerk/viewer clerk/table}
(query `{:select [?personLabel]
         :where [[?person ~(wdt :writing-language) ~(entity "Ancient Greek")]
                 [?person ~(wdt :occupation) ~(entity "philosopher")]]
         :limit 20})

;; We can use multiple properties to perform cross-cutting queries
;; that answer questions that no textual search engine could. For
;; example, here we ask which drugs interact with which gene products,
;; which genes encode those products, and which diseases are
;; associated with those genes. This automatically produces a list of
;; drugs that might be useful in targeting those diseases.
^{::clerk/viewer clerk/table}
(->> (query `{:select [?drugLabel ?geneLabel ?diseaseLabel]
              :where [[?drug ~(wdt :physically-interacts-with) ?gene_product]
                      [?gene_product ~(wdt :encoded-by) ?gene]
                      [?gene ~(wdt :genetic-association) ?disease]]
              :limit 100})
     (sort-by :drugLabel))

;; ## Aggregation

;; Queries also support aggregating and ordering operators, like
;; `count`, `:group-by`, and `:order-by`. Here's a query for eye color
;; frequency using all three:

^{::clerk/viewer clerk/table}
(query `{:select [?eyeColorLabel [(count ?person) ?count]]
         :where [[?person ~(wdt :eye-color) ?eyeColor]]
         :group-by [?eyeColorLabel]
         :order-by [(desc ?count)]})

;; ## Multilingual Support

;; Wikidata is aggressively multilingual! This can come in handy while
;; doing translation or digital humanities work, or doing any kind of
;; research in several languages. Here's a query that lists species of
;; Swift (the bird) with their English and German labels (which are
;; both often Latin). Note that we're using a server-side `:filter` to
;; make sure we only return birds for which we have names in both
;; languages.

^{::clerk/viewer clerk/table}
(query `{:select [?englishName ?germanName]
         :where [[?bird ~(wdt :parent-taxon) ~(entity "Apodiformes")]
                 [?bird :rdfs/label ?germanName]
                 [?bird :rdfs/label ?englishName]
                 [:filter (and (= (lang ?germanName) "de")
                               (= (lang ?englishName) "en"))]]
         :limit 10})

;; In addition to getting results across multiple languages, we can
;; also _query_ in different languages. To make this easier, Mundaneum
;; offers several ways to specify what language to use for entities,
;; labels, and descriptions.

;; In queries, a language-specific string is represented by a map
;; containing an ISO language code keyword as key and a string as
;; value, like `{:en "human"}`. This can also be used to specify the
;; language to use for an entity lookup. Here we ask for the entity
;; associated with the German label "Mensch" (the german word for
;; "human"):

(entity {:de "Mensch"})

;; The `label`, `describe` and `search` functions can also take an
;; extra first parameter indicating which language to use. This makes
;; it easy to find out the Catalan label and French description of
;; `:wd/Q5`.

(label :ca :wd/Q5)

(describe :fr :wd/Q5)

^{::clerk/viewer clerk/table}
(search :it "Dante")

;; So far, we've been operating with the English language as the
;; default. It's also possible to set the language for all operations
;; in a given scope using the dynamic variable `*default-language*`.

;; For example, here we ask for administrative territories within the
;; Republic of Ireland using the Irish name for the country while
;; fetching the entity ID and also receiving the results in the Irish
;; language.
^{::clerk/viewer clerk/table}
(binding [*default-language* :ga]
  (query `{:select [?areaLabel]
           :where [[~(entity "Éire")
                    ~(wdt :contains-the-administrative-territorial-entity)
                    ?area]]
           :limit 50}))

;; We can also set the default language system wide by resetting the
;; `*default-language*` atom to a keyword representing an ISO language
;; code. For example, to specify French as the default language one
;; would: `(reset! *default-language* :fr)`.

;; ## Federated Queries

;; One of the most powerful features of SPARQL is the ability to make
;; Federated Queries. When we send a query that contains a `:service`
;; section, it tells the server that it should forward that part of
;; the query to _another_ server and return the combined the results.

;; This query asks Wikidata for the `:WikiPathways-ID` for Melatonin
;; metabolism, but also tells the Wikidata server to ask the endpoint
;; at `<http://sparql.wikipathways.org/sparql>` (a separate database
;; of biological pathways) to find all the metabolites that are part
;; of that pathway.

^{::clerk/viewer clerk/table}
(query `{:prefixes {:dc "<http://purl.org/dc/elements/1.1/>"
                    :wp "<http://vocabularies.wikipathways.org/wp#>"}
         :select [?metaboliteName]
         :where [[:values {?item [~(entity "Melatonin metabolism and effects")]}]
                 [?item ~(wdt :WikiPathways-ID) ?wpid]
                 [?item ~(wdt :exact-match) ?source_pathway]
                 [:service "<http://sparql.wikipathways.org/sparql>"
                  [[?wp_pathway :dc/identifier ?source_pathway]
                   {?metabolite {a             #{:wp/Metabolite} 
                                 :dct/isPartOf #{?wp_pathway}
                                 :rdfs/label    #{?metaboliteName}}}]]]
         :limit 20})

;; This concludes our tour of the basic features of Mundaneum. The
;; following sections will feature some more advanced use cases. 😊
