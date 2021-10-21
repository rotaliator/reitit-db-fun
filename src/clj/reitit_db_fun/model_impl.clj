(ns reitit-db-fun.model-impl
  (:gen-class)
  (:require [integrant.core :as ig]
            [reitit-db-fun.model]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as jdbc-sql]
            [reitit-db-fun.uuid :as uuid]))

;; SQL
(def article-query (sql/format {:select [:article/* :address/* :user/*]
                                :from   [:article :address :user]
                                :where  [:and
                                         [:= :article/id ]
                                         [:= :article/author :user/id]
                                         [:= :user/address :address/id]]}))



(defrecord ArticleSQL [datasource]
  reitit-db-fun.model/IArticle
  (update-article [_ article] ;; jeśli jest id to robię update, jeśli nie ma to insert
    (let [{:article/keys [id title author body]} article

          result (if id
                   (jdbc-sql/update! datasource :article
                                     {:title title :author author :body body}
                                     {:id id})
                   (jdbc-sql/insert! datasource :article
                                     {:title  title
                                      :author author
                                      :body   body}))]
      (if id
        (jdbc-sql/query datasource ["select * from article where id = ?" id])
        (jdbc-sql/query datasource ["select * from article where rowid = ?"
                                    (get result (keyword "last_insert_rowid()"))]))
      ;; na koniec pobieram resultset i przerabiam na datomy

      ))
  (get-articles [_]
    (jdbc/execute! datasource ["select * from article;"]))
  (get-article [_ article-id]
    (jdbc/execute!  datasource ["select * from article where id = ?;"
                                article-id])))

(defmethod ig/init-key :model/article-sql [_ {:keys [datasource]}]
  (->ArticleSQL datasource))
