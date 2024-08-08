(ns clj-icu4j.collate
  (:require [clj-icu4j.locale :as l]
            [clojure.string :as string])
  (:import [com.ibm.icu.text Collator RuleBasedCollator])
  (:refer-clojure :exclude [compare]))

(def strengths
  {:primary Collator/PRIMARY
   :PRIMARY Collator/PRIMARY
   :secondary Collator/SECONDARY
   :SECONDARY Collator/SECONDARY
   :tertiary Collator/TERTIARY
   :TERTIARY Collator/TERTIARY
   :quarternary Collator/QUATERNARY
   :QUARTERNARY Collator/QUATERNARY})

(defn collator
"Instantiates a new frozen collator.
 Locale provided in opts map as key :locale according to inputs handled
 by `locale/get-locale`."
  [{:keys [locale rules strength case-first case-level] :as opts}]
  (let [loc (when locale
              (l/get-locale locale))
        ;; Call the constructor for the collator, which can take a locale
        ;; or custom rules. The API docs alow show an example of adding
        ;; custom rules on top of the default rules of a locale, so try to
        ;; support that behavior a locale and a custom rule string are specified.
        col (if loc
              (let [col (RuleBasedCollator/getInstance loc)]
                (if-not rules
                  col
                  (let [old-rules (.getRules col)
                        new-rules (str old-rules rules)]
                    (new RuleBasedCollator new-rules))))
              (if rules
                (new RuleBasedCollator rules)
                (RuleBasedCollator/getInstance)))]
    (when strength
      (.setStrength col (get strengths strength)))
    (case case-first
      (:upper :uppercase) (.setUpperCaseFirst col true)
      (:lower :lowercase) (.setLowerCaseFirst col true)
      :do-nothing)
    (when case-level
      (.setCaseLevel col (boolean case-level)))
    ;; return the immutable version ("frozen") of the collator
    (.freeze col)))

(defn compare
  [col s1 s2]
  (.compare col s1 s2))
