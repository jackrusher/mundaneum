# Mundaneum

This is a thin Clojure wrapper around the
[Wikidata](https://www.wikidata.org/wiki/Wikidata:Main_Page) project's
massive semantic database. It's named after the
[Mundaneum](https://en.wikipedia.org/wiki/Mundaneum), which was [Paul
Otley](https://en.wikipedia.org/wiki/Paul_Otlet)'s mad and wonderful
c. 1910 vision for something like the World Wide Web.

(There's a mini-doc about him and it
[here](https://www.youtube.com/watch?v=hSyfZkVgasI).)

## Coordinates

Recent changes to the group-id policy at Clojars have made publishing
artifacts there less appealing to me, so for the moment this library
is available to `deps.edn` users at:

``` clojure
io.github.jackrusher/mundaneum {:git/sha "SHA"}
```

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
(query `{:select [?work ?workLabel]
         :where  [[?work ~(wdt :author) ~(entity "James Joyce")]]
         :limit 10}
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
to get started. I've tried to make sure all the examples are easy to
translate to the DSL used here.

## Multilingual support

In queries, a language-specific string can be specified with a map
containing an ISO language code keyword as key and a string as value,
like `{:en "human"}`. This can also be used to specify the language to
use for an entity lookup:

``` clojure
(entity {:de "Mensch"})
;;=> :wd/Q5
```

The `label` and `describe` functions can also take an extra first
parameter indicating the language to use:

``` clojure
(label :de :wd/Q5)
;;=> "Mensch"
(describe :fr :wd/Q5)
;;=> "individu appartenant à l’espèce Homo sapiens, la seule espèce restante du genre Homo – distinct de « humain fictif » et de « humain possiblement fictif »"
```

Note that these calls are all memoized _per language_, so repeatedly
looking up a given entity/label/description causes no additional
network traffic.

In addition to these affordances, there is also a dynamic variable
`mundaneum.query/*default-language*` which is an `atom` containing an
ISO language code keyword like `:en` that controls which language will
be used _by default_ for labels and description input/output. If you
are planning to enjoy an interactive session in French you could set
the default like this:

``` clojure
(reset! *default-language* :fr)
```

On the other hand, if you want to mix languages freely, you can use a
local binding like this:

``` clojure
;; lookup an entity using Thai as the default language, then get the
;; English label for it.
(let [thai-name "กรุงเทพมหานคร"
      id (binding [*default-language* :th]
           (entity thai-name))]
  (str thai-name " is called " (label id) " in English."))
;;=> "กรุงเทพมหานคร is called Bangkok in English."
```

Although if one is doing something like this, it's probably nicer to
use the previously described API:

``` clojure
(describe (entity {:th "กรุงเทพมหานคร"}))
"capital of Thailand"
```

## Learn more

Additional documentation can be found in the Clerk notebooks in the
`notebooks` directory, beginning with `basics.clj`. If you start your
REPL with the `:dev` alias, you'll already have Clerk loaded. (This
will happen automatically if you use `cider-jack-in` from Emacs via a
bit of configuration in this repo's `.dir-locals` file.)

Enjoy!

## License

Copyright © 2016-2022 Jack Rusher. Distributed under the BSD 0-clause license.
