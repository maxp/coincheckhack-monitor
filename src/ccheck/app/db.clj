(ns ccheck.app.db
  (:require
    [clj-time.core :as tc]
    [monger.collection :as mc]
    [monger.query :as mq]
    [mount.core :refer [defstate]]
    ;    
    [mlib.conf :refer [conf]]
    [mlib.log :refer [debug info warn try-warn]]
    [mlib.mdb.conn :refer [connect disconnect]]))
;


(defstate mdb
 :start
   (connect (-> conf :mdb))
 :stop
   (disconnect mdb))


(defn dbc []
  (:db mdb))
;


(def ACCS  "accs")
(comment
  {
    :_id        "NEMADDRESS"
    :ct         :timstamp
    :last_xid   :int   ;; last  transaction id
    :amount     :num
    :noscan     :bool  ;; stop recursion
    :note       "str"})
;

(def TRANS "trans")
(comment
  {
    :_id    :int    ;; transaction id
    :hash   :hexstr
    :pubkey :hexstr ;; source
    :from   :addr   ;; generated from pubkey
    :rcp    :addr   ;; recpient
    :ntime  :int  ;; network time})
    :type   :int  ;; == 257
    :amount :num})
;
  
(def STATE "state")
(comment
  {
    :_id "state name"
    :ts  "ts"
    :val {}})
;

  
(defn create-indexes [db]
  (try
    (mc/create-index db ACCS (array-map :ct -1))

    (mc/create-index db TRANS (array-map :from  1))
    (mc/create-index db TRANS (array-map :rcp   1))
    (mc/create-index db TRANS (array-map :ntime 1))

    true
    ;;
    (catch Exception ex
      (warn "create-indexes:", ex))))
;

(defstate indexes
  :start
    (create-indexes (dbc)))
;


;; ;; ;; ;; ;; ;; ;; ;;


(defn clear-accs []
  (try-warn "clear-accs:"
    (mc/remove (dbc) ACCS {})))
;

(defn clear-trans []
  (try-warn "clear-trans:"
    (mc/remove (dbc) TRANS {})))
;

(defn new-acc [acc-data]
  (try
    (mc/insert (dbc) ACCS (assoc acc-data :ct (tc/now)))
    acc-data
    (catch Exception e
      (warn "new-acc:" (.getMessage e)))))
;

(defn acc-upsert [data])
  

(defn new-trans [trans-data]
  (try
    (mc/insert (dbc) TRANS trans-data)
    (catch Exception e
      (warn "new-trans:" (.getMessage e)))))
;

(defn trans-by-id [id]
  (try-warn ["trans-by-id:" id]
    (mc/find-map-by-id (dbc) TRANS id)))
; 


(defn scan-list 
  "return list of pairs [addr last-xid]"
  []
  (try-warn "scan-list:"
    (->>
      (mc/find-maps (dbc) ACCS {:noscan nil})
      (map 
        (fn [a] 
          [(:_id a) (:last_xid a)])))))
;

(defn max-xid []
  (try-warn "max-xid:"
    (->
      (mq/with-collection (dbc) TRANS
        (mq/find {})
        (mq/sort {:_id -1})
        (mq/limit 1))
      (first)
      (:_id))))
;

(defn acc-by-id [addr]
  (try-warn ["acc-by-id:" addr]
    (mc/find-map-by-id (dbc) ACCS addr)))
;

(defn acc-update [addr data]
  (try-warn ["acc-update:" addr]
    (mc/update-by-id (dbc) ACCS addr {:$set data})))
;

;; ;; ;; ;; ;; ;; ;; ;;

(defn trs-list []
  (try-warn "trs-list:"
    (mq/with-collection (dbc) TRANS
      (mq/find {})
      (mq/sort {:_id -1})
      (mq/limit 100000))))      
;

(defn addr-list []
  (try-warn "addr-list:"
    (mq/with-collection (dbc) ACCS
      (mq/find {})    ; {:noscan nil}
      (mq/sort {:ct -1})
      (mq/limit 10000))))
;


(defn trans-before-xid [dst xid]
  (try-warn ["trans-before-xid:" dst xid]
    (first 
      (mq/with-collection (dbc) TRANS
        (mq/find {:rcp dst :_id {:$lt xid}})
        (mq/sort {:_id -1})
        (mq/limit 1)))))
;

(defn trs-trace [addr]
  (loop [res [] 
         dst addr 
         xid Integer/MAX_VALUE
         limit 10000]
    (if-let [t (trans-before-xid dst xid)]
      (if (> limit 0)
        (recur 
          (conj res t)
          (:from t)
          (:_id t)
          (dec limit))
        ;;
        (do
          (warn "trs-trace: loop limit!")
          res))
      ;;
      res)))
;


;; ;; ;; ;; ;; ;; ;; ;;


(defn get-state [name]
  (try-warn ["get-state:" name]
    (:val
      (mc/find-map-by-id (dbc) STATE name))))
;

(defn update-state [name val]
  (try-warn ["update-state:" name val]
    (mc/update (dbc) STATE 
      {:_id name} 
      {:$set {:val val :ts (tc/now)}}
      {:upsert true})))
;

;;.
