(ns mundaneum.document)

;; Helper functions for the official wikidata Java API

(def fetcher
  (let [fetcher (org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher/getWikidataDataFetcher)
        filter  (.getFilter fetcher)]
    ;; only the english wiki just now
    (.setSiteLinkFilter filter (java.util.Collections/singleton "enwiki"))
    (.setLanguageFilter filter (java.util.Collections/singleton "en"))
    fetcher))

(def entity-document
  (memoize (fn [id] (.getEntityDocument fetcher id))))

(def entity-document-by-title
  (memoize (fn [title] (.getEntityDocumentByTitle fetcher "enwiki" title))))

(defn document-id [doc]
  (.getId (.getItemId doc)))

(defn property-id [thing]
  (.getId (.getPropertyId thing)))

(defn value-id [thing]
  (.getId (.getValue thing)))

(defn find-claim [id statement]
  (->> (iterator-seq (.getAllQualifiers (.getClaim statement)))
       (filter #(= id (property-id %)))
       first))

(defn find-statement [id statements]
  (first (filter #(= id (value-id %)) statements)))

(defn find-statement-group [id document]
  (.getStatements (.findStatementGroup document id)))

(defn label [document]
  (.getText (.getValue (first (.getLabels document)))))

(defn id->label [id]
  (label (entity-document id)))
