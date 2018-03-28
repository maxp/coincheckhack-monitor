
(ns ccheck.app.notify
  (:require
    [mount.core :refer [defstate]]
    ;
    [mlib.conf :refer [conf]]
    [mlib.core :refer [hesc]]
    [mlib.log :refer [debug info warn try-warn]]
    [mlib.tg.core :refer [send-html]]
    ;
    [ccheck.app.db :refer [get-state update-state acc-by-id]]
    [ccheck.nem.core :refer [expl-trans expl-addr expl-block str-xem]]))
;

(def LAST_HEIGHT "last_height")

(defn ccheck-path [addr]
  (str "http://ccheck.labs.cx/?addr=" addr))

  
(defstate notified-height
  :start
    (do
      (when-let [init-height (-> conf :notify :init-height)]
        (update-state LAST_HEIGHT init-height))
      (let [h (get-state LAST_HEIGHT)]
        (info "notified-height:" h)
        h)))
;

(defn update-last-height [height]
  (update-state LAST_HEIGHT height))
;

(defn sendMsg [name amount hash from height]
  (let [apikey (-> conf :notify :apikey)
        uids   (-> conf :notify :uids)
        text   (str "To: #" (hesc name) "\n" 
                    "XEM: <a href=\"" (expl-trans hash) "\">" (str-xem amount) "</a>"
                      "  [<a href=\"" (ccheck-path from) "\">Trace</a>]\n"
                    "From: <a href=\"" (expl-addr from) "\">" from "</a>\n"
                    "Block: <a href=\"" (expl-block height) "\">" height "</a>\n")]
    ;       
    (doseq [u uids]
      (send-html apikey u text))))

;

(defn trs-notify [from tr]
  (try-warn ["trs-notify:" tr]
    (let [height   (:height tr)
          fa       (acc-by-id from)
          acc      (acc-by-id (:rcp tr))
          note     (:note acc)]
      (when (and note
              (>= height notified-height))
        (info "notify:" note (:amount tr) (:hash tr) from) 
        (sendMsg note (:amount tr) (:hash tr) from height)))))
;


;;.

