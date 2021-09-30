(ns reitit-db-fun.core
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]
            [aleph.http :as http]
            [reitit.ring :as ring]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.coercion.malli]
            [reitit.coercion :as coercion]
            [clojure.tools.logging :as log]

            [reitit-db-fun.model]
            [reitit-db-fun.model-impl]
            [reitit-db-fun.storage]
            ;; XTDB
            [xtdb.api :as xt]

            ;; Datalevin
            ;;[datalevin.core :as d]

            ;; JDBC
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as jdbc-sql]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]

            ;; Datoms
            [reitit-db-fun.datom :as datom]

            ;; UUID
            [reitit-db-fun.uuid :as uuid]

            ;; Datascript
            [datascript.core :as d]
            [datascript.db :as db]))

;; ==== Config ====

(def config {:app/handler {:keys-to-wrap
                           {#_#_:node (ig/ref :storage/xtdb)
                            :model    (ig/ref #_:model/article-datalevin
                                              :model/article-sql
                                              #_:model/article-xtdb)}}

             :model/article-xtdb      {:node (ig/ref :storage/xtdb)}
             :model/article-datalevin {:conn (ig/ref :storage/datalevin)}
             :model/article-sql       {:datasource (ig/ref :storage/sql)}

             :adapter/aleph {:port    8080
                             :handler (ig/ref :app/handler)}

             :storage/xtdb      {}
             :storage/datalevin {:uri    "datalevin.db"
                                 :schema {}}
             :storage/sql       {:conn-options   {:jdbc-url "jdbc:sqlite:database.sqlite"}
                                 :migrations-dir "migrations"}})


(defn wrap-keys
  [keys-to-wrap]
  (fn [handler]
    (fn [request]
      (handler (merge request keys-to-wrap)))))

(defn get-article-handler
  [{:keys [model path-params]}]
  (println "DEBUG:" (pr-str path-params))
  ;; TODO proper coercion
  {:body (reitit-db-fun.model/get-article model (:article-id path-params))})

(defn get-articles-handler [{:keys [model]}]
  {:body (reitit-db-fun.model/get-articles model)})

(defn update-article-handler [{:keys [model body-params]}]
  {:body (reitit-db-fun.model/update-article model body-params)})


(defn get-app-handler [{:keys [keys-to-wrap]}]
  (ring/ring-handler
   (ring/router
    ["/api"
     ["/articles"
      {:get get-articles-handler}]
     ["/article"
      {:post update-article-handler}]
     ["/article/:article-id"
      {:get get-article-handler}]
     ["/ping"
      {:get {:handler (fn [req]
                        {:status 200
                         :body   {:message "pong"
                                  :request (pr-str req)}})}}]
     ["/status"
      {:get {:handler (fn [req]
                        {:status 200
                         :body   {:message "status"
                                  :model   (pr-str (:model req))}})}}]]
    ;; router data affecting all routes
    {:data {:muuntaja   m/instance
            :middleware [muuntaja/format-middleware
                         muuntaja/format-response-middleware
                         (wrap-keys keys-to-wrap)]}})))

(defonce main-system (atom nil))

(defmethod ig/init-key :adapter/aleph [_ {:keys [handler port]}]
  (http/start-server handler {:port port}))

(defmethod ig/halt-key! :adapter/aleph [_ server]
  (.close server))



(defmethod ig/init-key :app/handler [_ {:keys [keys-to-wrap]}]
  (get-app-handler {:keys-to-wrap keys-to-wrap}))

(defn start-system [system-atom config]
  (log/info "Starting system")
  (let [system @system-atom]
    (when-not system
      (ig/load-namespaces config)
      (reset! system-atom (ig/init config)))))

(defn stop-system [system-atom]
  (log/info "Stopping system")
  (let [system @system-atom]
    (when system
      (reset! system-atom (ig/halt! system)))))

(defn -main [_]
  (start-system main-system config))


(defn restart-system []
  (stop-system main-system)
  (start-system main-system config))


(comment
  (restart-system)


  (-> ((:app/handler @main-system)
       {:request-method :post
        :uri            "/api/article"
        :body-params    {#_#_:xt/id      1
                         :article/id     "3ba51497-4a08-4e48-9b2c-ec4c88930da7"
                         :article/title  "Title zmieniony ponownie"
                         :article/author "pkoza"
                         :article/gerne  "blog"}})
      :body
      slurp
      )

  (-> ((:app/handler @main-system)
       {:request-method :get
        :uri            "/api/article/1"})
      :body
      slurp)

  (-> ((:app/handler @main-system)
       {:request-method :get
        :uri            "/api/articles"})
      :body
      slurp)


  (let [app (:app/handler @main-system)]
    (doseq [idx (range 1000)]
      (app {:request-method :post
            :uri            "/api/article"
            :body-params    {:article/title   (str "Test-" (inc idx))
                             :article/author  "pkoza"
                             :article/address (rand-nth addresses)}})))

  (let [node (:storage/xtdb @main-system)
        id   1]
    (xt/q (xt/db node)
          '{:find  [(pull ?e [*])]
            :in    [id]
            :where [[?e :xt/id id]]}
          id))

  (-> (:model/article-datalevin @main-system)
      (reitit-db-fun.model/get-articles {}))

  ;; JDBC
  (->
   (let [datasource                        (:storage/sql @main-system)
         {:article/keys [id title author]} {:article/id     "9898906f-ce7f-42ad-a66d-7173b2c2bd03"
                                            :article/title  "Tytuł zmieniony"
                                            :article/author "tester!"}

         result (if id
                  (jdbc-sql/update! datasource :articles
                                    {:title title :author author}
                                    {:id id})
                  (jdbc-sql/insert! datasource :articles
                                    {:id     (str (java.util.UUID/randomUUID))
                                     :title  title
                                     :author author}))]
     (if id
       (jdbc-sql/query datasource ["select * from articles where id = ?" id])
       (jdbc-sql/query datasource ["select * from articles where rowid = ?"
                                   ((keyword "last_insert_rowid()")) result]))))

  (time (count (let [datasource (:storage/sql @main-system)
                     query      (sql/format {:select [:article/* :address/*]
                                             :from   [:article :address]
                                             :where  [:= :address/id :article/address]
                                             :limit  10})]
                 (->> (jdbc/execute! datasource query)
                      datom/resultset-into-datoms))))


  (let [datasource (:storage/sql @main-system)
        query      (sql/format {:insert-into [:address]
                                :columns     [:id :street :city]
                                :values      [[(str (uuid/time-ordered-with-random)) "Wróbla" "Puławy"]
                                              [(str (uuid/time-ordered-with-random)) "Kołłątaja" "Puławy"]
                                              [(str (uuid/time-ordered-with-random)) "Jaworowa" "Puławy"]
                                              [(str (uuid/time-ordered-with-random)) "Prusa" "Puławy"]]}
                               {:pretty true})]
    (->> (jdbc/execute! datasource query)))
  (let [datasource (:storage/sql @main-system)]
    (->> (jdbc/execute! datasource (sql/format {:select :* :from :address}))
         datom/resultset-into-datoms))

  (def addresses (let [datasource (:storage/sql @main-system)]
                   (->> {:select :* :from :address}
                        sql/format
                        (jdbc/execute! datasource)
                        (mapv :address/id))))

  (def initial-data (let [datasource (:storage/sql @main-system)
                          query      (sql/format {:select [:article/* :address/*]
                                                  :from   [:article :address]
                                                  :where  [:= :address/id :article/address]
                                                  :limit  10})]
                      (->> (jdbc/execute! datasource query)
                           datom/resultset-into-datoms)))

  (def test-db
    (let [db (-> (d/empty-db)
                 (d/db-with initial-data))]
      db))

  (def db2 (read-string s))

  )
