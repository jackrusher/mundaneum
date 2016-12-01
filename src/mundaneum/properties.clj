(ns mundaneum.properties)

;; Some pre-spidered properties to make it easier to create queries. I
;; should write some code to keep this list up-to-date, as there are
;; only ~3000 of them in total.

;; (reduce
;;  #(assoc %1 (keyword (cs/replace (first %2) #"[ /]" "-")) (rest %2))
;;  {}
;; )

(def properties
  '{:author ["P50"]
    :coordinate-location ["P625"]
    :religion
    ["P140"
     "Item"
     "religion: religion of a person, organization or religious building. Note that a church is not a religion!"
     "Elizabeth II <religion> Church of England"
     "-"],
    :canonization-status
    ["P411"
     "Item"
     "canonization status: stage in the process of attaining sainthood per the subject's religious organization"
     "John Paul II <canonization status> beatification"
     "-"],
    :member-of-political-party
    ["P102"
     "Item"
     "political party: the political party of which this politician is or has been a member"
     "Angela Merkel <member of political party> Christian Democratic Union"
     "-"],
    :academic-degree
    ["P512"
     "Item"
     "academic degree and no label: academic degree that the person holds"
     "Paul Krugman <academic degree> Doctor of Philosophy"
     "-"],
    :noble-family
    ["P53"
     "Item"
     "noble family: include dynasty and nobility houses"
     "Genghis Khan <noble family> Borjigin"
     "-"],
    :employer
    ["P108"
     "Item"
     "employer: organization for which the subject works or worked"
     "Enrico Fermi <employer> University of Chicago"
     "-"],
    :ancestral-home
    ["P66"
     "Item"
     "ancestral home: place of origin for ancestors of subject"
     "Hu Shih <ancestral home> Jixi County"
     "-"],
    :notable-work
    ["P800"
     "Item"
     "work and magnum opus: subject's notable scientific work or work of art, literature, or significance"
     "Émile Zola <notable work> Germinal"
     "-"],
    :position-held
    ["P39"
     "Item"
     "mandate: subject currently or formerly holds the object position or public office"
     "Angela Merkel <position held> Chancellor of Germany"
     "-"],
    :voice-type
    ["P412"
     "Item"
     "voice type: person's voice type. expected values: soprano, mezzo-soprano, contralto, countertenor, tenor, baritone, bass [and derivatives]"
     "Cesare Valletti <voice type> tenor"
     "-"],
    :birth-name
    ["P1477"
     "Monolingual text"
     "name at birth: full name of a person at birth, if different from their current, generally used name [samples: John Peter Doe for Joe Doe, Ann Smith for Ann Miller]"
     "John Paul II <birth name> Karol Józef Wojtyła [Polish]"
     "-"],
    :instrument
    ["P1303"
     "Item"
     "musical instrument: instrument that a person plays"
     "Ringo Starr <instrument> drum"
     "-"],
    :audio-recording-of-the-subjects-spoken-voice
    ["P990"
     "Commons media file"
     "audio file representing the speaking voice of a person; or of an animated cartoon or other fictitious character"
     "Mary Robinson <audio recording of the subject's spoken voice> Mary Robinson - Desert Island Discs - 28 July 2013.flac"
     "-"],
    :name-in-native-language
    ["P1559"
     "Monolingual text"
     "name: name of a person in their native language"
     "Amakusa Shirō <name in native language> 天草 四郎 [Japanese]"
     "-"],
    :date-of-death
    ["P570"
     "Point in time"
     "date of death: date on which the subject died"
     "Jean-François Champollion <date of death> 1832-03-04"
     "-"],
    :image-of-grave
    ["P1442"
     "Commons media file"
     "picture of a person or animal's grave, gravestone or tomb"
     "Lucas Cranach the Elder <image of grave> Cranach der Ältere Grab.jpg"
     "-"],
    :languages-spoken-written-or-signed
    ["P1412"
     "Item"
     "language proficiency: language[s] that a person speaks or writes. Can include native languages"
     "Margrethe II of Denmark <languages spoken, written or signed> Danish, English, French, Swedish, German and Faroese"
     "-"],
    :native-language
    ["P103"
     "Item"
     "first language: language or languages a person has learned from birth"
     "Usievalad Ihnatouski <native language> Belarusian"
     "-"],
    :affiliation
    ["P1416"
     "Item"
     "organization that a person is affiliated with"
     "Raoul Bott <affiliation> Institute for Advanced Study"
     "-"],
    :occupation
    ["P106"
     "Item"
     "profession: occupation of a person; see also 'field of work' [Property:P101], 'position held' [Property:P39]"
     "Abraham Klein <occupation> physicist"
     "-"],
    :official-residence
    ["P263"
     "Item"
     "official residence: the residence at which heads of government and other senior figures officially reside"
     "Prime Minister of Canada <official residence> 24 Sussex Drive"
     "-"],
    :signature
    ["P109"
     "Commons media file"
     "signature: image of a person's signature"
     "Leo Tolstoy <signature> Leo Tolstoy signature.svg"
     "-"],
    :Commons-Creator-page
    ["P1472"
     "String"
     "name of the person's creator page on Wikimedia Commons [without the prefix 'Creator']"
     "Augustus Henry Fox <Commons Creator page> Henry Fox Augustus Henry Fox"
     "-"],
    :birthday
    ["P3150"
     "Item"
     "birthday: item for day and month on which the subject was born. Used when full 'date of birth' [P569] isn't known."
     "Haruka Shimotsuki <birthday> November 15"
     "-"],
    :doctoral-student
    ["P185"
     "Item"
     "doctoral student[s] of a professor"
     "Enrico Fermi <doctoral student> Tsung-Dao Lee"
     "doctoral advisor"],
    :sexual-orientation
    ["P91"
     "Item"
     "sexual orientation: the sexual orientation of the person - use IF AND ONLY IF they have stated it themselves, unambiguously, or it has been widely agreed upon by historians after their death"
     "Alan Turing <sexual orientation> homosexuality"
     "-"],
    :honorific-prefix
    ["P511"
     "Item"
     "honorific and title of honor: word or expression used before a name, in addressing or referring to a person"
     "Douglas Haig, 1st Earl Haig <honorific prefix> The Right Honourable"
     "-"],
    :Project-Gutenberg-author-ID
    ["P1938"
     "External identifier"
     "Project Gutenberg: author identifier at Project Gutenberg"
     "David Christie Murray <Project Gutenberg author ID> 25179"
     "-"],
    :educated-at
    ["P69"
     "Item"
     "Alma mater: educational institution attended by the subject"
     "Abraham Klein <educated at> Harvard University"
     "-"],
    :award-received
    ["P166"
     "Item"
     "award: award or recognition received by a person, organisation or creative work"
     "Liu Xiaobo <award received> 2010 Nobel Peace Prize"
     "-"],
    :given-name
    ["P735"
     "Item"
     "given name: first name or another given name of this person. Values used with the property shouldn't link disambiguations nor family names."
     "George Washington <given name> George"
     "-"],
    :date-of-birth
    ["P569"
     "Point in time"
     "date of birth: date on which the subject was born"
     "Jean-François Champollion <date of birth> 1790-12-23"
     "-"],
    :killed-by
    ["P157"
     "Item"
     "murderer: person who killed the subject"
     "John F. Kennedy <killed by> Lee Harvey Oswald"
     "-"],
    :cause-of-death
    ["P509"
     "Item"
     "cause of death: underlying or immediate cause of death. Underlying cause [e.g. car accident, stomach cancer] preferred. Use 'manner of death' [P1196] for broadest category, e.g. natural causes, accident, homicide, suicide"
     "Theodore Roosevelt <cause of death> coronary thrombosis"
     "-"],
    :shooting-handedness
    ["P423"
     "Item"
     "whether the hockey player passes oŗ shoots left- or right-handed"
     "Joe Thornton <shooting handedness> left-handed shot"
     "-"],
    :field-of-work
    ["P101"
     "Item"
     "field of work: specialization of a person or organization, see P106 for the occupation"
     "Abraham Klein <field of work> quantum field theory"
     "-"],
    :coat-of-arms-image
    ["P94"
     "Commons media file"
     "coat of arms: image of the item's coat of arms"
     "Phillipe de Plessis <coat of arms image> Armoiries Philippe du Plaissis.svg"
     "-"],
    :Eight-Banner-register
    ["P470"
     "Item"
     "Manchu household register for people of the Qing Dynasty"
     "Oboi <Eight Banner register> Manchu Bordered Yellow Banner"
     "-"],
    :coat-of-arms
    ["P237"
     "Item"
     "coat of arms: subject's coat of arms"
     "George Washington <coat of arms> Coat of arms of George Washington"
     "-"],
    :convicted-of
    ["P1399"
     "Item"
     "crime a person was convicted of"
     "Mark Hofmann <convicted of> murder, forgery and fraud"
     "-"],
    :ethnic-group
    ["P172"
     "Item"
     "ethnic group: subject's ethnicity [consensus is that a VERY high standard of proof is needed for this field to be used. In general this means 1] the subject claims it him/herself, or 2] it is widely agreed on by scholars, or 3] is fictional and portrayed as such]."
     "Hosni Mubarak <ethnic group> Arab"
     "-"],
    :has-pet
    ["P1429"
     "Item"
     "pet: pet that a person owns"
     "Barack Obama <has pet> Bo"
     "-"],
    :feast-day
    ["P841"
     "Item"
     "saint's principal feast day"
     "Saint Patrick <feast day> March 17"
     "-"],
    :astronaut-mission
    ["P450"
     "Item"
     "spaceflight: space mission that the subject is or has been a member of [do not include future missions]"
     "Marcos Pontes <astronaut mission> Soyuz TMA-8"
     "crew member"],
    :sex-or-gender
    ["P21"
     "Item"
     "sex and gender: sexual identity of subject: male [Q6581097], female [Q6581072], intersex [Q1097630], transgender female [Q1052281], transgender male [Q2449503]. Animals: male animal [Q44148], female animal [Q43445]. Groups of same gender use 'subclass of' [P279]"
     "Confucius <sex or gender> male"
     "-"],
    :honorific-suffix
    ["P1035"
     "Item"
     "honorific: word or expression with connotations conveying esteem or respect when used, after a name, in addressing or referring to a person"
     "→ Property talk:P1035"
     "-"],
    :student-of
    ["P1066"
     "Item"
     "teacher: person who has taught this person"
     "Alexander the Great <student of> Aristotle"
     "student"],
    :pseudonym
    ["P742"
     "String"
     "pseudonym: alias used by someone or by which this person is universally known"
     "Mark Twain <pseudonym> Mark Twain"
     "-"],
    :manner-of-death
    ["P1196"
     "Item"
     "manner of death: circumstances of a person's death; one of: natural causes, accident, suicide, homicide, pending investigation or special 'unknown value'. Use 'cause of death' [P509] for more immediate or underlying causes and events, e.g. heart attack, car accident"
     "Paul Walker <manner of death> accident"
     "-"],
    :dan-kyu-rank
    ["P468"
     "Item"
     "rank system used in several board games [e.g. go, shogi, renju], martial arts [e.g. judo, kendo, wushu] and some other games"
     "Go Seigen <dan/kyu rank> 9 dan"
     "-"],
    :place-of-death
    ["P20"
     "Item"
     "place of death: the most specific known [e.g. city instead of country, or hospital instead of city]"
     "John F. Kennedy <place of death> Dallas"
     "-"],
    :country-of-citizenship
    ["P27"
     "Item"
     "citizenship: the object is a country that recognizes the subject as its citizen"
     "Nelson Mandela <country of citizenship> South Africa"
     "-"],
    :website-account-on
    ["P553"
     "Item"
     "user account: a website that the person or organization has an account on [use with P554] Note: only used with reliable source or if the person or organization disclosed it."
     "Sascha Lobo <website account on> Quora"
     "-"],
    :eye-color
    ["P1340"
     "Item"
     "eye color: color of the irises of a person's eyes"
     "Linda Evangelista <eye color> blue-green"
     "-"],
    :manager-director
    ["P1037"
     "Item"
     "person who manages any kind of group"
     "Louvre <manager/director> Jean-Luc Martinez"
     "organisation directed from the office"],
    :place-of-birth
    ["P19"
     "Item"
     "place of birth: most specific known [e.g. city instead of country, or hospital instead of city]"
     "Marie Curie <place of birth> Warsaw"
     "-"],
    :place-of-burial
    ["P119"
     "Item"
     "burial and grave: location of grave, resting place, place of ash-scattering, etc, [e.g. town/city or cemetery] for a person or animal. There may be several places: e.g. re-burials, cenotaphs, parts of body buried separately."
     "John F. Kennedy <place of burial> Arlington National Cemetery"
     "-"],
    :family-name
    ["P734"
     "Item"
     "family name: surname or last name of a person"
     "George Washington <family name> Washington"
     "-"],
    :student
    ["P802"
     "Item"
     "student: notable student[s] of a person"
     "Albert Einstein <student> Ernst G. Straus"
     "student of"],
    :doctoral-advisor
    ["P184"
     "Item"
     "doctoral advisor: person who supervised the doctorate or PhD thesis of the subject"
     "Enrico Fermi <doctoral advisor> Luigi Puccianti"
     "doctoral student"],
    :noble-title
    ["P97"
     "Item"
     "royal or noble rank: titles held by the person"
     "William Mansfield, 1st Baron Sandhurst <noble title> Baron Sandhurst"
     "-"],
    :handedness
    ["P552" "Item" "handedness of the person" "→ Property talk:P552" "-"],
    :member-of
    ["P463"
     "Item"
     "member: part of a specific organization or club. Do not use for membership in ethnic or social groups, nor for holding a position such as a member of parliament [use P39 for that]."
     "Isaac Newton <member of> Royal Society"
     "-"],
    :filmography
    ["P1283"
     "Item"
     "filmography: list of films a person has contributed to"
     "Bruce Lee <filmography> Bruce Lee filmography"
     "-"],
    :participant-of
    ["P1344"
     "Item"
     "event a person or an organization was a participant in, inverse of P710 or P1923"
     "Alberto Tomba <participant of> 1992 Winter Olympics"
     "participant and participating teams"]
    :followed-by
    ["P156"
     "Item"
     "followed by: immediately following item in some series of which the subject is part. Use P1366 [replaced by] if the item is replaced, e.g. political offices, states"
     "Rocky <followed by> Rocky II"
     "follows"],
    :name-in-kana
    ["P1814"
     "String"
     "kana: the reading of a Japanese name in kana"
     "Junichiro Koizumi <name in kana> こいずみ じゅんいちろう"
     "-"],
    :described-by-source
    ["P1343"
     "Item"
     "dictionary, encyclopaedia, etc. where this item is described"
     "New York City <described by source> 1911 Encyclopædia Britannica"
     "-"],
    :color
    ["P462" "Item" "color: color of subject" "charcoal <color> black" "-"],
    :instance-of
    ["P31"
     "Item"
     "instance of: The subject is an instance of the object item. Use more specific properties when applicable, e.g. occupation [P106] instead of 'is a <writer>' [see Help:Basic membership properties for more information]"
     "Laika <instance of> dog"
     "-"],
    :owned-by
    ["P127"
     "Item"
     "proprietor: owner of the subject"
     "Chelsea F.C. <owned by> Roman Abramovich"
     "owner of"],
    :use
    ["P366"
     "Item"
     "use: main use of the subject [includes current and former usage]"
     "Willis Tower <use> office building"
     "-"],
    :proved-by
    ["P1318"
     "Item"
     "person who proved something"
     "Poincaré conjecture <proved by> Grigori Perelman"
     "-"],
    :is-a-list-of
    ["P360"
     "Item"
     "common element between all listed items"
     "list of popes <is a list of> pope"
     "has list"],
    :time-of-discovery
    ["P575"
     "Point in time"
     "discovery: date or point in time when the item was discovered"
     "Uranus <time of discovery> March 13, 1781"
     "-"],
    :subclass-of
    ["P279"
     "Item"
     "subclass: all of this class of items are instances of some more general class of items [see Help:Basic membership properties for more information]"
     "tree <subclass of> plant"
     "-"],
    :follows
    ["P155"
     "Item"
     "follows: immediately prior item in some series of which the subject is part. Use P1365 [replaces] if the preceding item was replaced, e.g. political offices, states and there is no identity between precedent and following geographic unit"
     "1 <follows> 0"
     "followed by"],
    :part-of
    ["P361"
     "Item"
     "part: this item is a part of that item [see Help:Basic membership properties for more information]"
     "quark <part of> hadron"
     "has as part"],
    :named-after
    ["P138"
     "Item"
     "eponym: entity or event that inspired the subject's name, or namesake [in at least one language]"
     "Jules Verne <named after> Jules Verne"
     "-"],
    :different-from
    ["P1889"
     "Item"
     "difference: item that is different from another item, but they are often confused"
     "Philip A. Goodwin <different from> Philip R. Goodwin"
     "-"],
    :sourcing-circumstances
    ["P1480"
     "Item"
     "information source: qualifiers for claims: circa, misprint, presumably"
     "Muḥammad ibn Mūsā al-Khwārizmī <date of birth> 780 <sourcing circumstances> circa"
     "-"],
    :discoverer-or-inventor
    ["P61"
     "Item"
     "inventor, innovator and discoverer: discovered, first described, or invented by"
     "Enceladus <discoverer or inventor> William Herschel"
     "-"],
    :facet-of
    ["P1269"
     "Item"
     "topic of which this item is an aspect, item that offers a broader perspective on the same topic"
     "history of Madagascar <facet of> Madagascar"
     "-"],
    :patent-number
    ["P1246"
     "External identifier"
     "patent: identifier for a patented invention"
     "mouse <patent number> US3987685"
     "-"],
    :opposite-of
    ["P461"
     "Item"
     "opposite and antonym: item that is the opposite of this item"
     "black <opposite of> white"
     "-"],
    :quantity-symbol
    ["P416"
     "String"
     "quantity symbol: symbol for a physical quantity"
     "electric charge <quantity symbol> "],
    :shape
    ["P1419"
     "Item"
     "shape: geometric shape of an object"
     "pyramid <shape> pyramid"
     "-"],
    :Unicode-character
    ["P487"
     "String"
     "Unicode symbols: Unicode character representing the item"
     "euro sign <Unicode character> €"
     "-"],
    :applies-to-part
    ["P518"
     "Item"
     "part of the item for which the claim is valid"
     "Belly Amphora by the Andokides Painter <creator> Andokides painter <applies to part> red-figure pottery"
     "-"],
    :archives-at
    ["P485"
     "Item"
     "the institution holding the subject's archives"
     "Johann Sebastian Bach <archives at> Bach-Archiv Leipzig"
     "-"],
    :sRGB-color-hex-triplet
    ["P465"
     "String"
     "sRGB: sRGB hex triplet format for subject color [e.g. 7FFFD4] specifying the 8bit red, green and blue components"
     "aquamarine <sRGB color hex triplet> 7FFFD4"
     "-"],
    :approved-by
    ["P790"
     "Item"
     "item is approved by other item[s] [qualifier: statement is approved by other item[s]]"
     "GNU General Public License <approved by> Open Source Initiative"
     "-"],
    :said-to-be-the-same-as
    ["P460"
     "Item"
     "Wikimedia duplicated page: this item is said to be the same as that item, but the statement is disputed"
     "Maidilibala <said to be the same as> Uskhal Khan Tögüs Temür"
     "-"],
    :categorys-main-topic
    ["P301"
     "Item"
     "primary topic of the subject Wikimedia category"
     "Category:Germany <category's main topic> Germany"
     "topic's main category"],
    :topics-main-category
    ["P910"
     "Item"
     "Wikimedia category: main category"
     "Asia <topic's main category> Category:Asia"
     "category's main topic"],
    :has-as-part
    ["P527"
     "Item"
     "has part: part of this subject, also valuable subclass or exemplar. Inverse property of 'part of' [P361]."
     "Adam and Eve <has as part> Adam"
     "part of and does not have part"]
    :continent
    ["P30"
     "Item"
     "continent: continent of which the subject is a part. Use for countries, locations in Antarctica, and items located in countries that belong to more than one continent, e.g. Russian cities."
     "India <continent> Asia"
     "-"],
    :coordinate-of-southernmost-point
    ["P1333"
     "Geographic coordinates"
     "southernmost point of a place. For administrative entities this includes offshore islands"
     "Columbia County <coordinate of southernmost point> 40°46'29.3592'N, 76°22'47.1252'W"
     "coordinate of northernmost point"],
    :income-classification-Philippines
    ["P1879"
     "Item"
     "income class: classification grade of a Philippine local government unit based on income"
     "Eastern Samar <income classification [Philippines]> 2nd provincial income class"
     "-"],
    :located-in-time-zone
    ["P421"
     "Item"
     "time zone: time zone for this item"
     "Cameroon <located in time zone> West Africa Time"
     "-"],
    :coordinate-of-easternmost-point
    ["P1334"
     "Geographic coordinates"
     "easternmost point of a location"
     "Columbia County <coordinate of easternmost point> 40°56'58.4376'N, 76°12'27.9072'W"
     "coordinate of westernmost point"],
    :population
    ["P1082"
     "Number"
     "population size: number of people inhabiting the place; number of people of subject"
     "San Francisco <population> 837,442"
     "-"],
    :coordinate-of-westernmost-point
    ["P1335"
     "Geographic coordinates"
     "westernmost point of a location"
     "Columbia County <coordinate of westernmost point> 41°9'17.5536'N, 76°38'22.7472'W"
     "coordinate of easternmost point"],
    :located-on-terrain-feature
    ["P706"
     "Item"
     "landform: located on the specified landform or body of water. Should not be used when the value is only political/administrative [provinces, states, countries, etc.]."
     "Oahu <located on terrain feature> Pacific Ocean"
     "-"],
    :direction-relative-to-location
    ["P654"
     "Item"
     "cardinal direction: qualifier for geographical locations to express relative direction"
     "Hennessey <direction relative to location> north"
     "-"],
    :coordinate-of-northernmost-point
    ["P1332"
     "Geographic coordinates"
     "northernmost point of a location. For an administrative entity this includes offshore islands"
     "Columbia County <coordinate of northernmost point> 41°18'29.682'N, 76°18'39.9312'W"
     "coordinate of southernmost point"],
    :country
    ["P17"
     "Item"
     "country and former country: sovereign state of this item"
     "Prince Edward Island <country> Canada"
     "-"],
    :place-name-sign
    ["P1766"
     "Commons media file"
     "town sign: image of road sign with place name on it"
     "Old Appleton <place name sign> Old Appleton, Missouri, Road sign.jpg"
     "-"],
    :located-on-street
    ["P669"
     "Item"
     "street: street, road, or square, where the item is located. To add the number, use Property:P670 'street number' as qualifier"
     "De Utrecht <located on street> Beurs van Berlage"
     "-"]
    :head-of-government
    ["P6"
     "Item"
     "head of government: head of the executive power of a town, city, municipality, state, country, or other governmental body"
     "Riga <head of government> Nils Ušakovs"
     "-"],
    :anthem
    ["P85"
     "Item"
     "anthem: name of the subject's anthem"
     "Canada <anthem> O Canada"
     "-"],
    :shares-border-with
    ["P47"
     "Item"
     "political border and border: countries or administrative subdivisions that this item borders, of similar administrative rank [e.g. countries share borders with other countries]"
     "People's Republic of China <shares border with> Mongolia"
     "-"],
    :basic-form-of-government
    ["P122"
     "Item"
     "form of government: the subject's government"
     "Mexico <basic form of government> republic"
     "-"],
    :capital
    ["P36"
     "Item"
     "capital: location [city, municipality] of governmental seat of the country, or administrative territorial entity"
     "United States of America <capital> Washington, D.C."
     "capital of"],
    :exclave-of
    ["P500"
     "Item"
     "territory is legally or politically attached to a main territory with which it is not physically contiguous because of surrounding alien territory. It may also be an enclave."
     "Ceuta <exclave of> Spain"
     "-"],
    :official-language
    ["P37"
     "Item"
     "official language: language designated as official by this item"
     "Russia <official language> Russian"
     "-"],
    :legislative-body
    ["P194"
     "Item"
     "legislature: legislative body governing this entity; political institution with elected representatives, such as a parliament/legislature or council"
     "Israel <legislative body> Knesset"
     "-"],
    :currency
    ["P38"
     "Item"
     "currency: currency used by item"
     "Mongolia <currency> Mongolian tögrög"
     "-"],
    :territory-claimed-by
    ["P1336"
     "Item"
     "administrative divisions that claim control of a given area"
     "Senkaku Islands <territory claimed by> Taiwan"
     "-"],
    :party-chief-representative
    ["P210"
     "Item"
     "chief representative of a party in an institution or an administrative unit [use qualifier to identify the party]"
     "Beijing <party chief representative> Guo Jinlong"
     "-"],
    :top-level-internet-domain
    ["P78"
     "Item"
     "top-level domain: Internet domain name system top-level code"
     "Honduras <top-level internet domain> .hn"
     "-"],
    :applies-to-territorial-jurisdiction
    ["P1001"
     "Item"
     "the item [an institution, law, public office ...] belongs to or applies to the value [a territorial jurisdiction: a country, state, municipality, ...]"
     "California Department of Transportation <applies to territorial jurisdiction> California"
     "-"],
    :enclave-within
    ["P501"
     "Item"
     "territory is entirely surrounded by the other [enclaved]"
     "Campione d'Italia <enclave within> Ticino"
     "-"],
    :head-of-state
    ["P35"
     "Item"
     "head of state: official with the highest formal authority in a country"
     "Germany <head of state> Joachim Gauck"
     "-"],
    :sister-city
    ["P190"
     "Item"
     "twin town: twin towns, sister cities, twinned municipalities and other localities that have a partnership or cooperative agreement, either legally or informally acknowledged by their governments"
     "Shanghai <sister city> Yokohama"
     "-"],
    :contains-settlement
    ["P1383"
     "Item"
     "human settlement: settlement which an administrative division contains"
     "Surdila-Greci <contains settlement> Brateșu Vechi"
     "131"],
    :located-in-the-administrative-territorial-entity
    ["P131"
     "Item"
     "administrative territorial entity: the item is located on the territory of the following administrative entity. Use P276 [location] for specifying the location of non-administrative places and for items about events"
     "Cambridge <located in the administrative territorial entity> Middlesex County"
     "150"],
    :contains-administrative-territorial-entity
    ["P150"
     "Item"
     "[list of] direct subdivisions of an administrative territorial entity"
     "Příbram District <contains administrative territorial entity> Dobříš"
     "131"]})
