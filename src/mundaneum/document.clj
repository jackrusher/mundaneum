(ns mundaneum.document)

;; WikiData entities are served as documents via the WikibaseDataFetcher

(def fetcher
  "Singleton fetcher instance for the WikiData API endpoint."
  (let [fetcher (org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher/getWikidataDataFetcher)
        filter  (.getFilter fetcher)]
    ;; only the english wiki just now
    ;; (.setSiteLinkFilter filter (java.util.Collections/singleton "enwiki"))
    ;; (.setLanguageFilter filter (java.util.Collections/singleton "en"))
    fetcher))

(def entity-document
  "Returns an entity document by `id`, caching the document."
  (memoize (fn [id] (.getEntityDocument fetcher id))))

(def entity-document-by-title
  "Returns an entity document by `title`, caching the document."
  (memoize (fn [title] (.getEntityDocumentByTitle fetcher "enwiki" title))))

(defn document-id
  "Returns the entity id of `doc`."
  [doc]
  (.getId (.getItemId doc)))

(defn property-id
  "Returns the property id of `thing`."
  [thing]
  (.getId (.getPropertyId thing)))

(defn value-id
  "Retuns the id of the value of `thing`."  
  [thing]
  (.getId (.getValue thing)))

(defn statement-group-by-id
  "Returns the statement group of `statement` that matches `id`."  
  [id document]
  (.getStatements (.findStatementGroup document id)))

(defn statements-by-id
  "Returns the claims of `statement` that match `id`."  
  [id statements]
  (filter #(= id (value-id %)) statements))

(defn statement-by-id
  "Return the first claim of `statement` that matches `id`."  
  [id statements]
  (first (statements-by-id id statements)))

(defn claims
  "Return the claims for `statement`."  
  [statement]
  (iterator-seq (.getAllQualifiers (.getClaim statement))))

(defn claims-by-id
  "Return the claims of `statement` that match `id`."  
  [id statement]
  (filter #(= id (property-id %)) (claims statement)))

(defn claim-by-id [id statement]
  "Return the first claim of `statement` that matches `id`."  
  (first (claims-by-id id statement)))

(defn label
  "Returns the text of one of the labels of `document`, optionally choosing in the order specified by the sequence of language names `langs`."
  ([document] (label document ["en" "de" "fr" "es" "it" "ca" "nl"]))  
  ([document langs]
   (-> (let [labels (.getLabels document)]
         (loop [langs ["en" "de" "fr" "es" "it" "ca" "nl"]]
           (if-let [text (get labels (first langs))]
             text
             (if (empty? langs)
               (first labels)
               (recur (rest langs))))))
       (.getText))))

(defn id->label
  "Returns the text of the first label of the document with `id`."
  [id]
  (label (entity-document id)))

(defn describe
  "Return the English textual description of the document with `id`."
  ([id] (describe id "en"))
  ([id lang]
   (when-let [descs (.getDescriptions (entity-document id))]
     (when-let [desc (get descs lang)]
       (.getText desc)))))

(defn properties
  "Return a set of properties for the claims made about the entity with this `id`."
  [id]
  (into #{}
        (map #(.getId (.getPropertyId (.getMainSnak (.getClaim %))))
             (iterator-seq
              (.getAllStatements (entity-document id))))))
