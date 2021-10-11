(ns reitit-db-fun.main
  (:require [goog.object :as go]
            [rum.core :as rum]
            [malli.core :as m]
            [malli.error :as me]
            [ajax.core :refer [GET POST]]
            [reitit-db-fun.validations :as v]))

(defonce app-state (atom {:articles [{:title     "Tytul artykulu"
                                      :body      "Ciekawa tresc artykulu"
                                      :author-id 1}]}))


(rum/defc title < rum/static
  [text]
  [:h1 text])

(rum/defc input < rum/static
  [{:keys [type name label message value on-change]}]
  (let [inputs
        {:text
         [:.field
          [:label.label label]
          [:.control
           [:input.input {:value       value
                          :type        "text"
                          :placeholder label
                          :on-change   on-change}]]
          [:p.help.is-danger message]]

         :text-area
         [:.field
          [:label.label label]
          [:.control
           [:textarea.textarea {:name        name
                                :type        :textarea
                                :placeholder label
                                :value       value
                                :on-change   on-change}]]
          [:p.help.is-danger message]]}]
    (get inputs type)))

(rum/defc articles < rum/static
  [articles]
  (into [:div]
        (for [article articles]
          [:<>
           (input {:type :text :value (:title article)})
           (input {:type :text-area :value (:body article)})])))


(defn save-article! [article]
  (swap! app-state update :articles conj article))


;; dodać lokalny stan z {} i walidować
(rum/defcs article-form < (rum/local
                           {:form-data {:title "" :body ""}
                            :messages  {}}
                           ::article-form)
  [state]
  (let [local-state        (::article-form state)
        {:keys           [form-data messages]} @local-state
        {:keys [title body]} form-data]
    [:div
     (input {:name      :title :type :text :label "Tytuł" :value title
             :message   (get messages :title "")
             :on-change (fn [e] (swap! local-state assoc-in [:form-data :title] (-> e .-target .-value)))})
     (input {:name      :body :type :text-area :label "Treść" :value body
             :message   (get messages :body "")
             :on-change (fn [e] (swap! local-state assoc-in [:form-data :body] (-> e .-target .-value)))})
     [:button.button.is-primary
      {:on-click
       (fn [_]
         (if (m/validate v/Article {:title title :body body})
           (do
             (swap! local-state assoc :messages {})
             (save-article! {:title title :body body})
             (prn "call to backend" {:title title :body body}))
           (let [errors (-> v/Article
                            (m/explain {:title title :body body})
                            (me/humanize))]
             (swap! local-state assoc :messages {})
             (doseq [[k v] errors]
               (swap! local-state assoc-in [:messages k] v)))))}
      "Zapisz"]]))

(rum/defc app []
  [:.container
   (title "Artukuły")
   (articles (:articles @app-state))
   (article-form)])


(defn ^:dev/after-load init []
  (rum/mount (app) (js/document.getElementById "app")))

(comment

  (-> v/Article
      (m/explain {:title     "tytuł"
                  :body      "treść"
                  :author-id "1"})
      (me/humanize {:errors (-> me/default-errors
                                (assoc ::m/missing-key
                                       {:error/message "Pole wymagane"}))}))


  )
