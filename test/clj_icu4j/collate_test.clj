(ns clj-icu4j.collate-test
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
           (map (partial apply collator-compare l1) compare-pairs)))
    (is (= [-1 -1 -1 0 -1 0]
           (map (partial apply collator-compare l2) compare-pairs)))
    (is (= [-1 -1 -1 -1 -1 -1]
           (map (partial apply collator-compare l3) compare-pairs)))))

(deftest locale-test
  ;; Swedish starts with a, z is "last", and then
  ;; puts a+diacritics after z.
  ;; Using German words just to illustrate a+diacritic
  (let [sv (collator {:locale "sv"})]
    (is (= -1 (collator-compare sv "apfel" "zukunft")))
    (is (= -1 (collator-compare sv "zukunft" "äpfel")))))

(deftest comparator-test
  (let [expected ["பட்டம்" "படம்" "படி" "படு" "பண்டம்" "பத்து" "பந்து"]
        input [(nth expected 4)
               (nth expected 1)
               (nth expected 3)
               (nth expected 0)
               (nth expected 2)
               (nth expected 6)
               (nth expected 5)]
        coll-ta (collator {:locale "ta"})
        coll-ta-comp (collator->comparator coll-ta)]
    (is (= expected (sort coll-ta-comp input)))))
