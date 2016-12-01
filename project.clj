(defproject mundaneum "0.1.0-SNAPSHOT"
  :description "A clojure wrapper around WikiData."
  :url "https://github.com/jackrusher/mundaneum"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-time "0.12.2"]
                 [clj-http "3.1.0"]
                 [org.wikidata.wdtk/wdtk-wikibaseapi "0.7.0"]
                 [org.eclipse.rdf4j/rdf4j-query "2.1.2"]
                 [org.eclipse.rdf4j/rdf4j-repository-api "2.1.2"]
                 [org.eclipse.rdf4j/rdf4j-runtime "2.1.2"]])
