(ns reitit-db-fun.main
  (:require [goog.object :as go]
            [rum.core :as rum]
            [malli.core :as m]
            [malli.error :as me]
            [reitit-db-fun.validations :as v]))

(defonce app-state (atom {:articles [{:title "x"
                                      :body "y"
                                      :author-id "a"}]}))


(rum/defc title [text]
  [:h1 text])

(rum/defc articles < rum/static
  [articles]
  (into [:ul]
        (for [article articles]
          [:li (:title article)])))

(rum/defc input < rum/static
  [{:keys [type name label message value on-change]}]
  (let [inputs
        {:text
         [:div.field
          [:label.label label]
          [:div.control
           [:input.input {:value       value
                          :type        "text"
                          :placeholder label
                          :on-change   on-change}]]
          [:p.help.is-danger message]]
         :text-area
         [:div.field
          [:label.label label]
          [:div.control
           [:textarea.textarea {:name        name
                                :type        :textarea
                                :placeholder label
                                :value       value
                                :on-change   on-change}]]
          [:p.help.is-danger message]]
         }]
    (get inputs type)))


;; dodać lokalny stan z {} i walidować
(rum/defcs article-form < (rum/local
                           {:title "" :title-message ""
                            :body  "" :body-message  ""}
                           ::article-form)
  [state]
  (let [local-state            (::article-form state)
        {:keys [title body
                title-message
                body-message]} @local-state]
    [:div
     (input {:name      :title :type :text :label "Tytuł" :value title
             :message title-message
             :on-change (fn [e] (swap! local-state assoc :title (-> e .-target .-value)))})
     (input {:name      :body :type :text-area :label "Treść" :value body
             :message body-message
             :on-change (fn [e] (swap! local-state assoc :body (-> e .-target .-value)))})
     [:button.button.is-primary {:on-click
                                 (fn [_]
                                   (if (m/validate v/Article {:title title :body body})
                                     (prn "call to backend" {:title title :body body})
                                     (let [errors (-> v/Article
                                                      (m/explain {:title title :body body})
                                                      (me/humanize))]
                                       (doseq [[k v] errors]
                                         (let [msg-key (-> k name (#(str % "-message")) keyword)]
                                           (swap! local-state assoc msg-key v))))))}
      "Zapisz"]]))

(rum/defc app []
  [:div.container
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
  (js/alert "asdsd")

  )
