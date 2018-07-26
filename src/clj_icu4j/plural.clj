(ns clj-icu4j.plural
  (:require [clj-icu4j.locale :as locale]
            [clojure.set :as set])
  (:import [com.ibm.icu.text PluralRules PluralRules$PluralType]))

(def PLURAL-TYPES {::cardinal PluralRules$PluralType/CARDINAL
                   ::ordinal PluralRules$PluralType/ORDINAL})

(defn plural-rules-for-plural-type
  [locale plural-type]
  (PluralRules/forLocale locale plural-type))

(defn plural-type-display-name
  [plural-type]
  (let [plural-type-to-keywords (set/map-invert PLURAL-TYPES)
        plural-type-keyword (get plural-type-to-keywords plural-type)
        plural-type-name (name plural-type-keyword)]
    plural-type-name))
