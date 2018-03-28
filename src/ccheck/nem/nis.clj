(ns ccheck.nem.nis
  (:require
    [clojure.string :as s]
    [clj-http.client :as http]
    [cheshire.core :refer [parse-string]]
    ;    
    [mlib.conf :refer [conf]]
    [mlib.log :refer [debug info warn]]
    ;
    [ccheck.nem.core :refer [cleanup-addr]]))
;

(def NIS_TIMEOUT 8000)


(defn nis-node []
  (let [nodes (-> conf :nis :nodes)]
    (get nodes (rand-int (count nodes)))))
;

(defn safe-json [s]
  (try
    (parse-string s true)
    (catch Exception ignore
      s)))
;

(defn nis-get
  "returns [{result} {error}]"
  [url path params]
  (try
    (let [tout (get-in conf [:nis :timeout] NIS_TIMEOUT)
          {body :body status :status}
          (http/get (str url path)
            { :content-type     :json
              :query-params     params
              :throw-exceptions false
              :socket-timeout   tout
              :conn-timeout     tout})
          res (safe-json body)]
      (if (= 200 status)
        [res nil]
        [nil (merge {:error "http" :status status} res)]))
    (catch Exception e
      [nil {:error "exception" :message (.getMessage e)}])))
;

(defn api [method params]
  (let [[res err] (nis-get (nis-node) method params)]
    (if err
      (warn "api:" method err params)
      res)))
;


(defn heartbeat []
  (api "/heartbeat" nil))
;

(defn chain-height []
  (:height 
    (api "/chain/height" nil)))
;

(defn account [addr]
  (let [address (cleanup-addr addr)
        res (api "/account/get" {:address address})]
    (merge (:account res) (:meta res))))
;

(defn account-mosaic [addr]
  (let [address (cleanup-addr addr)]
    (first (nis-get (nis-node) "/account/mosaic/owned" {:address address}))))
;

(defn pubkey-account [pubkey]
  (let [res (api "/account/get/from-public-key" {:publicKey pubkey})]
    (merge (:account res) (:meta res))))
;

(defn account-transfers [addr out? xid]
  (let [url (if out? "/account/transfers/outgoing" "/account/transfers/incoming")
        {data :data} (api url 
                          (if xid
                            {:address addr :id xid}
                            {:address addr}))]
    ;
    (for [{mt :meta tr :transaction} data]
      { :xid    (-> mt :id)
        :hash   (-> mt :hash :data)
        :height (-> mt :height)
        :type   (:type      tr)
        :ntime  (:timeStamp tr)   ;; NEM network time
        :rcp    (:recipient tr) 
        :amount (:amount    tr)
        :pubkey (:signer    tr)})))
    ;;
;

(defn trans-seq [addr out? coll xid]
  (lazy-seq
    (let [coll (or (seq coll) 
                   (account-transfers addr out? xid))]
      (when-let [head (first coll)]
        (cons head 
            (trans-seq addr out? (rest coll) (:xid head)))))))
;

(defn out-trans [addr]
  (->>
    (trans-seq addr true nil nil)))
;

(defn in-trans [addr]
  (->>
    (trans-seq addr false nil nil)))
;

;.
