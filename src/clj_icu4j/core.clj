(ns clj-icu4j.core
  (:refer-clojure :exclude [line-seq])
  (:import com.ibm.icu.text.BreakIterator))


(defn break-iter-icu-to-java
  "Returns a Java Iterator out of an ICU iterator object.
Takes in icu-iter as the ICU iterator object, and
iter-sentinal as the equality check value of the return
value of next to indicate termination of iteration."
  [icu-iter s]
  (let [start-val (atom (.first icu-iter))
        end-val (atom (.next icu-iter)) 
        iter (proxy [java.util.Iterator]
                 []
               (remove [] (throw (Exception. "Can't call 'remove' on immutable iterator")))
               (hasNext [] (not= @end-val BreakIterator/DONE))
               (next [] (let [old-start-val @start-val
                              old-end-val @end-val
                              new-end-val (.next icu-iter)]
                          (reset! start-val old-end-val)
                          (reset! end-val new-end-val)
                          (subs s old-start-val old-end-val))))]
    iter))

(defn break-iter-java
  "Given an instance of a BreakIterator, and the string that it is
  based on, returns a concrete instance that implements java.util.Iterator with the same functionality." 
  [break-iter s]
  (let [break-iter-icu (doto break-iter
                         (.setText s))
        break-iter-java (break-iter-icu-to-java break-iter-icu s)]
    break-iter-java))

;; title case iterator and seq

(defn- break-iter-title-java
  "Return a java.util.Iterator with the functionality of a BreakIterator
of title breaks."
  [s]
  (break-iter-java (BreakIterator/getTitleInstance) s))

(defn title-seq
  [s]
  (-> (break-iter-title-java s)
      iterator-seq))

;; sentence iterator and seq

(defn- break-iter-sentence-java
  "Return a java.util.Iterator with the functionality of a BreakIterator
of sentences."
  [s]
  (break-iter-java (BreakIterator/getSentenceInstance) s))

(defn sentence-seq
  [s]
  (-> (break-iter-sentence-java s)
      iterator-seq))

;; word iterator and seq

(defn- break-iter-word-java
  "Return a java.util.Iterator with the functionality of a BreakIterator
of words."
  [s]
  (break-iter-java (BreakIterator/getWordInstance) s))


(defn- break-iter-word-java-exclude-punct
  "Return a java.util.Iterator with the functionality of a BreakIterator
  of words, but exclude punctuation, whitespace, etc. from the words."
  [s]
  (let [break-iter-icu (doto (BreakIterator/getWordInstance)
                         (.setText s))
        start-val (atom (.first break-iter-icu))
        end-val (atom (.next break-iter-icu))
        iter (proxy [java.util.Iterator]
                 []               
               (remove [] (throw (Exception. "Can't call 'remove' on immutable iterator")))
               (hasNext []
                 ;; first, skip through all the non-words
                 (while (and (= (.getRuleStatus break-iter-icu) BreakIterator/WORD_NONE)
                             (not= @end-val BreakIterator/DONE))
                   (do (reset! start-val @end-val)
                       (reset! end-val (.next break-iter-icu)))) 
                 ;; return if we are at end
                 (not= @end-val BreakIterator/DONE))
               (next []
                 (let [old-start-val @start-val
                       old-end-val @end-val
                       new-end-val (.next break-iter-icu)]
                   (reset! start-val old-end-val)
                   (reset! end-val new-end-val)
                   (subs s old-start-val old-end-val))))]
    iter))

(defn word-seq
  ([s]
   (word-seq s false))
  ([s include-punct]
   (let [word-iter-java (if include-punct
                          (break-iter-word-java s)
                          (break-iter-word-java-exclude-punct s))]
     (-> word-iter-java
         iterator-seq))))

;; line iterator and seq

(defn- break-iter-line-java
  "Return a java.util.Iterator with the functionality of a BreakIterator
of lines."
  [s]
  (break-iter-java (BreakIterator/getLineInstance) s))


;; Do not confuse with clojure.core/line-seq, which
;; provides completely different behavior.
(defn line-seq
  [s]
  (-> (break-iter-line-java s)
      iterator-seq))

;; logical character iterator and seq

(defn- break-iter-char-java
  "Return a java.util.Iterator with the functionality of a BreakIterator
of logical characters."
  [s]
  (break-iter-java (BreakIterator/getCharacterInstance) s))

(defn char-seq
  [s]
  (-> (break-iter-char-java s)
      iterator-seq))
