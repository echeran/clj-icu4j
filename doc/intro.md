# Introduction to clj-icu4j

## Examples

``` clj
(ns clj-icu4j.core)
(require '[clojure.string :as string])

(def s " This is a sentence. This is another sentence. இது ஒரு வாக்கியம்.  இது அடுத்த வாக்கியம்.  எப்படி இருக்கிறது? ")
```

``` clj
;; Note: clj-icu4j.core/line-seq is very different than clojure.core/line-seq

clj-icu4j.core> (->> (line-seq s) 
                     pprint)
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

``` clj
clj-icu4j.core> (->> (line-seq s)
                     (drop 9)
                     pprint)
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

``` clj
clj-icu4j.core> (require '[clojure.string :as string])

clj-icu4j.core> (->> (line-seq s)
                     (drop 9)
                     (map string/trim)
                     pprint)
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

``` clj
clj-icu4j.core> (->> (line-seq s)
                     (drop 9)
                     (map string/trim)
                     (map char-seq)
                     pprint)
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
