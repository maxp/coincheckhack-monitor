(ns ccheck.app.scan
  (:import 
    java.util.concurrent.atomic.AtomicInteger
    java.util.concurrent.atomic.AtomicLong)
  (:require
    [clojure.string :as s]
    [clojure.core.async :refer [thread]]
    [clj-time.core :as tc]
    [clj-time.coerce :refer [from-long]]
    [mount.core :refer [defstate]]
    ;    
    [mlib.conf :refer [conf]]
    [mlib.core :refer [abs]]
    [mlib.log :refer [debug info warn]]
    [mlib.time :refer [tf-ddmmyy-hhmmss]]
    [mlib.tg.core :refer [send-text]]
    ;
    [ccheck.app.db :refer 
      [ clear-accs clear-trans
        new-acc acc-by-id acc-update scan-list    
        new-trans trans-by-id max-xid]]
    [ccheck.nem.core :refer [str-xem str-ntime XEM_DIVISOR]]
    [ccheck.nem.nis :refer [api chain-height out-trans in-trans pubkey-account]]
    [ccheck.app.notify :refer [trs-notify update-last-height]]))
;


(def XEM_VALUABLE_OUT (* 500 XEM_DIVISOR))
(def XEM_VALUABLE_IN  (* 200 XEM_DIVISOR))

(def HACK_NTIME 71000000)   ;; 71082999)


(defn init-database []
  (info "init database")
  (clear-accs)
  (clear-trans)
  ;
  (doseq [a (-> conf :scan :hacker-list)]
    (new-acc {:_id a :last_xid 0}))
  ;
  (doseq [a (-> conf :scan :known-list)]
    (new-acc { :_id      (:addr a) 
               :noscan   true 
               :last_xid 0 
               :note     (:note a)})))
  ;
;


(defn fresh-trans [addr last-xid]
  (take-while 
    #(< last-xid (:xid %))
    (out-trans addr)))
;

(defn valuable-in? [{amount :amount type :type}]
  (and amount (= type 257) (> amount XEM_VALUABLE_IN)))


(defn valuable-out? [{amount :amount type :type}]
  (and amount (= type 257) (> amount XEM_VALUABLE_OUT)))
;

(defn has-incoming-before [addr ntime]
  (->>
    (in-trans addr)
    (some 
      #(and 
          (< (:ntime %) HACK_NTIME)
          (valuable-in? %)))))
;
   

(defn new-recipient [{rcp :rcp ntime :ntime}]
  (when-not (acc-by-id rcp)    ; when account does not exists
    (let [last-xid  (some 
                      (fn [t] (when (> ntime (:ntime t)) (:xid t)))
                      (out-trans rcp))]
      (new-acc 
        { :_id      rcp 
          :last_xid (or last-xid 0)
          :noscan   (has-incoming-before rcp HACK_NTIME)}))))
;
  

(defn scan-addr [addr last-xid]
  (let [trs (fresh-trans addr last-xid)
        xid (-> trs first (:xid 0))
        rcp-count (volatile! 0)]
    ;;
    (when (and xid (< last-xid xid))
      ;; TODO: fetch balance
      (acc-update addr {:last_xid xid}))
    ;;
    (doseq [tr trs :when (valuable-out? tr)] 
      (let [from (:address (pubkey-account (:pubkey tr)))
            data (-> tr 
                  (assoc :_id (:xid tr) :from from)
                  (dissoc :xid))]
        (new-trans data)
        (trs-notify from tr)
        (when-let [new-rcp (new-recipient tr)]
          (vswap! rcp-count inc)
          (info "new recipient:" new-rcp))))
      ;;
    ;;
    @rcp-count))
;


(defn scan-addresses []
  (let [n (volatile! 0)]
    (doseq [[addr xid] (scan-list)]
      (vswap! n inc)
      (let [rcp-n (scan-addr addr xid)]
        (when (> rcp-n 0)
          (info "new recipients:" rcp-n))))
    (debug "scaned:" @n)
    (update-last-height (chain-height))))
;

;;.
