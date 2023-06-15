(ns clj-icu4j.calendar
  (:require [clojure.math.numeric-tower :as math :refer [expt floor round abs]]))

;; This namespace converts the Common Lisp code from
;; https://github.com/EdReingold/calendar-code2/blob/main/calendar.l#L3884
;; into Clojure to test & debug the Rust ICU4X impls for
;; calendars being added currently


(def january
  ;; TYPE standard-month
  ;; January on Julian/Gregorian calendar.
  1)

(def february
  ;; TYPE standard-month
  ;; February on Julian/Gregorian calendar.
  2)

(def march
  ;; TYPE standard-month
  ;; March on Julian/Gregorian calendar.
  3)

(def april
  ;; TYPE standard-month
  ;; April on Julian/Gregorian calendar.
  4)

(def may
  ;; TYPE standard-month
  ;; May on Julian/Gregorian calendar.
  5)

(def june
  ;; TYPE standard-month
  ;; June on Julian/Gregorian calendar.
  6)

(def july
  ;; TYPE standard-month
  ;; July on Julian/Gregorian calendar.
  7)

(def august
  ;; TYPE standard-month
  ;; August on Julian/Gregorian calendar.
  8)

(def september
  ;; TYPE standard-month
  ;; September on Julian/Gregorian calendar.
  9)

(def october
  ;; TYPE standard-month
  ;; October on Julian/Gregorian calendar.
  10)

(def november
  ;; TYPE standard-month
  ;; November on Julian/Gregorian calendar.
  11)

(def december
  ;; TYPE standard-month
  ;; December on Julian/Gregorian calendar.
  12)



(defn rd [tee]
  ;; TYPE moment -> moment
  ;; Identity function for fixed dates/moments.  If internal
  ;; timekeeping is shifted, change $epoch$ to be RD date of
  ;; origin of internal count.  $epoch$ should be an integer.
  (let [epoch 0]
    (- tee epoch)))



(defn deg [x]
  ;; TYPE real -> angle
  ;; TYPE list-of-reals -> list-of-angles
  ;; $x$ degrees.
  ;; For typesetting purposes.
  x)

(defn radians-from-degrees [theta]
  ;; TYPE real -> radian
  ;; Convert angle $theta$ from degrees to radians.
  (* (mod theta 360) Math/PI 1/180))

(defn sin-degrees [theta]
  ;; TYPE angle -> amplitude
  ;; Sine of $theta$ (given in degrees).
  (Math/sin (radians-from-degrees theta)))

(defn cos-degrees [theta]
  ;; TYPE angle -> amplitude
  ;; Cosine of $theta$ (given in degrees).
  (Math/cos (radians-from-degrees theta)))
;;;;;;;;;;



(def gregorian-epoch
  ;; TYPE fixed-date
  ;; Fixed date of start of the (proleptic) Gregorian
  ;; calendar.
  (rd 1))

(defn quotient [m n]
  ;; TYPE (real nonzero-real) -> integer
  ;; Whole part of $m$/$n$.
  ;;(int (/ m n))

  ;; copying the ICU4X logic from icu_calendar::helpers::quotient,
  ;; which is based on i32 (ints)
  ;; (let [a (quot m n)
  ;;       b (rem m n)]
  ;;   (if (or (>= m 0) (zero? b))
  ;;     a
  ;;     (dec a)))

  (-> (div-rem-euclid m n)
      first))

;; copying the ICU4X logic from icu_calendar::helpers::div_rem_euclid,
;; which is based on i32 (ints)
(defn div-rem-euclid [n d]
  (let [a (quot n d)
        b (rem n d)]
    (if (or (>= n 0) (zero? b))
      [a b]
      [(dec a) (+ d b)])))

(defn rem-euclid [n d]
  (-> (div-rem-euclid n d)
      second))

(defn gregorian-year-from-fixed [date]
  ;; TYPE fixed-date -> gregorian-year
  ;; Gregorian year corresponding to the fixed $date$.
  (let [d0                              ; Prior days.
        (- date gregorian-epoch)
        n400                           ; Completed 400-year cycles.
        (quotient d0 146097)
        d1                             ; Prior days not in n400.
        (mod d0 146097)
        n100                           ; 100-year cycles not in n400.
        (quotient d1 36524)
        d2                          ; Prior days not in n400 or n100.
        (mod d1 36524)
        n4                       ; 4-year cycles not in n400 or n100.
        (quotient d2 1461)
        d3                     ; Prior days not in n400, n100, or n4.
        (mod d2 1461)
        n1                          ; Years not in n400, n100, or n4.
        (quotient d3 365)
        year (+ (* 400 n400)
                (* 100 n100)
                (* 4 n4)
                n1)]
    (if (or (= n100 4) (= n1 4))
        year      ; Date is day 366 in a leap year.
      (inc year)))); Date is ordinal day (1+ (mod d3 365))
                                        ; in (1+ year).

(defn standard-month [date]
  ;; TYPE standard-date -> standard-month
  ;; Month field of $date$ = (year month day).
  (second date))

(defn standard-day [date]
  ;; TYPE standard-date -> standard-day
  ;; Day field of $date$ = (year month day).
  (nth date 2))

(defn standard-year [date]
  ;; TYPE standard-date -> standard-year
  ;; Year field of $date$ = (year month day).
  (first date))

(defn gregorian-leap-year? [g-year]
  ;; TYPE gregorian-year -> boolean
  ;; True if $g-year$ is a leap year on the Gregorian
  ;; calendar.
  (and (= (mod g-year 4) 0)
       (not (contains?
             (set (list 100 200 300))
              (mod g-year 400)))))

(defn fixed-from-gregorian [g-date]
  ;; TYPE gregorian-date -> fixed-date
  ;; Fixed date equivalent to the Gregorian date $g-date$.
  (let [month (standard-month g-date)
        day (standard-day g-date)
        year (standard-year g-date)]
    (+ (dec gregorian-epoch); Days before start of calendar
       (* 365 (dec year)); Ordinary days since epoch
       (quotient (dec year)
                 4); Julian leap days since epoch...
       (-          ; ...minus century years since epoch...
        (quotient (dec year) 100))
       (quotient   ; ...plus years since epoch divisible...
        (dec year) 400)  ; ...by 400.
       (quotient        ; Days in prior months this year...
        (- (* 367 month) 362); ...assuming 30-day Feb
        12)
       (if (<= month 2) ; Correct for 28- or 29-day Feb
           0
         (if (gregorian-leap-year? year)
             -1
           -2))
       day)))          ; Days so far this month.


(defn gregorian-date-difference [g-date1 g-date2]
  ;; TYPE (gregorian-date gregorian-date) -> integer
  ;; Number of days from Gregorian date $g-date1$ until
  ;; $g-date2$.
  (- (fixed-from-gregorian g-date2)
     (fixed-from-gregorian g-date1)))

(defn gregorian-date [year month day]
  ;; TYPE (gregorian-year gregorian-month gregorian-day)
  ;; TYPE  -> gregorian-date
  (list year month day))

(defn poly [x a]
  ;; TYPE (real list-of-reals) -> real
  ;; Sum powers of $x$ with coefficients (from order 0 up)
  ;; in list $a$.
  (if (empty? a)
      0
      (+ (first a) (* x (poly x (rest a))))))

(defn ephemeris-correction [tee]
  ;; TYPE moment -> fraction-of-day
  ;; Dynamical Time minus Universal Time (in days) for
  ;; moment $tee$.  Adapted from "Astronomical Algorithms"
  ;; by Jean Meeus, Willmann-Bell (1991) for years
  ;; 1600-1986 and from polynomials on the NASA
  ;; Eclipse web site for other years.
  (let [year (gregorian-year-from-fixed (floor tee))
        c (/ (gregorian-date-difference
              (gregorian-date 1900 january 1)
              (gregorian-date year july 1))
             36525)
        c2051 (* 1/86400
                 (+ -20 (* 32 (expt (/ (- year 1820) 100) 2))
                    (* 0.5628 (- 2150 year))))
        y2000 (- year 2000)
        c2006 (* 1/86400
                 (poly y2000
                       (list 62.92 0.32217 0.005589)))
        c1987 (* 1/86400
                 (poly y2000
                       (list 63.86 0.3345 -0.060374 
                             0.0017275
                             0.000651814 0.00002373599)))
        c1900 (poly c 
                    (list -0.00002 0.000297 0.025184
                          -0.181133 0.553040 -0.861938
                          0.677066 -0.212591))
        c1800 (poly c 
                    (list -0.000009 0.003844 0.083563 
                          0.865736
                          4.867575 15.845535 31.332267
                          38.291999 28.316289 11.636204
                          2.043794))
        y1700 (- year 1700)
        c1700 (* 1/86400
                 (poly y1700
                       (list 8.118780842 -0.005092142
                             0.003336121 -0.0000266484)))
        y1600 (- year 1600)
        c1600 (* 1/86400
                 (poly y1600
                       (list 120 -0.9808 -0.01532 
                             0.000140272128)))
        y1000 (/ (- year 1000) 100)
        c500 (* 1/86400
                (poly y1000
                      (list 1574.2 -556.01 71.23472 0.319781
                            -0.8503463 -0.005050998 
                            0.0083572073)))
        y0 (/ year 100)
        c0 (* 1/86400
              (poly y0
                    (list 10583.6 -1014.41 33.78311 
                          -5.952053 -0.1798452 0.022174192
                          0.0090316521)))
        y1820 (/ (- year 1820) 100)
        other (* 1/86400
                 (poly y1820 (list -20 0 32)))]
    (cond (<= 2051 year 2150) c2051
          (<= 2006 year 2050) c2006
          (<= 1987 year 2005) c1987
          (<= 1900 year 1986) c1900
          (<= 1800 year 1899) c1800
          (<= 1700 year 1799) c1700
          (<= 1600 year 1699) c1600
          (<= 500 year 1599) c500
          (< -500 year 500) c0
          :true other)))


(defn gregorian-new-year [g-year]
  ;; TYPE gregorian-year -> fixed-date
  ;; Fixed date of January 1 in $g-year$.
  (fixed-from-gregorian
   (gregorian-date g-year january 1)))

;;;; Section: Time and Astronomy

(defn hr [x]
  ;; TYPE real -> duration
  ;; $x$ hours.
  (/ x 24))

(def j2000
  ;; TYPE moment
  ;; Noon at start of Gregorian year 2000.
  (+ (hr 12) (gregorian-new-year 2000)))

(def mean-synodic-month
  ;; TYPE duration
  29.530588861)




;;;


(defmacro sigma [list body]
  ;; TYPE (list-of-pairs (list-of-reals->real))
  ;; TYPE  -> real
  ;; $list$ is of the form ((i1 l1)...(in ln)).
  ;; Sum of $body$ for indices i1...in
  ;; running simultaneously thru lists l1...ln.

  ;; list is now a typical Clojure binding vector
  `(apply + (map (fn ~(vec (map first (partition 2 list)))
                    ~body)
                  ~@(map second (partition 2 list)))))


(defn universal-from-dynamical [tee]
  ;; TYPE moment -> moment
  ;; Universal moment from Dynamical time $tee$.
  (- tee (ephemeris-correction tee)))

(defn nth-new-moon [n]
  ;; TYPE integer -> moment
  ;; Moment of $n$-th new moon after (or before) the new moon
  ;; of January 11, 1.  Adapted from "Astronomical Algorithms"
  ;; by Jean Meeus, Willmann-Bell, corrected 2nd edn., 2005.
  (let [n0 24724                       ; Months from RD 0 until j2000.
        k (- n n0)                   ; Months since j2000.
        c (/ k 1236.85)              ; Julian centuries.
        approx (+ j2000
                  (poly c (list 5.09766
                                (* mean-synodic-month
                                   1236.85)
                                0.00015437
                                -0.000000150
                                0.00000000073)))
        cap-E (poly c (list 1 -0.002516 -0.0000074))
        solar-anomaly
        (poly c (deg (list 2.5534
                           (* 1236.85 29.10535670)
                           -0.0000014 -0.00000011)))
        lunar-anomaly
        (poly c (deg (list 201.5643 (* 385.81693528
                                       1236.85)
                           0.0107582 0.00001238
                           -0.000000058)))
        moon-argument                  ; Moon's argument of latitude.
        (poly c (deg (list 160.7108 (* 390.67050284
                                       1236.85)
                           -0.0016118 -0.00000227
                           0.000000011)))
        cap-omega                      ; Longitude of ascending node.
        (poly c (deg (list 124.7746 (* -1.56375588 1236.85)
                           0.0020672 0.00000215)))
        E-factor (list 0 1 0 0 1 1 2 0 0 1 0 1 1 1 0 0 0 0
                       0 0 0 0 0 0)
        solar-coeff (list 0 1 0 0 -1 1 2 0 0 1 0 1 1 -1 2
                          0 3 1 0 1 -1 -1 1 0)
        lunar-coeff (list 1 0 2 0 1 1 0 1 1 2 3 0 0 2 1 2
                          0 1 2 1 1 1 3 4)
        moon-coeff (list 0 0 0 2 0 0 0 -2 2 0 0 2 -2 0 0
                         -2 0 -2 2 2 2 -2 0 0)
        sine-coeff
        (list -0.40720 0.17241 0.01608 0.01039
              0.00739 -0.00514 0.00208
              -0.00111 -0.00057 0.00056
              -0.00042 0.00042 0.00038
              -0.00024 -0.00007 0.00004
              0.00004 0.00003 0.00003
              -0.00003 0.00003 -0.00002
              -0.00002 0.00002)
        correction
        (+ (* -0.00017 (sin-degrees cap-omega))
           (sigma [v sine-coeff
                   w E-factor
                   x solar-coeff
                   y lunar-coeff
                   z moon-coeff]
                  (* v (expt cap-E w)
                     (sin-degrees
                      (+ (* x solar-anomaly)
                         (* y lunar-anomaly)
                         (* z moon-argument))))))
        add-const
        (list 251.88 251.83 349.42 84.66
              141.74 207.14 154.84 34.52 207.19
              291.34 161.72 239.56 331.55)
        add-coeff
        (list 0.016321 26.651886
              36.412478 18.206239 53.303771
              2.453732 7.306860 27.261239 0.121824
              1.844379 24.198154 25.513099
              3.592518)
        add-factor
        (list 0.000165 0.000164 0.000126
              0.000110 0.000062 0.000060 0.000056
              0.000047 0.000042 0.000040 0.000037
              0.000035 0.000023)
        extra
        (* 0.000325
           (sin-degrees
            (poly c
                  (deg (list 299.77 132.8475848
                             -0.009173)))))
        additional
        (sigma [i add-const
                j add-coeff
                l add-factor]
               (* l (sin-degrees (+ i (* j k)))))]
    (universal-from-dynamical
     (+ approx correction extra additional))))

(defn dynamical-from-universal [tee_rom-u]
  ;; TYPE moment -> moment
  ;; Dynamical time at Universal moment $tee_rom-u$.
  (+ tee_rom-u (ephemeris-correction tee_rom-u)))

(defn julian-centuries [tee]
  ;; TYPE moment -> century
  ;; Julian centuries since 2000 at moment $tee$.
  (/ (- (dynamical-from-universal tee) j2000)
     36525))

(defn mean-lunar-longitude [c]
  ;; TYPE century -> angle
  ;; Mean longitude of moon (in degrees) at moment
  ;; given in Julian centuries $c$.
  ;; Adapted from "Astronomical Algorithms" by Jean Meeus,
  ;; Willmann-Bell, 2nd edn., 1998, pp. 337-340.
  (mod
   (poly c
         (deg (list 218.3164477 481267.88123421
                    -0.0015786 1/538841 -1/65194000)))))


(defn lunar-elongation [c]
  ;; TYPE century -> angle
  ;; Elongation of moon (in degrees) at moment
  ;; given in Julian centuries $c$.
  ;; Adapted from "Astronomical Algorithms" by Jean Meeus,
  ;; Willmann-Bell, 2nd edn., 1998, p. 338.
  (mod
   (poly c
         (deg (list 297.8501921 445267.1114034
                    -0.0018819 1/545868 -1/113065000)))
   360))

(defn solar-anomaly [c]
  ;; TYPE century -> angle
  ;; Mean anomaly of sun (in degrees) at moment
  ;; given in Julian centuries $c$.
  ;; Adapted from "Astronomical Algorithms" by Jean Meeus,
  ;; Willmann-Bell, 2nd edn., 1998, p. 338.
  (mod
   (poly c
         (deg (list 357.5291092 35999.0502909
                    -0.0001536 1/24490000)))
   360))

(defn lunar-anomaly [c]
  ;; TYPE century -> angle
  ;; Mean anomaly of moon (in degrees) at moment
  ;; given in Julian centuries $c$.
  ;; Adapted from "Astronomical Algorithms" by Jean Meeus,
  ;; Willmann-Bell, 2nd edn., 1998, p. 338.
  (mod
   (poly c
         (deg (list 134.9633964 477198.8675055
                    0.0087414 1/69699 -1/14712000)))
   360))

(defn moon-node [c]
  ;; TYPE century -> angle
  ;; Moon's argument of latitude (in degrees) at moment
  ;; given in Julian centuries $c$.
  ;; Adapted from "Astronomical Algorithms" by Jean Meeus,
  ;; Willmann-Bell, 2nd edn., 1998, p. 338.
  (mod
   (poly c
         (deg (list 93.2720950 483202.0175233
                    -0.0036539 -1/3526000 1/863310000)))
   360))

(defn nutation [tee]
  ;; TYPE moment -> circle
  ;; Longitudinal nutation at moment $tee$.
  (let [c                               ; moment in Julian centuries
        (julian-centuries tee)
        cap-A (poly c (deg (list 124.90 -1934.134
                                 0.002063)))
        cap-B (poly c (deg (list 201.11 72001.5377
                                 0.00057)))]
    (+ (* (deg -0.004778) (sin-degrees cap-A))
       (* (deg -0.0003667) (sin-degrees cap-B)))))

(defn lunar-longitude [tee]
  ;; TYPE moment -> angle
  ;; Longitude of moon (in degrees) at moment $tee$.
  ;; Adapted from "Astronomical Algorithms" by Jean Meeus,
  ;; Willmann-Bell, 2nd edn., 1998, pp. 338-342.
  (let [c (julian-centuries tee)
        cap-L-prime (mean-lunar-longitude c)
        cap-D (lunar-elongation c)
        cap-M (solar-anomaly c)
        cap-M-prime (lunar-anomaly c)
        cap-F (moon-node c)
        cap-E (poly c (list 1 -0.002516 -0.0000074))
        args-lunar-elongation
        (list 0 2 2 0 0 0 2 2 2 2 0 1 0 2 0 0 4 0 4 2 2 1
              1 2 2 4 2 0 2 2 1 2 0 0 2 2 2 4 0 3 2 4 0 2
              2 2 4 0 4 1 2 0 1 3 4 2 0 1 2)
        args-solar-anomaly
        (list 0 0 0 0 1 0 0 -1 0 -1 1 0 1 0 0 0 0 0 0 1 1
              0 1 -1 0 0 0 1 0 -1 0 -2 1 2 -2 0 0 -1 0 0 1
              -1 2 2 1 -1 0 0 -1 0 1 0 1 0 0 -1 2 1 0)
        args-lunar-anomaly
        (list 1 -1 0 2 0 0 -2 -1 1 0 -1 0 1 0 1 1 -1 3 -2
              -1 0 -1 0 1 2 0 -3 -2 -1 -2 1 0 2 0 -1 1 0
              -1 2 -1 1 -2 -1 -1 -2 0 1 4 0 -2 0 2 1 -2 -3
              2 1 -1 3)
        args-moon-node
        (list 0 0 0 0 0 2 0 0 0 0 0 0 0 -2 2 -2 0 0 0 0 0
              0 0 0 0 0 0 0 2 0 0 0 0 0 0 -2 2 0 2 0 0 0 0
              0 0 -2 0 0 0 0 -2 -2 0 0 0 0 0 0 0)
        sine-coeff
        (list 6288774 1274027 658314 213618 -185116 -114332
              58793 57066 53322 45758 -40923 -34720 -30383
              15327 -12528 10980 10675 10034 8548 -7888
              -6766 -5163 4987 4036 3994 3861 3665 -2689
              -2602 2390 -2348 2236 -2120 -2069 2048 -1773
              -1595 1215 -1110 -892 -810 759 -713 -700 691
              596 549 537 520 -487 -399 -381 351 -340 330
              327 -323 299 294)
        correction
        (* (deg 1/1000000)
           (sigma [v sine-coeff
                   w args-lunar-elongation
                   x args-solar-anomaly
                   y args-lunar-anomaly
                   z args-moon-node]
                  (* v (expt cap-E (abs x))
                     (sin-degrees
                      (+ (* w cap-D)
                         (* x cap-M)
                         (* y cap-M-prime)
                         (* z cap-F))))))
        venus (* (deg 3958/1000000)
                 (sin-degrees
                  (+ (deg 119.75) (* c (deg 131.849)))))
        jupiter (* (deg 318/1000000)
                   (sin-degrees
                    (+ (deg 53.09)
                       (* c (deg 479264.29)))))
        flat-earth
        (* (deg 1962/1000000)
           (sin-degrees (- cap-L-prime cap-F)))]
    (mod (+ cap-L-prime correction venus jupiter flat-earth
            (nutation tee))
         360)))

(defn aberration [tee]
  ;; TYPE moment -> circle
  ;; Aberration at moment $tee$.
  (let [c                               ; moment in Julian centuries
        (julian-centuries tee)]
    (- (* (deg 0.0000974)
          (cos-degrees
           (+ (deg 177.63) (* (deg 35999.01848) c))))
       (deg 0.005575))))

(defn solar-longitude [tee]
  ;; TYPE moment -> season
  ;; Longitude of sun at moment $tee$.
  ;; Adapted from "Planetary Programs and Tables from -4000
  ;; to +2800" by Pierre Bretagnon and Jean-Louis Simon,
  ;; Willmann-Bell, 1986.
  (let [c                               ; moment in Julian centuries
        (julian-centuries tee)
        coefficients
        (list 403406 195207 119433 112392 3891 2819 1721
              660 350 334 314 268 242 234 158 132 129 114
              99 93 86 78 72 68 64 46 38 37 32 29 28 27 27
              25 24 21 21 20 18 17 14 13 13 13 12 10 10 10
              10)
        multipliers
        (list 0.9287892 35999.1376958 35999.4089666
              35998.7287385 71998.20261 71998.4403
              36000.35726 71997.4812 32964.4678
              -19.4410 445267.1117 45036.8840 3.1008
              22518.4434 -19.9739 65928.9345
              9038.0293 3034.7684 33718.148 3034.448
              -2280.773 29929.992 31556.493 149.588
              9037.750 107997.405 -4444.176 151.771
              67555.316 31556.080 -4561.540
              107996.706 1221.655 62894.167
              31437.369 14578.298 -31931.757
              34777.243 1221.999 62894.511
              -4442.039 107997.909 119.066 16859.071
              -4.578 26895.292 -39.127 12297.536
              90073.778)
        addends
        (list 270.54861 340.19128 63.91854 331.26220
              317.843 86.631 240.052 310.26 247.23
              260.87 297.82 343.14 166.79 81.53
              3.50 132.75 182.95 162.03 29.8
              266.4 249.2 157.6 257.8 185.1 69.9
              8.0 197.1 250.4 65.3 162.7 341.5
              291.6 98.5 146.7 110.0 5.2 342.6
              230.9 256.1 45.3 242.9 115.2 151.8
              285.3 53.3 126.6 205.7 85.9
              146.1)
        lambda
        (+ (deg 282.7771834)
           (* (deg 36000.76953744) c)
           (* (deg 0.000005729577951308232)
              (sigma [x coefficients
                      y addends
                      z multipliers]
                     (* x (sin-degrees (+ y (* z c)))))))]
    (mod (+ lambda (aberration tee) (nutation tee))
         360)))

(defn lunar-phase [tee]
  ;; TYPE moment -> phase
  ;; Lunar phase, as an angle in degrees, at moment $tee$.
  ;; An angle of 0 means a new moon, 90 degrees means the
  ;; first quarter, 180 means a full moon, and 270 degrees
  ;; means the last quarter.
  (let [phi (mod (- (lunar-longitude tee)
                    (solar-longitude tee))
                 360)
        t0 (nth-new-moon 0)
        n (round (/ (- tee t0) mean-synodic-month))
        phi-prime (* (deg 360)
                     (mod (/ (- tee (nth-new-moon n))
                             mean-synodic-month)
                          1))]
    (if (> (abs (- phi phi-prime)) (deg 180)) ; close call
        phi-prime
        phi)))


(defmacro next [index initial condition]
  ;; TYPE (* integer (integer->boolean)) -> integer
  ;; First integer greater or equal to $initial$ such that
  ;; $condition$ holds.
  `(loop [~index ~initial]
     (if ~condition
       ~index
       (recur (inc ~index)))))

(defn new-moon-at-or-after [tee]
  ;; TYPE moment -> moment
  ;; Moment UT of first new moon at or after $tee$.
  (let [t0 (nth-new-moon 0)
        phi (lunar-phase tee)
        n (round (- (/ (- tee t0) mean-synodic-month)
                    (/ phi (deg 360))))]
    (nth-new-moon (next k n (>= (nth-new-moon k) tee)))))
