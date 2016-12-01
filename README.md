# Mundaneum

This is a tiny, highly incomplete clojure wrapper around
the [Wikidata](https://www.wikidata.org/wiki/Wikidata:Main_Page)
project's massive semantic database. It's named after
the [Mundaneum](https://en.wikipedia.org/wiki/Mundaneum), which
was [Paul Otley](https://en.wikipedia.org/wiki/Paul_Otlet)'s mad and
wonderful c. 1910 vision for something like the World Wide Web.

(There's a mini-doc about him and it
[here](https://www.youtube.com/watch?v=hSyfZkVgasI).)

## Condition

The state of this code is appalling. It's around six person-hours old,
and currently a flaming mess of fragility and typos. It is presently
presented for entertainment purposes only.

This project is just for fun, so I'm not really sure if I'll turn it
into a proper library with things like doc-strings and tests and some
possibility of actually working for anyone but me.

In the meantime, it can at least answer questions like:

``` clojure
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
```

Enjoy!

## License

Copyright © 2016 Jack Rusher

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
