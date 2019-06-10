(ns clj-icu4j.message
  (:refer-clojure :exclude [format])
  (:require [clojure.string :as string])
  (:import com.ibm.icu.text.MessageFormat))

(defn selection-pattern-1-arg
  "Return the String pattern for MessageFormat when a selection options map is provided"
  [selection-map]
  (let [{:keys [select-args select-cases]} selection-map]
    (assert (= 1 (count select-args)) "More args than I can currently handle (but I can make myself recursive to handle N=2+ args, of course)")
    (let [first-arg (first select-args)
          cases-str (->> (for [[k v] select-cases]
                           (let [scalar-case-val (if (sequential? k) (first k) k)]
                             (str scalar-case-val " {" v "}")))
                         (string/join " "))
          inner-str (if (<= (count first-arg) 2)
                      (let [arg-name (name (first first-arg))
                            arg-type (name (second first-arg))
                            pattern-parts [arg-name arg-type cases-str]
                            comma-sep-parts-str (string/join ", " pattern-parts)]
                        comma-sep-parts-str)
                      (let [comma-sep-parts-str (string/join ", " (map name first-arg))
                            pattern-parts [comma-sep-parts-str
                                           cases-str]]
                        (string/join "\n" pattern-parts)))
          pattern-str (str "{" inner-str "}")]
      pattern-str)))

(defn selection-pattern-multi-arg
  "Same as selection-pattern-1-arg, but allow for multiple args.
  The selection cases map should be keyed by vectors of case values in the same order of the selection args that they represent."
  [selection-map]
  (let [select-args (:select-args selection-map)
        select-cases (:select-cases selection-map)]
    (if (= 1 (count select-args))
      (selection-pattern-1-arg selection-map)
      (let [first-arg (first select-args)
            cases-by-first-arg-val (->> (keys select-cases)
                                        (group-by first))
            case-sub-patterns (for [[first-arg-val matching-cases] cases-by-first-arg-val]
                                (let [new-select-args (rest select-args)
                                      new-select-cases (into {} (for [case matching-cases]
                                                                  (let [new-case (rest case)
                                                                        pattern (get select-cases case)]
                                                                    [new-case pattern])))
                                      new-selection-map (assoc selection-map
                                                               :select-args new-select-args
                                                               :select-cases new-select-cases)
                                      ;; recursive call is here!
                                      sub-pattern (selection-pattern-multi-arg new-selection-map)]
                                  (str  first-arg-val " {" sub-pattern "}")))
            case-sub-patterns-str (string/join "\n" case-sub-patterns)
            first-arg-parts (concat (map name first-arg)
                                    [case-sub-patterns-str]) 
            inner-str (if (<= (count first-arg) 2)
                      (let [arg-name (name (first first-arg))
                            arg-type (name (second first-arg))
                            pattern-parts [arg-name arg-type case-sub-patterns-str]
                            comma-sep-parts-str (string/join ", " pattern-parts)]
                        comma-sep-parts-str)
                      (let [comma-sep-parts-str (string/join ", " (map name first-arg))
                            pattern-parts [comma-sep-parts-str
                                           case-sub-patterns-str]]
                        (string/join "\n" pattern-parts))) 
            first-arg-pattern (str "{" (string/join ", " first-arg-parts) "}")]
        first-arg-pattern))))

(defn ^MessageFormat formatter
  "Instantiate a MessageFormat object by passing a String pattern or a selection options map."
  [pattern-or-selection]
  (if (string? pattern-or-selection)
    (let [pattern pattern-or-selection]
      (MessageFormat. pattern))
    (do
      (assert (map? pattern-or-selection) "You must pass either a String pattern or a selection map")
      (let [selection-map pattern-or-selection
            {:keys [locale select-args select-cases]} selection-map
            pattern (selection-pattern-multi-arg selection-map)]
        (MessageFormat. pattern locale)))))

(defn ^String format
  "Provide a localized interpolated string based on the MessageFormat object and the specific input values provided.
Provide a sequential argument if the formatter pattern used numerical arguments. Else, provide a map for a selection pattern / named arugments."
  [fmtr args]
  (if (sequential? args)
    (.format fmtr (to-array args))
    (do
      (assert (map? args) "You must pass either a sequential collection of values or a map of arg names and values.")
      (let [{:keys []} args
            named-args-map args
            string-key-args-map (into {}
                                      (for [[k v] named-args-map]
                                        (let [arg-name (-> (name k))]
                                          [arg-name v])))]
        (.format fmtr string-key-args-map)))))
