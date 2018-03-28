(ns ccheck.web.srv
  (:require
    ; [clojure.string :as s]
    ; [clj-time.core :as tc]
    ; [clojure.core.async :refer [chan <!! alts!! close! thread timeout]]
    [ring.adapter.jetty :refer [run-jetty]]
    [compojure.core :refer [GET routes]]
    [compojure.route :refer [resources not-found]]
    [mount.core :refer [defstate]]
    [mlib.conf :refer [conf]]
    [mlib.log :refer [debug info warn]]
    [mlib.web.middleware :refer [middleware]]
    [ccheck.web.handlers :refer [root addrs trans]]))
;


(defn make-routes []
  (routes
    (GET "/"        [] root)
    (GET "/addrs"   [] addrs)
    (GET "/trans"   [] trans)
;    (GET "/black-list" [] black-list)
    (resources "/")
    (not-found "Page not found.")))
;


(defstate web-server
  :start
    (if-let [cnf (:http conf)]
      (do
        (info "web-server:" cnf)
        (->
          (make-routes)
          (middleware)
          (run-jetty cnf)))
      (warn "web-server disabled."))
  :stop
    (when web-server
      (debug "web-server stop.")
      (.stop web-server)))
;

;;.
