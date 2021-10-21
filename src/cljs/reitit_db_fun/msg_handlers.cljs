(ns reitit-db-fun.msg-handlers
  "Frontend!
  Metody do obsługi przychodzących komunikatów
  Komuniaty są struktury: [:ns/id {:dane \"komuniaktu\"}]
  Problem - jak użyć z tego poziomu implementacji modelu?
  event-msg-handler przyjmuje to co daje sente...
  może poprzez partial?
  "
  (:require [taoensso.sente :as sente]
            [reitit-db-fun.validations :as v]
            [reitit-db-fun.db :as db]))

(defn- log [& message]
  (println (pr-str message)))

(defmulti event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  (fn [{:keys [?data]}] (first ?data)))

(defmethod event-msg-handler :datoms/save!
  [{:as ev-msg :keys [event id ?data send-fn]}]
  (log "datoms/save! handler" ?data)
  (db/save-datoms! (second ?data)))

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event id ?data send-fn]}]
  (log "default handler" ev-msg))

(comment

  (db/save-datoms! [[:db/add 1000001 :user/address "wrobla" 536870912]])
  (db/conn)
  )
