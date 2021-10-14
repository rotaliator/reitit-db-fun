(ns reitit-db-fun.main
  (:require [goog.object :as go]
            [rum.core :as rum]
            [malli.core :as m]
            [malli.error :as me]
            [ajax.core :refer [GET POST]]
            [reitit-db-fun.validations :as v]

            [reitit-db-fun.db :refer [conn]]
            [datascript.core :as ds]
            [reitit-db-fun.client :as client] ;; słaba nazwa. może sente?
            ))

(defn entities-for-attr [db attribute]
  (->> (ds/datoms db :aevt attribute)
       (map :e)
       (ds/pull-many db [:*])))


(defn all-articles [db]
  (->> (ds/datoms db :aevt :article/title)
       (map :e)
       (ds/pull-many db [:*])))


(rum/defc title < rum/static
  [text]
  [:h1.title text])

(rum/defc input < rum/static
  [{:keys [type name label message value on-change read-only options]
    :or   {read-only false}}]
  (let [inputs
        {:text
         [:.field
          [:label.label label]
          [:.control
           [:input.input {:value       value
                          :type        "text"
                          :placeholder label
                          :on-change   on-change
                          :read-only   read-only}]]
          [:p.help.is-danger message]]

         :text-area
         [:.field
          [:label.label label]
          [:.control
           [:textarea.textarea {:name        name
                                :type        :textarea
                                :placeholder label
                                :value       value
                                :on-change   on-change
                                :read-only   read-only}]]
          [:p.help.is-danger message]]

         :select
         [:.field
          [:label.label label]
          [:.control
           [:.select {:name      name
                      :read-only read-only
                      :on-change on-change}
            (into [:select]
                  (for [[value text] options] [:option {:value value} text]))]]
          [:p.help.is-danger message]]}]
    (get inputs type)))

(rum/defc articles
  [db]
  (into [:div]
        (for [article (all-articles db)]
          [:<>
           (input {:type :text :value (:article/title article) :read-only true})
           (input {:type :text-area :value (:article/body article) :read-only true})
           (input {:type :text :value (->> (:article/author-id article)
                                           (ds/datoms db :eavt)
                                           first
                                           :v) :read-only true})])))

(defn save-article!
  "TODO na razie zapisuje lokalnie w bazie. Docelowo w backendzie."
  [article]
  (println "saving:" article)
  (ds/transact! conn [(merge article {:db/id -1})
                      ])
  (client/chsk-send! [:article/save! article]))

;; dodać lokalny stan z {} i walidować
(rum/defcs article-form < (rum/local
                           {:form-data {:article/title ""
                                        :article/body ""
                                        :article/author-id nil}
                            :messages  {}}
                           ::article-form)
  [state authors]
  (let [#_ "TODO dodać reaktywną listę autorów.. hmmm"
        #_ " w sumie to bez sensu trochę...."

        local-state                  (::article-form state)
        {:keys [form-data messages]} @local-state]
    [:div
     (input {:name      :title :type :text :label "Tytuł" :value (:article/title form-data)
             :message   (get messages :article/title "")
             :on-change (fn [e]
                          (swap! local-state assoc-in [:form-data :article/title]
                                 (-> e .-target .-value)))})
     (input {:name      :body :type :text-area :label "Treść" :value (:article/body form-data)
             :message   (get messages :article/body "")
             :on-change (fn [e]
                          (swap! local-state assoc-in [:form-data :article/body]
                                 (-> e .-target .-value)))})
     (input {:name      :author-id :type :select :label "Autor" :options authors
             :message   (get messages :article/author-id "")
             :on-change (fn [e]
                          (swap! local-state assoc-in [:form-data :article/author-id]
                                 (-> e .-target .-value js/parseInt)))})
     [:button.button.is-primary
      {:on-click
       (fn [_]
         (if (m/validate v/Article form-data)
           (do
             (swap! local-state assoc :messages {})
             (save-article! form-data))
           (let [errors (-> v/Article
                            (m/explain form-data)
                            (me/humanize))]
             (js/console.log (pr-str errors))
             (swap! local-state assoc :messages {})
             (doseq [[k v] errors]
               (swap! local-state assoc-in [:messages k] v)))))}
      "Zapisz"]]))

(rum/defc app < rum/reactive
  [conn]
  (let [db (rum/react conn)]
    [:.container
     (article-form (mapv (juxt :db/id :author/name) (entities-for-attr db :author/name)))
     (title "Artukuły")
     (articles db)
     ]))


(defn ^:dev/after-load init []
  (rum/mount (app conn) (js/document.getElementById "app")))

(comment

  (-> v/Article
      (m/explain {:title     "tytuł"
                  :body      "treść"
                  :author-id "1"})
      (me/humanize {:errors (-> me/default-errors
                                (assoc ::m/missing-key
                                       {:error/message "Pole wymagane"}))}))

  (save-article! {:article/title "title2" :article/body "body2"
                  :article/author-id "test"})

  (all-articles @conn)
  (entities-for-attr @conn :article/title)
  (entities-for-attr @conn :author/name)


  (mapv (juxt :db/id :author/name) (entities-for-attr @conn :author/name))
  (ds/db conn)
  )
