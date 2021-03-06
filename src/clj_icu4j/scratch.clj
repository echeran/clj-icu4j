(ns clj-icu4j.scratch
  (:require [clj-icu4j.locale :as locale]
            [clj-icu4j.plural :as plural]))

;; This ns is for playing around.  Aka "playground" in Java
;; projects.

(defn get-plural-keywords
  "Given a Locale and PluralType, get the plural rules"
  [locale plural-type]
  (let [plural-rules (plural/plural-rules-for-plural-type locale plural-type)
        keywords (.getKeywords plural-rules)]
    keywords))

(defn print-plural-keywords
  "Given a Locale and PluralType, get the plural rules, and print out the locale, PluralType, and all of the rule's keywords"
  [locale plural-type]
  (let [keywords (get-plural-keywords locale plural-type)]
    (println (format "Locale: %s (%s), plural type: %s"
                     (.getDisplayName locale)
                     (.toLanguageTag locale)
                     (plural/plural-type-display-name plural-type)))
    (doseq [k keywords]
      (println "\t" k))))

(defn print-all-plural-keywords
  [^String locale-id]
  (let [locale (locale/get-canonical-locale locale-id)]
    (doseq [plural-type (vals plural/PLURAL-TYPES)]
      (print-plural-keywords locale plural-type))))
