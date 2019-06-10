(ns clj-icu4j.message-test
  (:refer-clojure :exclude [format])
  (:require [clojure.test :refer :all]
            [clj-icu4j.message :as msg])
  (:import com.ibm.icu.util.ULocale))

;; Test data for the following tests are adapted from preamble of
;; Javadoc entry for MessageFormat:
;; http://icu-project.org/apiref/icu4j/com/ibm/icu/text/MessageFormat.html

(deftest string-pattern-sequential-vals
  (let [fmtr (msg/formatter "The disk \"{1}\" contains {0} file(s).")]
    (is (= "The disk \"MyDisk\" contains 3 file(s)."
           (msg/format fmtr [3 "MyDisk"])))
    (is (= "The disk \"MyDisk\" contains 1 file(s)."
           (msg/format fmtr [1 "MyDisk"])))
    (is (= "The disk \"MyDisk\" contains 1,273 file(s)."
           (msg/format fmtr [1273 "MyDisk"])))))
  
(deftest string-pattern-map-vals
  (let [fmtr (msg/formatter "The disk \"{disk_name}\" contains {num_files} file(s).")]
    (is (= "The disk \"MyDisk\" contains 3 file(s)."
           (msg/format fmtr {"disk_name" "MyDisk"
                             "num_files" 3})))
    (is (= "The disk \"MyDisk\" contains 1 file(s)."
           (msg/format fmtr {"disk_name" "MyDisk"
                             "num_files" 1})))
    (is (= "The disk \"MyDisk\" contains 1,273 file(s)."
           (msg/format fmtr {"disk_name" "MyDisk"
                             "num_files" 1273})))))

(deftest selection-format-map-vals-1-arg
  (let [fmtr (msg/formatter {:select-args [["num_files" :plural]]
                             :select-cases {"=0"    "There are no files on disk \"{disk_name}\"."
                                            "=1"    "There is one file on disk \"{disk_name}\"."
                                            "other" "There are # files on disk \"{disk_name}\"."}
                             :locale ULocale/ENGLISH})
        args-map-1 {"num_files" 0
                    "disk_name" "MyDisk"}]
    (testing "use the args map from the Javadoc examples"
      (is (= "There are no files on disk \"MyDisk\"."
             (msg/format fmtr args-map-1))))
    (testing "updating the args map based on Javadoc examples"
      (let [args-map-2 (assoc args-map-1 "num_files" 3)]
        (is (= "There are 3 files on disk \"MyDisk\"."
           (msg/format fmtr args-map-2)))))))

;; The following test data comes from
;; http://userguide.icu-project.org/formatparse/messages

(deftest selection-format-map-vals-multi-arg-v1
  (testing "version 1 of simplifying multi-arg selection syntax"
    (let [fmtr (msg/formatter {:select-args [["gender_of_host" "select"]
                                             ["num_guests" "plural" "offset:1"]]
                               :select-cases {["female" "=0"] "{host} does not give a party."
                                              ["female" "=1"] "{host} invites {guest} to her party."
                                              ["female" "=2"] "{host} invites {guest} and one other person to her party."
                                              ["female" "other"] "{host} invites {guest} and # other people to her party."
                                              ["male" "=0"] "{host} does not give a party."
                                              ["male" "=1"] "{host} invites {guest} to his party."
                                              ["male" "=2"] "{host} invites {guest} and one other person to his party."
                                              ["male" "other"] "{host} invites {guest} and # other people to his party."
                                              ["other" "=0"] "{host} does not give a party."
                                              ["other" "=1"] "{host} invites {guest} to their party."
                                              ["other" "=2"] "{host} invites {guest} and one other person to their party."
                                              ["other" "other"] "{host} invites {guest} and # other people to their party."}
                               :locale ULocale/ENGLISH})]
      (let [args-map {"gender_of_host" "male"
                      "host" "MC Hammer"
                      "num_guests" 0
                      ;;"guest" "DJ Jazzy Jeff"
                      }]
        (is (= "MC Hammer does not give a party."
               (msg/format fmtr args-map))))
      (let [args-map {"gender_of_host" "male"
                      "host" "MC Hammer"
                      "num_guests" 2
                      "guest" "DJ Jazzy Jeff"
                      }]
        (is (= "MC Hammer invites DJ Jazzy Jeff and one other person to his party."
               (msg/format fmtr args-map))))
      (let [args-map {"gender_of_host" "female"
                      "host" "Paula Poundstone"
                      "num_guests" 1
                      "guest" "Michelle Wolf"}]
        (is (= "Paula Poundstone invites Michelle Wolf to her party."
               (msg/format fmtr args-map))))
      (let [args-map {"gender_of_host" "other"
                      "host" "The Fry Guys"
                      "num_guests" 8
                      "guest" "Ronald McDonald"}]
        (is (= "The Fry Guys invites Ronald McDonald and 7 other people to their party."
               (msg/format fmtr args-map)))))))

(deftest selection-format-map-vals-multi-arg-v2
  (testing "version 2 of simplifying multi-arg selection syntax"
    (let [gender-possessive-pronouns {"female" "her"
                                      "male" "his"
                                      "other" "their"}
          gender-cases-submap-fn (fn [gender]
                                   (let [possessive-pronoun (get gender-possessive-pronouns gender)
                                         submap {[gender "=0"] "{host} does not give a party."
                                                 [gender "=1"] (str "{host} invites {guest} to " possessive-pronoun  " party.")
                                                 [gender "=2"] (str "{host} invites {guest} and one other person to " possessive-pronoun  " party.")
                                                 [gender "other"] (str "{host} invites {guest} and # other people to " possessive-pronoun " party.")}]
                                     submap))
          gender-cases-submaps (for [gender (keys gender-possessive-pronouns)]
                                 (gender-cases-submap-fn gender))
          select-cases-map (into {} gender-cases-submaps)
          fmtr (msg/formatter {:select-args [["gender_of_host" "select"]
                                             ["num_guests" "plural" "offset:1"]]
                               :select-cases select-cases-map
                               :locale ULocale/ENGLISH})]
      (let [args-map {"gender_of_host" "male"
                      "host" "MC Hammer"
                      "num_guests" 0
                      ;;"guest" "DJ Jazzy Jeff"
                      }]
        (is (= "MC Hammer does not give a party."
               (msg/format fmtr args-map))))
      (let [args-map {"gender_of_host" "male"
                      "host" "MC Hammer"
                      "num_guests" 2
                      "guest" "DJ Jazzy Jeff"
                      }]
        (is (= "MC Hammer invites DJ Jazzy Jeff and one other person to his party."
               (msg/format fmtr args-map))))
      (let [args-map {"gender_of_host" "female"
                      "host" "Paula Poundstone"
                      "num_guests" 1
                      "guest" "Michelle Wolf"}]
        (is (= "Paula Poundstone invites Michelle Wolf to her party."
               (msg/format fmtr args-map))))
      (let [args-map {"gender_of_host" "other"
                      "host" "The Fry Guys"
                      "num_guests" 8
                      "guest" "Ronald McDonald"}]
        (is (= "The Fry Guys invites Ronald McDonald and 7 other people to their party."
               (msg/format fmtr args-map)))))))

(deftest selection-format-map-vals-multi-arg-v3
  (testing "version 3 of simplifying multi-arg selection syntax"
    (let [gender-possessive-pronouns {"female" "her"
                                      "male" "his"
                                      "other" "their"}
          gender-cases-submap-fn (fn [gender]
                                   (let [possessive-pronoun (get gender-possessive-pronouns gender)
                                         submap {[gender "=0"] "{host} does not give a party."
                                                 [gender "=1"] (str "{host} invites {guest} to " possessive-pronoun  " party.")
                                                 [gender "=2"] (str "{host} invites {guest} and one other person to " possessive-pronoun  " party.")
                                                 [gender "other"] (str "{host} invites {guest} and # other people to " possessive-pronoun " party.")}]
                                     submap))
          select-cases-map (into {} (->> (keys gender-possessive-pronouns)
                                         (map gender-cases-submap-fn)))
          fmtr (msg/formatter {:select-args [["gender_of_host" "select"]
                                             ["num_guests" "plural" "offset:1"]]
                               :select-cases select-cases-map
                               :locale ULocale/ENGLISH})]
      (let [args-map {"gender_of_host" "male"
                      "host" "MC Hammer"
                      "num_guests" 0
                      ;;"guest" "DJ Jazzy Jeff"
                      }]
        (is (= "MC Hammer does not give a party."
               (msg/format fmtr args-map))))
      (let [args-map {"gender_of_host" "male"
                      "host" "MC Hammer"
                      "num_guests" 2
                      "guest" "DJ Jazzy Jeff"
                      }]
        (is (= "MC Hammer invites DJ Jazzy Jeff and one other person to his party."
               (msg/format fmtr args-map))))
      (let [args-map {"gender_of_host" "female"
                      "host" "Paula Poundstone"
                      "num_guests" 1
                      "guest" "Michelle Wolf"}]
        (is (= "Paula Poundstone invites Michelle Wolf to her party."
               (msg/format fmtr args-map))))
      (let [args-map {"gender_of_host" "other"
                      "host" "The Fry Guys"
                      "num_guests" 8
                      "guest" "Ronald McDonald"}]
        (is (= "The Fry Guys invites Ronald McDonald and 7 other people to their party."
               (msg/format fmtr args-map)))))))
