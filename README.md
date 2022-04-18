# Mundaneum

This is a thin Clojure wrapper around the
[Wikidata](https://www.wikidata.org/wiki/Wikidata:Main_Page) project's
massive semantic database. It's named after the
[Mundaneum](https://en.wikipedia.org/wiki/Mundaneum), which was [Paul
Otley](https://en.wikipedia.org/wiki/Paul_Otlet)'s mad and wonderful
c. 1910 vision for something like the World Wide Web.

(There's a mini-doc about him and it
[here](https://www.youtube.com/watch?v=hSyfZkVgasI).)

## Motivation

Wikidata is amazing! And it provides API access to all the knowledge
it has collected! This is great, but exploratory programmatic access
to that data can be fairly painful.

The official Wikidata API Java library offers a document-oriented
interface that makes it hard to ask interesting questions. A better
way to do most things is with the Wikidata query service, which uses
the
standard [Semantic Web](https://en.wikipedia.org/wiki/Semantic_Web)
query language, [SPARQL](https://en.wikipedia.org/wiki/SPARQL).

The SPARQL query service is nice, but because WikiData's data model
must cope with (a) items with multiple names in multiple languages,
and (b) single names that map to multiple items, they've used a layer
of abstraction by which everything in the DB is referred to by an `id`
that looks like `P50` (property number 50, meaning "author") or
`Q6882` (entity number 6882, the author "James Joyce").

For example, to get a selection of works authored by James Joyce,
one would issue a query like:

``` sparql
SELECT ?work
WHERE { ?work wdt:P50 wd:Q6882. } 
LIMIT 10
```

(Users of [Datomic](http://www.datomic.com) will recognize the `?work`
style of selector, which is not a coincidence as SPARQL and Datomic
are both flavors of [Datalog](https://en.wikipedia.org/wiki/Datalog).)

The above query is simple enough, except for the non-human-readable
identifiers in the `WHERE` clause, which were both found by manually
searching the web interface at Wikidata.

In order to do exploratory programming against this API in a more
human-friendly way without leaving my coding environment, I've built
this library. The approach I took was:

* download and reformat the full list of ~2000 properties (fresh as of
  2022-04-18), shape them into a map of keyword/keyword pairs where
  the key is made form the English name of the property and the value
  is a namespaced keyword like `:prefix/id`:

``` clojure
(wdt :author)
;;=> :wdt/P50
```

* create a helper function that tries to correctly guess the id of an
  entity based on a string that's similar to its "label" (common name,
  currently sadly restricted to English in this code)

``` clojure
(entity "James Joyce")
;;=> :wd/Q6882

;; the entity function tries to return the most notable entity 
;; that matches, but sometimes that isn't what you want.

(describe (entity "U2"))
;;=> "Irish alternative rock band"

;; not the one I meant, let's try with more info:
(describe (entity "U2" (wdt :part-of) (entity "Berlin U-Bahn")))
;;=> "underground line in Berlin"
```

This already helps to keep my emacs-driven process running
smoothly. The next point of irritation was assembling query strings by
hand, like an animal. Luckily, the very well put together
[Flint](https://github.com/yetanalytics/flint/) library provides an
excellent Clojure DSL for the SPARQL query language. Combined with my
helper functions, this looks like:

``` clojure
;; what are some works authored by James Joyce?
(query (template {:select [?work ?workLabel]
                  :where  [[?work ~(wdt :author) ~(entity "James Joyce")]]
                  :limit 10})
;; [{:work "Q864141", :workLabel "Eveline"}
;;  {:work "Q861185", :workLabel "A Little Cloud"}
;;  {:work "Q459592", :workLabel "Dubliners"}
;;  {:work "Q682681", :workLabel "Giacomo Joyce"}
;;  {:work "Q764318", :workLabel "Two Gallants"}
;;  {:work "Q429967", :workLabel "Chamber Music"}
;;  {:work "Q465360", :workLabel "A Portrait of the Artist as a Young Man"}
;;  {:work "Q6511", :workLabel "Ulysses"}
;;  {:work "Q866956", :workLabel "An Encounter"}
;;  {:work "Q6507", :workLabel "Finnegans Wake"}] 
```

This is actually quite similar to the programmatic query interface I
created for the first
purpose-built [TripleStore](https://en.wikipedia.org/wiki/Triplestore)
around 20 years ago.

This code is much easier to understand if you have some familiarity
with SPARQL and how it can be used to query Wikidata. I strongly
recommend [this
introduction](https://m.wikidata.org/wiki/Wikidata:SPARQL_query_service/queries)
to get started. I'm trying to make sure all the examples are easy to
translate to the DSL used here.

## Learn more

The `mundaneum.examples` namespace is all examples, should you care to
have a play. In addition to working through those, a good exercise is
to visit the WikiData SPARQL examples page and translate some
interesting queries into the DSL.

Enjoy!

## License

Copyright © 2016-2022 Jack Rusher. Distributed under the BSD 0-clause license.
