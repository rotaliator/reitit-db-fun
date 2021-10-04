(ns reitit-db-fun.main
  (:require [goog.object :as go]
            [rum.core :as rum]))

(rum/defc label [text]
  [:div {:class "label"} text])


(defn ^:dev/after-load init []
  (rum/mount (label "napis") (js/document.getElementById "app")))
