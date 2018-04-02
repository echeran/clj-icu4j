# Introduction to clj-icu4j

## Examples

``` clj
user> (require '[clojure.string :as string])
user> (require '[clojure.pprint :as pprint])
user> (require '[clj-icu4j.segment :as seg])

(def s " This is a sentence. This is another sentence. இது ஒரு வாக்கியம்.  இது அடுத்த வாக்கியம்.  எப்படி இருக்கிறது? ")
```

Let's start off with pretty-printing.  Useful when we get to nested seqs / data structures.
``` clj
;; Note: clj-icu4j.core/line-seq is very different than clojure.core/line-seq

user> (->> (seq/line-seq s) 
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
user> (->> (seq/line-seq s)
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

``` clj
user> (require '[clojure.string :as string])

user> (->> (seq/line-seq s)
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

After we conver the string into a seq of lines, we map the char-seq on the resulting lines in the seq.
``` clj
user> (->> (seq/line-seq s)
           (drop 9)
           (map string/trim)
           (map seq/char-seq)
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
user> (->> (seg/line-seq s)
           (map seg/word-seq)
           (remove nil?)
           (map (fn [word-seq] (map seg/char-seq word-seq)))
           pprint/pprint)
```

`keep` is the same as `map` followed by `remove nil?`.

``` clj
user> (->> (seg/line-seq s)
           (keep seg/word-seq)
           (map (fn [word-seq] (map seg/char-seq word-seq)))
           pprint/pprint)
```

`mapcat` flattens the structure one level after performing map.  We see characters within words, but the word-level structure is still visible.

``` clj
user> (->> (seg/line-seq s)
           (mapcat seg/word-seq)
           (map seg/char-seq)
           pprint/pprint)
```

Two `mapcat`s gives us the equivalent of what we would have gotten originally if we had called `flatten` at the end -- all characters are on the same level in the seq.

``` clj
user> (->> (seg/line-seq s)
           (mapcat seg/word-seq)
           (mapcat seg/char-seq)
           pprint/pprint)
```
