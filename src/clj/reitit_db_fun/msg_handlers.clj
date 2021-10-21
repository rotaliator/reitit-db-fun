(ns reitit-db-fun.msg-handlers
  "Backend!
  Metody do obsługi przychodzących komunikatów
  Komuniaty są struktury: [:ns/id {:dane \"komuniaktu\"}]
  Problem - jak użyć z tego poziomu implementacji modelu?
  event-msg-handler przyjmuje to co daje sente...
  może poprzez partial?"
  (:require [integrant.core :as ig]
            [clojure.tools.logging :as log]
            [taoensso.sente :as sente]
            [malli.core :as m]
            [malli.error :as me]
            [reitit-db-fun.validations :as v]
            [reitit-db-fun.model :as model]
            [reitit-db-fun.sente-functions :as sente-fn]))

(defmulti event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  (fn [_ _ m] (:id m)))

(defmethod event-msg-handler :article/save!
  [model sente-functions {:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/info "Saving article:" event)
  (let [article (second event)]
    (if (m/validate v/Article article)
      (do (log/info "Saving to DB!")
          (model/update-article model article)
          (sente-fn/send-datoms-to-all-clients sente-functions [[:db/add 3 :author/name "Backender" 536870913]]))
      (log/error "Error validating:" article
                 (-> v/Article
                     (m/explain article)
                     (me/humanize))))
    (when ?reply-fn
      (?reply-fn {:saving article}))))


(defmethod event-msg-handler :default
  [model sente-functions {:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/info "Unmathed msg handler for event:" event)
  (when ?reply-fn
    (?reply-fn {:unmatched-event-as-echoed-from-server event})))

(defmethod ig/init-key ::msg-handlers [key {:keys [sente-state model sente-functions]}]
  (log/info "Starting" key)
  (sente/start-server-chsk-router! (:ch-chsk sente-state) (partial event-msg-handler model sente-functions)))

(defmethod ig/halt-key! ::msg-handlers [key stop-fn]
  (log/info "Stopping" key)
  (stop-fn))

(comment
  ;;to zwraca stop-fn więc umieścić w komponencie integranta!
  ;; i na halt-key wywołać (stop-fn)

  (ns-unmap *ns* 'event-msg-handler)

  )
