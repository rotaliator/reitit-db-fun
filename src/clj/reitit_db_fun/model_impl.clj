(ns reitit-db-fun.model-impl
  (:gen-class)
  (:require [integrant.core :as ig]
            [reitit-db-fun.model]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as jdbc-sql]
            [reitit-db-fun.uuid :as uuid]
            [honey.sql :as sql]
            [honey.sql.helpers :refer [select where from left-join]]
            [reitit-db-fun.datom :as datom]))

;; SQL

(def article-query-full (-> (select :article/* #_:address/* :user/name :user/id)
                            (from   :article)
                            (left-join :user [:= :article/author :user/id])
                            #_(left-join :address [:= :user/address :address/id])))

(def article-query (-> (select :article/*)
                       (from   :article)))

(comment (sql/format article-query))

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
                                      :body   body}))
          id     (or id (get result (keyword "last_insert_rowid()")))
          _      (println "id:" id)
          #_     (if id
                   (jdbc-sql/query datasource ["select * from article where id = ?" id])
                   (jdbc-sql/query datasource ["select * from article where rowid = ?"
                                               (get result (keyword "last_insert_rowid()"))]))

          ;; na koniec pobieram resultset i przerabiam na datomy
          query     (-> article-query (where [:= :article/id id]))
          query-str (sql/format query)
          _         (println query-str)
          resultset (jdbc/execute! datasource query-str)
          _         (println resultset)]
      (datom/resultset-into-datoms resultset)))

  (get-articles [_]
    (jdbc/execute! datasource ["select * from article;"]))
  (get-article [_ article-id]
    (jdbc/execute!  datasource ["select * from article where id = ?;"
                                article-id]))
  (get-articles-datoms [_]
    (let [query     (sql/format article-query)
          resultset (jdbc/execute! datasource query)]
      (datom/resultset-into-datoms resultset))))

(defmethod ig/init-key :model/article-sql [_ {:keys [datasource]}]
  (->ArticleSQL datasource))
