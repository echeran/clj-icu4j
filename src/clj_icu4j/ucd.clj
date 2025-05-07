(ns clj-icu4j.ucd
  (:require [clojure.core.logic.fd :as fd]
            [clojure.string :as string]))

(defrecord CodePoint [^int cp]
  Object
  (toString [_] (let [hex-str (Integer/toHexString cp)]
                  (if (< (count hex-str) 4)
                    (-> (format "%4s" hex-str)
                        (string/replace #" " "0"))
                    hex-str))))

(defrecord CodePointRange [^int start ^int end]
  Object
  (toString [_] (str (CodePoint. start) ".." (CodePoint. end))))

(defmethod print-method CodePoint [this, ^java.io.Writer w]
  (.write w (str this)))

(defmethod print-method CodePointRange [this, ^java.io.Writer w]
  (.write w (str this)))

(defn parse-code-point
  "Parse a code point hexadecimal into a decimal number"
  [s]
  (CodePoint. (Integer/parseInt s 16)))

(defn parse-code-point-or-range
  "Parse a Unicode data file code-point-or-range string into an interval"
  [s]
  (let [[start end] (-> s
                        (string/split #"\.\.")
                        (map parse-code-point))]
    (CodePointRange. start end)))
