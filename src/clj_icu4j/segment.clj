(ns clj-icu4j.segment
  (:refer-clojure :exclude [line-seq])
  (:import com.ibm.icu.text.BreakIterator
           java.util.Locale))

(defrecord BreakCursor [^int start-val
                        ^int end-val])

(defrecord BreakIter [^String s
                      kind
                      ^java.util.Locale locale
                      skip-rule-statuses
                      cursor-atom])

(def ^:private kind-map {::char BreakIterator/KIND_CHARACTER
                         ::line BreakIterator/KIND_LINE
                         ::sentence BreakIterator/KIND_SENTENCE
                         ::title BreakIterator/KIND_TITLE
                         ::word BreakIterator/KIND_WORD})

(def ^:private kinds (keys kind-map))

(def ^:private default-skip-rule-statuses-map {::word #{BreakIterator/WORD_NONE}})

;; opts-map {::locale ::skip-rule-statuses}
;; validate {::locale -> use BreakIterator/getAvailableLocales, :skip-rule-statuses -> int array}

(defmulti break-iter-icu
  (fn [kind opts-map]
    kind))
(defmethod break-iter-icu ::char
  [kind opts-map]
  (if-let [locale (::locale opts-map)]
    (BreakIterator/getCharacterInstance locale)
    (BreakIterator/getCharacterInstance)))
(defmethod break-iter-icu ::line
  [kind opts-map]
  (if-let [locale (::locale opts-map)]
    (BreakIterator/getLineInstance locale)
    (BreakIterator/getLineInstance)))
(defmethod break-iter-icu ::sentence
  [kind opts-map]
  (if-let [locale (::locale opts-map)]
    (BreakIterator/getSentenceInstance locale)
    (BreakIterator/getSentenceInstance)))
(defmethod break-iter-icu ::title
  [kind opts-map]
  (if-let [locale (::locale opts-map)]
    (BreakIterator/getTitleInstance locale)
    (BreakIterator/getTitleInstance)))
(defmethod break-iter-icu ::word
  [kind opts-map]
  (if-let [locale (::locale opts-map)]
    (BreakIterator/getWordInstance locale)
    (BreakIterator/getWordInstance)))
(defmethod break-iter-icu :default
  [kind opts-map]
  (throw (Exception. (str "I don't know kind" kind "in available kinds" kinds))))


(defn- break-iter-icu-to-java
  [iter-icu kind opts-map s]
  (do
    (.setText iter-icu s))
  (let [cursor-init-map {:start-val (.first iter-icu)
                         :end-val (.next iter-icu)}
        cursor (map->BreakCursor cursor-init-map)
        cursor-atom (atom cursor)
        iter-init-map {:s s
                       :kind kind
                       :locale (::locale opts-map)
                       :skip-rule-statuses (::skip-rule-statuses opts-map)
                       :cursor-atom cursor-atom}
        iter (map->BreakIter iter-init-map)
        iter-proxy (proxy [java.util.Iterator]
                 []               
               (remove [] (throw (Exception. "Can't call 'remove' on immutable iterator")))
               (hasNext []
                 ;; first, skip through all the places we defined by rule status codes
                 (when-let [skip-statuses (:skip-rule-statuses iter)]
                   (while (and (contains? skip-statuses (.getRuleStatus iter-icu))
                               (not= (:end-val @cursor-atom) BreakIterator/DONE))
                     (let [next-val (.next iter-icu)]
                       (do (reset! cursor-atom {:start-val (:end-val @cursor-atom)
                                                :end-val next-val}))))) 
                 ;; return whether we are at end or not
                 (not= (:end-val @cursor-atom) BreakIterator/DONE))
               (next []
                 (let [old-cursor @cursor-atom
                       next-val (.next iter-icu)
                       new-cursor {:start-val (:end-val @cursor-atom)
                                        :end-val next-val}]
                   (reset! cursor-atom new-cursor)
                   (subs s (:start-val old-cursor) (:end-val old-cursor)))))]
    iter-proxy))

(defn- break-iter-java
  [kind opts-map s]
  (let [break-iter (break-iter-icu kind opts-map)
        break-iter-java (break-iter-icu-to-java break-iter kind opts-map s)]
    break-iter-java))

(defn- break-seq
  ([kind s]
   (let [default-opts-map {::skip-rule-statuses (get default-skip-rule-statuses-map kind)}]
    (break-seq kind default-opts-map s)))
  ([kind opts-map s]
   (let []
     (-> (break-iter-java kind opts-map s)
         iterator-seq))))
 
(defn char-seq
  [& args]
  (apply break-seq (cons ::char args)))

(defn line-seq
  [& args]
  (apply break-seq (cons ::line args)))

(defn sentence-seq
  [& args]
  (apply break-seq (cons ::sentence args)))

(defn title-seq
  [& args]
  (apply break-seq (cons ::title args)))

(defn word-seq
  [& args]
  (apply break-seq (cons ::word args)))


