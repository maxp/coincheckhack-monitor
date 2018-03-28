
(ns ccheck.web.handlers
  (:require
    [clojure.string :as s]
    [mlib.conf :refer [conf]]
    [mlib.core :refer [hesc]]

    [ccheck.web.html :refer [render]]
    [ccheck.app.db :refer [trs-list trs-trace addr-list acc-by-id]]
    [ccheck.nem.core :refer [str-ntime str-xem cleanup-addr XEM_DIVISOR expl-addr expl-trans]]
    [ccheck.nem.nis :refer [account]]))
;

(defn xem-fmt [xem]
  (format "%18.6f" (double (/ (or xem 0) XEM_DIVISOR))))
;

(defn root [req]
  (let [addr (-> req :params :addr str s/trim not-empty)
        address (cleanup-addr addr)
        acc  (acc-by-id address)
        note (:note acc)
        trace (when addr 
                (-> address
                  (trs-trace)
                  (not-empty)))]  
    (render req {}
      [:h1 "Coincheck theft trace"]
      [:hr]
      [:div.row.justify-content-center
        [:div.col-10
          [:form {:action "." :method "GET"}
            [:div.form-group
              [:label {:for "addr"} "Address:"]
              [:input#addr.form-control {:type "text" :name "addr" :value addr}]]
            (when note
              [:div.form-group {:style "margin-left: 1ex"} 
                (str "Note: " (hesc note))])
            [:div.form-group
              [:button.btn.btn-info "Check"]]]]]
      ;;
      (when addr
        (list 
          [:hr]
          [:div.row.justify-content-center
            [:div.col-10
              (if trace
                [:div
                  [:pre
                    (for [t trace 
                          :let [from   (:from t) 
                                amount (:amount t) 
                                hash   (:hash t)
                                acc    (acc-by-id from)]]
                      [:div 
                        [:a {:href (expl-addr from) :target "_blank"} from]
                        " "
                        [:a {:href (expl-trans hash) :target "_blank"} (xem-fmt amount)]
                        " "
                        (-> acc :note hesc)
                        "\n"])]]
                      ;;
                ;;
                [:div.text-center {:style "color: #0a0;"}
                  [:b "Good!"]])]]))
      ;;
      [:hr]
      [:ul
        [:li [:a {:href "/trans"} "trans"]]
        [:li [:a {:href "/addrs"} "addrs"]]]
      [:hr])))
;

(defn trans [req]
  (let [trs (trs-list)]
    (render req {} 
      [:br]
      [:h2 "Transactions"]
      [:div
        (count trs)
        [:hr]
        [:div
          [:pre
            (for [t trs
                  :let [from  (:from t)
                        rcp   (:rcp t)
                        racc  (acc-by-id rcp)
                        rnote (:note racc)]]
              (list 
                [:a {:href (str "/?addr=" rcp)} (str-ntime (:ntime t))]
                " "
                [:a {:href (expl-trans (:hash t)) :target "_blank"} (xem-fmt (:amount t))]
                "  "
                [:a {:href (expl-addr from) :target "_blank"} from]
                " -> "
                [:a {:href (expl-addr rcp) :title rnote :target "_blank"} rcp]
                (when rnote " *")
                "\n"))]]]
      ;
      [:hr])))
;

(defn addrs [req]
  (let [addrs (addr-list)]
    (render req {} 
      [:br]
      [:h2 "Addresses"]
      [:div
        (count addrs)
        [:hr]
        [:div
          [:pre
            (for [t addrs 
                  :let [addr  (:_id t) 
                        acc   (account addr)
                        dbacc (acc-by-id addr)
                        note  (:note dbacc)]]
              (list 
                [:a {:href (expl-addr addr) :target "_blank"} addr]
                " "
                [:a {:href (str "/?addr=" addr)} (xem-fmt (:balance acc 0))]
                (when note
                  (str " " (hesc note)))
                "\n"))]]]
      ;
      [:hr])))
;

;(defn black-list [req]
;   (let [res {}] ;(:res @scan-state)]
;     (json-resp
;       { :ok     true
;         :amount (:amount res)
;         :count  (:count  res)
;         :addrs  (map (fn [w] (str (:addr w))) (:tlist res))})))
; ;
      
;;.
