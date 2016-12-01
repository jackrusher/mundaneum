(ns mundaneum.document)

;; Helper functions for the official wikidata Java API

(def fetcher
  (let [fetcher (org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher/getWikidataDataFetcher)
        filter  (.getFilter fetcher)]
    ;; only the english wiki just now
    (.setSiteLinkFilter filter (java.util.Collections/singleton "enwiki"))
    (.setLanguageFilter filter (java.util.Collections/singleton "en"))
    fetcher))

(def get-entity-document
  (memoize (fn [id] (.getEntityDocument fetcher id))))

(def entity-document-by-title
  (memoize (fn [title] (.getEntityDocumentByTitle fetcher "enwiki" title))))

(defn get-document-id [doc]
  (.getId (.getItemId doc)))

(defn get-property-id [thing]
  (.getId (.getPropertyId thing)))

(defn get-value-id [thing]
  (.getId (.getValue thing)))

(defn find-claim [id statement]
  (->> (iterator-seq (.getAllQualifiers (.getClaim statement)))
       (filter #(= id (get-property-id %)))
       first))

(defn find-statement [id statements]
  (first (filter #(= id (get-value-id %)) statements)))

(defn find-statement-group [id document]
  (.getStatements (.findStatementGroup document id)))

(defn get-label [document]
  (.getText (.getValue (first (.getLabels document)))))

(defn id->label [id]
  (get-label (get-entity-document id)))
