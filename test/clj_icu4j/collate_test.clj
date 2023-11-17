(ns clj-icu4j.collate-test
  (:refer-clojure :exclude [compare])
  (:require [clj-icu4j.collate :refer :all]
            [clojure.test :refer [deftest is]]))

(deftest strength-test
  (let [compare-pairs [["a" "b"]
                       ["as" "às"]
                       ["às" "at"]
                       ["ao" "Ao"]
                       ["Ao" "aò"]
                       ["A" "Ⓐ"]]
        l1 (collator {:strength :primary})
        l2 (collator {:strength :secondary})
        l3 (collator {:strength :tertiary})]
    (is (= [ -1 0 -1 0 0 0]
           (map (partial apply compare l1) compare-pairs)))
    (is (= [-1 -1 -1 0 -1 0]
           (map (partial apply compare l2) compare-pairs)))
    (is (= [-1 -1 -1 -1 -1 -1]
           (map (partial apply compare l3) compare-pairs)))))

(deftest locale-test
  ;; Swedish starts with a, z is "last", and then
  ;; puts a+diacritics after z.
  ;; Using German words just to illustrate a+diacritic
  (let [sv (collator {:locale "sv"})]
    (is (= -1 (compare sv "apfel" "zukunft")))
    (is (= -1 (compare sv "zukunft" "äpfel")))))