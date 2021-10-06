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

;; dodać lokalny stan z {} i walidować
(rum/defc article-form < rum/static
  []
  [:form {:on-submit (fn [e] (.preventDefault e) (js/console.log e))}
   [:input.input {:name :title :type :text :placeholder "Tytuł"}]
   [:textarea.textarea {:name :body :type :textarea :placeholder "Treść"}]
   [:button.button.is-primary {:type :submit} "Zapisz"]])


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
                  :body      "eść"
                  :author-id "1"})
      (me/humanize {:errors (-> me/default-errors
                                (assoc ::m/missing-key
                                       {:error/message "Pole wymagane"}))}))

  )
