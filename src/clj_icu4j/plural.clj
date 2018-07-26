(ns clj-icu4j.plural
  (:require [clj-icu4j.locale :as locale])
  (:import [com.ibm.icu.text PluralRules PluralRules$PluralType]))

(def PLURAL-TYPES {::cardinal PluralRules$PluralType/CARDINAL
                   ::ordinal PluralRules$PluralType/ORDINAL})

(defn plural-rules-for-plural-type
  [locale plural-type]
  (PluralRules/forLocale locale plural-type))
