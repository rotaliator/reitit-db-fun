(ns reitit-db-fun.model
  (:gen-class))

(defprotocol IArticle
  (update-article [_ article])
  (get-articles [_])
  (get-article [_ article-id]))
