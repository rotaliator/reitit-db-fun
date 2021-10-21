(ns reitit-db-fun.sente-functions
  (:require [integrant.core :as ig]
            [clojure.tools.logging :as log]))

(defprotocol IToClient
  (send-datoms-to-all-clients [_ datoms]))

(defrecord ToClient [sente-state]
  IToClient
  (send-datoms-to-all-clients [_ datoms]
    (let [chsk-send! (:chsk-send! sente-state)
          clients    (:any @(:connected-uids sente-state))]
      (doseq [client clients]
        (log/info :sending-datoms-to client)
        (chsk-send! client [:datoms/save! datoms])))))

(defmethod ig/init-key ::sente-functions [key {:keys [sente-state model] :as config}]
  (log/info "Starting" key)
  (->ToClient sente-state))

(defmethod ig/halt-key! ::sente-functions [key _]
  (log/info "Stopping" key))
