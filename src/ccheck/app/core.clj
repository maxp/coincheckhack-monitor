(ns ccheck.app.core
  (:require
    [clojure.core.async :refer [thread]]
    [mount.core :refer [defstate]]
    ;    
    [mlib.conf :refer [conf]]
    [mlib.log :refer [debug info warn try-warn]]
    ;
    [ccheck.app.scan :refer [init-database scan-addresses]]))
;


(defonce runflag (atom true))


(defn start [cfg]
  (reset! runflag true)
  ;
  (when (:init-database cfg)
    (init-database))
  ;;
  (thread
    (debug "starting scanner loop")
    (loop []
      (scan-addresses)
      ;; TODO: notifications
      (when @runflag
        (Thread/sleep (* 1000 (:interval cfg)))
        (recur)))
    (debug "scanner loop stopped")))
; 

(defn stop [scanner-loop]
  (debug "stopping scanner")
  (reset! runflag false))
;

(defstate scanner-loop
  :start
    (if-let [cfg (-> conf :scan)]
      (start cfg)
      false)
  :stop
    (when scanner-loop
      (stop scanner-loop)))
;

;;.
