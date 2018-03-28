(ns ccheck.nem.core
  (:require
    [clojure.string :as s]
    [clj-time.core :refer [date-time utc]]
    [clj-time.coerce :refer [to-long from-long]]
    [clj-time.format :refer [unparse formatter]]))    
;


(def NEM_EPOCH_DATETIME (date-time 2015 3 29 0 6 25))
(def NEM_EPOCH_MS (to-long NEM_EPOCH_DATETIME))
(def NEM_EPOCH (quot NEM_EPOCH_MS 1000))

(def XEM_DIVISOR 1000000)

(defn cleanup-addr [addr]
  (s/replace (str addr) #"[ \-]" ""))
;

(defn str-ntime [ntime]
  (when ntime
    (unparse
      (formatter "dd.MM.yy HH:mm:ss" utc)
      (from-long (* 1000 (+ NEM_EPOCH ntime))))))
;

(defn str-xem [uxem]
  (when uxem
    (let [i (quot uxem XEM_DIVISOR)
          d (rem  uxem XEM_DIVISOR)]
      (str
        (format "%d" i)
        (when (not= 0 d)
          (s/replace 
            (format ".%06d" (if (< d 0) (- d) d))
            #"(.+?)(0*)$" "$1"))))))
;

(defn expl-addr [addr]
  (str "http://explorer.ournem.com/#/s_account?account=" addr))
;

(defn expl-trans [hash]
  (str "http://explorer.ournem.com/#/s_tx?hash=" hash))
;

(defn expl-block [height]
  (str "http://explorer.ournem.com/#/s_block?height=" height))
;

;;.
