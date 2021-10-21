(ns reitit-db-fun.sente
  (:require [integrant.core :as ig]
            [clojure.tools.logging :as log]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.aleph :refer [get-sch-adapter]]
            #_[reitit-db-fun.msg-handlers :refer [event-msg-handler]]))

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
