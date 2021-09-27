(ns reitit-db-fun.model-impl
  (:gen-class)
  (:require [integrant.core :as ig]
            [reitit-db-fun.model]
            [xtdb.api :as xt]
            [datalevin.core :as d]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as jdbc-sql]

            [reitit-db-fun.uuid :as uuid]))

(defn ensure-xtid [{:keys [:xt/id] :as entity}]
  (if-not id
    (assoc entity :xt/id (java.util.UUID/randomUUID))
    entity))

(defrecord ArticleXTDB [node]
  reitit-db-fun.model/IArticle
  (update-article [_ article]
    (println :update-article-xtdb article)
    (xt/submit-tx-async node [[::xt/put
                               (ensure-xtid article)]]))
  (get-articles [_]
    (println :get-articles-xtdb)
    (xt/q (xt/db node) '{:find  [(pull ?e [*])]
                         :where [[?e :article/title ?t]]
                         :limit 100}))
  (get-article [_ article-id]
    (println :get-article-xtdb article-id)
    {:article/title "test xtdb"}))

(defmethod ig/init-key :model/article-xtdb [_ {:keys [node]}]
  (->ArticleXTDB node))


(defrecord ArticleDatalevin [conn]
  reitit-db-fun.model/IArticle
  (update-article [_ article]
    (println :update-article-datalevin article)
    {:message "updated-datalevin"})
  (get-articles [_]
    (d/q '[:find (pull ?e [*])
           :where
           [?e :article/title _]]
         (d/db conn)))
  (get-article [_ article-id]
    (println :get-article-datalevin article-id)
    (d/q '[:find (pull ?e [*])
           :in $ ?article-id
           :where
           [?e :db/id ?article-id]]
         (d/db conn)
         article-id)))

(defmethod ig/init-key :model/article-datalevin [_ {:keys [conn]}]
  (->ArticleDatalevin conn))

;; SQL

(defrecord ArticleSQL [datasource]
  reitit-db-fun.model/IArticle
  (update-article [_ article] ;; jeśli jest id to robię update, jeśli nie ma to insert
    (let [{:article/keys [id title author]} article

          result (if id
                   (jdbc-sql/update! datasource :article
                                     {:title title :author author}
                                     {:id id})
                   (jdbc-sql/insert! datasource :article
                                     {:id     (str (uuid/time-ordered-with-random))
                                      :title  title
                                      :author author}))]
      (if id
        (jdbc-sql/query datasource ["select * from article where id = ?" id])
        (jdbc-sql/query datasource ["select * from article where rowid = ?"
                                    (get result (keyword "last_insert_rowid()"))]))))
  (get-articles [_]
    (jdbc/execute! datasource ["select * from article;"]))
  (get-article [_ article-id]
    (jdbc/execute!  datasource ["select * from article where id = ?;"
                                article-id])))

(defmethod ig/init-key :model/article-sql [_ {:keys [datasource]}]
  (->ArticleSQL datasource))
