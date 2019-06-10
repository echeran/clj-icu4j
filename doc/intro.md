# Introduction to clj-icu4j

## MessageFormat Examples

```clj
(require '[clj-icu4j.message :as msg])
```

Many of the following examples come from the Javadoc of `MessageFormat` (http://icu-project.org/apiref/icu4j/com/ibm/icu/text/MessageFormat.html) or "Formatting Messages" documentation (http://userguide.icu-project.org/formatparse/messages).

Let's start with a simple `MessageFormat` pattern using numerical/positional placeholders in the pattern.

```clj
(def fmtr (msg/formatter "The disk \"{1}\" contains {0} file(s)."))

(msg/format fmtr [3 "MyDisk"])
;=>
;"The disk \"MyDisk\" contains 3 file(s)."

(msg/format fmtr [1 "MyDisk"])
;=>
;"The disk \"MyDisk\" contains 1 file(s)."

;; Observe the number formatting

(msg/format fmtr [1273 "MyDisk"])
;=>
;"The disk \"MyDisk\" contains 1,273 file(s)."
```

Here are named arguments/placeholders in the pattern.  After creating the formatter object, the values provided to the formatter for interpolation are given in a map.

```clj
(def fmtr (msg/formatter "The disk \"{disk_name}\" contains {num_files} file(s)."))

(msg/format fmtr {"disk_name" "MyDisk"
                  "num_files" 3})
;=>
;"The disk \"MyDisk\" contains 3 file(s)."

(msg/format fmtr {"disk_name" "MyDisk"
                  "num_files" 1})
;=>
;"The disk \"MyDisk\" contains 1 file(s)."

(msg/format fmtr {"disk_name" "MyDisk"
                  "num_files" 1273})
;=>
;"The disk \"MyDisk\" contains 1,273 file(s)."
```

Formatters that select a pattern from a range of possible patterns depending on the values of provided arguments will require a little more information to create the formatter.  The arguments whose values are used for selection will be provided in a list.  The possible pattern strings for the formatter are provided in a map that is keyed by the values of the selection arguments.  A locale is also required as it may disambiguate the semantics of the selection criteria (ex: plural categories).

An example of a formatter for a scenario with one selection argument (`num_files`):

```clj
(import 'com.ibm.icu.util.ULocale)

(def fmtr (msg/formatter {:select-args [["num_files" :plural]]
                          :select-cases {"=0"    "There are no files on disk \"{disk_name}\"."
                                         "=1"    "There is one file on disk \"{disk_name}\"."
                                         "other" "There are # files on disk \"{disk_name}\"."}
                          :locale ULocale/ENGLISH}))

(msg/format fmtr {"disk_name" "MyDisk"
                  "num_files" 0})
;=>
;"There are no files on disk \"MyDisk\"."

(msg/format fmtr {"disk_name" "MyDisk"
                  "num_files" 3})
;=>
;"There are 3 files on disk \"MyDisk\"."
```

When multiple selection arguments are present, the map of possible pattern strings are keyed by vectors of possible combinations of values for the selection arguments.

```clj
(def fmtr (msg/formatter {:select-args [["gender_of_host" "select"]
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
                          :locale ULocale/ENGLISH}))

(msg/format fmtr {"gender_of_host" "male"
                  "host" "MC Hammer"
                  "num_guests" 0})
;=>
;"MC Hammer does not give a party."

(msg/format fmtr {"gender_of_host" "male"
                  "host" "MC Hammer"
                  "num_guests" 2
                  "guest" "DJ Jazzy Jeff"})
;=>
;"MC Hammer invites DJ Jazzy Jeff and one other person to his party."

(msg/format fmtr {"gender_of_host" "female"
                  "host" "Paula Poundstone"
                  "num_guests" 1
                  "guest" "Michelle Wolf"})
;=>
;"Paula Poundstone invites Michelle Wolf to her party."

(msg/format fmtr {"gender_of_host" "other"
                  "host" "The Fry Guys"
                  "num_guests" 8
                  "guest" "Ronald McDonald"})
;=>
;"The Fry Guys invites Ronald McDonald and 7 other people to their party."
```

Of course, there are ways in which the creation of the selection cases map can be simplified:

```clj
(def gender-possessive-pronouns {"female" "her"
                                 "male" "his"
                                 "other" "their"})

(defn gender-cases-submap
  [gender]
  (let [possessive-pronoun (get gender-possessive-pronouns gender)
        submap {[gender "=0"] "{host} does not give a party."
                [gender "=1"] (str "{host} invites {guest} to " possessive-pronoun  " party.")
                [gender "=2"] (str "{host} invites {guest} and one other person to " possessive-pronoun  " party.")
                [gender "other"] (str "{host} invites {guest} and # other people to " possessive-pronoun " party.")}]
  submap))

(def select-cases-map (into {} (->> (keys gender-possessive-pronouns)
                                    (map gender-cases-submap))))

(def fmtr (msg/formatter {:select-args [["gender_of_host" "select"]
                                        ["num_guests" "plural" "offset:1"]]
                          :select-cases select-cases-map
                          :locale ULocale/ENGLISH}))
```

## BreakIterator Examples

``` clj
(require '[clojure.string :as string])
(require '[clojure.pprint :as pprint])
(require '[clj-icu4j.segment :as seg])

(def s " This is a sentence. This is another sentence. இது ஒரு வாக்கியம்.  இது அடுத்த வாக்கியம்.  எப்படி இருக்கிறது? ")
```

Let's start off with pretty-printing.  Useful when we get to nested seqs / data structures.
``` clj
;; Note: clj-icu4j.core/line-seq is very different than clojure.core/line-seq

(->> (seg/line-seq s) 
     pprint/pprint)
;=>
;(" "
; "This "
; "is "
; "a "
; "sentence. "
; "This "
; "is "
; "another "
; "sentence. "
; "இது "
; "ஒரு "
; "வாக்கியம்.  "
; "இது "
; "அடுத்த "
; "வாக்கியம்.  "
; "எப்படி "
; "இருக்கிறது? ")
```

Using Clojure's core library on seqs
``` clj
(->> (seg/line-seq s)
     (drop 9)
     pprint/pprint)
;=>
;("இது "
; "ஒரு "
; "வாக்கியம்.  "
; "இது "
; "அடுத்த "
; "வாக்கியம்.  "
; "எப்படி "
; "இருக்கிறது? ")
```

Let's get rid of the extra whitespace after each segment.

``` clj
(require '[clojure.string :as string])

(->> (seg/line-seq s)
     (drop 9)
     (map string/trim)
     pprint/pprint)
;=>
;("இது"
; "ஒரு"
; "வாக்கியம்."
; "இது"
; "அடுத்த"
; "வாக்கியம்."
; "எப்படி"
; "இருக்கிறது?")
```

After we convert the string into a seq of 'line' segments, we map the char-seq on the resulting 'line' segments in the seq.

``` clj
(->> (seg/line-seq s)
     (drop 9)
     (map string/trim)
     (map seg/char-seq)
     pprint/pprint)
;=>
;(("இ" "து")
; ("ஒ" "ரு")
; ("வா" "க்" "கி" "ய" "ம்" ".")
; ("இ" "து")
; ("அ" "டு" "த்" "த")
; ("வா" "க்" "கி" "ய" "ம்" ".")
; ("எ" "ப்" "ப" "டி")
; ("இ" "ரு" "க்" "கி" "ற" "து" "?"))
```



``` clj
(->> (seg/line-seq s)
     (map seg/word-seq)
     (remove nil?)
     (map (fn [word-seq] (map seg/char-seq word-seq)))
     pprint/pprint)
;=>
;((("T" "h" "i" "s"))
; (("i" "s"))
; (("a"))
; (("s" "e" "n" "t" "e" "n" "c" "e"))
; (("T" "h" "i" "s"))
; (("i" "s"))
; (("a" "n" "o" "t" "h" "e" "r"))
; (("s" "e" "n" "t" "e" "n" "c" "e"))
; (("இ" "து"))
; (("ஒ" "ரு"))
; (("வா" "க்" "கி" "ய" "ம்"))
; (("இ" "து"))
; (("அ" "டு" "த்" "த"))
; (("வா" "க்" "கி" "ய" "ம்"))
; (("எ" "ப்" "ப" "டி"))
; (("இ" "ரு" "க்" "கி" "ற" "து")))
```

`keep` is the same as `map` followed by `remove nil?`.

``` clj
(->> (seg/line-seq s)
     (keep seg/word-seq)
     (map (fn [word-seq] (map seg/char-seq word-seq)))
     pprint/pprint)
;=>
;((("T" "h" "i" "s"))
; (("i" "s"))
; (("a"))
; (("s" "e" "n" "t" "e" "n" "c" "e"))
; (("T" "h" "i" "s"))
; (("i" "s"))
; (("a" "n" "o" "t" "h" "e" "r"))
; (("s" "e" "n" "t" "e" "n" "c" "e"))
; (("இ" "து"))
; (("ஒ" "ரு"))
; (("வா" "க்" "கி" "ய" "ம்"))
; (("இ" "து"))
; (("அ" "டு" "த்" "த"))
; (("வா" "க்" "கி" "ய" "ம்"))
; (("எ" "ப்" "ப" "டி"))
; (("இ" "ரு" "க்" "கி" "ற" "து")))
```

`mapcat` flattens the structure one level after performing map.  We see characters within words, but the word-level structure is still visible.

``` clj
(->> (seg/line-seq s)
     (mapcat seg/word-seq)
     (map seg/char-seq)
     pprint/pprint)
;=>
;(("T" "h" "i" "s")
; ("i" "s")
; ("a")
; ("s" "e" "n" "t" "e" "n" "c" "e")
; ("T" "h" "i" "s")
; ("i" "s")
; ("a" "n" "o" "t" "h" "e" "r")
; ("s" "e" "n" "t" "e" "n" "c" "e")
; ("இ" "து")
; ("ஒ" "ரு")
; ("வா" "க்" "கி" "ய" "ம்")
; ("இ" "து")
; ("அ" "டு" "த்" "த")
; ("வா" "க்" "கி" "ய" "ம்")
; ("எ" "ப்" "ப" "டி")
; ("இ" "ரு" "க்" "கி" "ற" "து"))
```

Two `mapcat`s gives us the equivalent of what we would have gotten originally if we had called `flatten` at the end -- all characters are on the same level in the seq.

``` clj
(->> (seg/line-seq s)
     (mapcat seg/word-seq)
     (mapcat seg/char-seq)
     pprint/pprint)
;=>
;("T"
; "h"
; "i"
; "s"
; "i"
; "s"
; "a"
; "s"
; "e"
; "n"
; "t"
; "e"
; "n"
; "c"
; "e"
; "T"
; "h"
; "i"
; "s"
; "i"
; "s"
; "a"
; "n"
; "o"
; "t"
; "h"
; "e"
; "r"
; "s"
; "e"
; "n"
; "t"
; "e"
; "n"
; "c"
; "e"
; "இ"
; "து"
; "ஒ"
; "ரு"
; "வா"
; "க்"
; "கி"
; "ய"
; "ம்"
; "இ"
; "து"
; "அ"
; "டு"
; "த்"
; "த"
; "வா"
; "க்"
; "கி"
; "ய"
; "ம்"
; "எ"
; "ப்"
; "ப"
; "டி"
; "இ"
; "ரு"
; "க்"
; "கி"
; "ற"
; "து")
```
