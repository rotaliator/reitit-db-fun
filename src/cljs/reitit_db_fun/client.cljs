(ns reitit-db-fun.client
  (:require [clojure.core.async :as async
             :refer [go go-loop <! >! put! chan]]
            [taoensso.sente  :as sente :refer [cb-success?]]
            [reitit-db-fun.msg-handlers :refer [event-msg-handler]]))

;;TODO
(def ?csrf-token
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))


(defonce sente-state
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket-client! "/chsk" ?csrf-token {:type :auto})]
    {:chsk       chsk
     :ch-chsk    ch-recv ; ChannelSocket's receive channel
     :chsk-send! send-fn ; ChannelSocket's send API fn
     :chsk-state state}   ; Watchable, read-only atom
    ))

(def chsk       (:chsk sente-state))
(def ch-chsk    (:ch-chsk sente-state)) ; ChannelSocket's receive channel
(def chsk-send! (:chsk-send! sente-state)) ; ChannelSocket's send API fn
(def chsk-state (:chsk-state sente-state))   ; Watchable, read-only atom


(defonce handler
  (sente/start-client-chsk-router! ch-chsk event-msg-handler))

(comment
  (chsk-send! [:article/save! {:test7 "Hello from frontend!"}]
              1000)

  (+ 1 1)


  )
