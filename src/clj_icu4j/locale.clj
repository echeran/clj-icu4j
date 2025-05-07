(ns clj-icu4j.locale
  (:import com.ibm.icu.util.ULocale))

(defn get-locale
  [l]
  (if (string? l)
    (ULocale/forLanguageTag l)
    l))

(defn loc->old-cldr-str
  [loc]
  (str loc))

(defn loc->str
  [loc]
  (.toLanguageTag loc))
