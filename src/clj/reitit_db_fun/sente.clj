(ns reitit-db-fun.sente
  (:require [integrant.core :as ig]
            [clojure.tools.logging :as log]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.aleph :refer [get-sch-adapter]]
            [reitit-db-fun.msg-handlers :refer [event-msg-handler]]))

(defmethod ig/init-key ::sente [key config]
  (log/info "Starting" key)
  (let [{:keys [ch-recv send-fn connected-uids
                ajax-post-fn ajax-get-or-ws-handshake-fn]}
        (sente/make-channel-socket-server! (get-sch-adapter) {:csrf-token-fn nil})]
    {:ring-ajax-post                ajax-post-fn
     :ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn
     :ch-chsk                       ch-recv ; ChannelSocket's receive channel
     :chsk-send!                    send-fn ; ChannelSocket's send API fn
     :connected-uids                connected-uids} ; Watchable, read-only atom
    ))

(defmethod ig/halt-key! ::sente [key sente-state]
  (log/info "Stopping" key))


;; Sente stuff
(comment
  (def ring-ajax-post                (:ring-ajax-post sente-state))
  (def ring-ajax-get-or-ws-handshake (:ring-ajax-get-or-ws-handshake sente-state))
  (def ch-chsk                       (:ch-chsk sente-state)) ; ChannelSocket's receive channel
  (def chsk-send!                    (:chsk-send! sente-state)) ; ChannelSocket's send API fn
  (def connected-uids                (:connected-uids sente-state)) ; Watchable, read-only atom

  ;; pod Integranta
  (def sente-router-not-yet-ig
    (sente/start-server-chsk-router! ch-chsk event-msg-handler))

  ;; żeby zatrzymać handler
  (sente-router-not-yet-ig)


  )
