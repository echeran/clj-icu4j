(ns clj-icu4j.locale
  (:import com.ibm.icu.util.ULocale))

(defn get-canonical-locale
  [^String locale-id-str]
  (ULocale/createCanonical locale-id-str))
