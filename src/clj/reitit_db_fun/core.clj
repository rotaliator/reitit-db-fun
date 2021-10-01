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

  ;; przygotowuję dane.
  ;; Przykładowe adresy
  (let [datasource (:storage/sql @main-system)
        query      (sql/format {:insert-into [:address]
                                :columns     [:street :city]
                                :values      [["Wróbla" "Puławy"]
                                              ["Kołłątaja" "Puławy"]
                                              ["Jaworowa" "Puławy"]
                                              ["Prusa" "Puławy"]]}
                               {:pretty true})]
    (->> (jdbc/execute! datasource query)))
  ;; przykładowi userzy

  (let [datasource (:storage/sql @main-system)
        addresses  (->> {:select :* :from :address}
                        sql/format
                        (jdbc/execute! datasource)
                        (mapv :address/id))
        query      (sql/format {:insert-into [:user]
                                :columns     [:name :address]
                                :values      [["Wacław" (rand-nth addresses)]
                                              ["Fred" (rand-nth addresses)]
                                              ["Ewa" (rand-nth addresses)]
                                              ["Adam" (rand-nth addresses)]
                                              ["Szczepan" (rand-nth addresses)]]}
                               {:pretty true})]
    (->> (jdbc/execute! datasource query)))

  ;; tworzę artykuły, tym razem  poprzez api
  (let [datasource (:storage/sql @main-system)
        authors    (->> {:select :* :from :user}
                        sql/format
                        (jdbc/execute! datasource)
                        (mapv :user/id))
        app        (:app/handler @main-system)]
    (doseq [idx (range 100)]
      (app {:request-method :post
            :uri            "/api/article"
            :body-params    {:article/title  (str "Test-" (inc idx))
                             :article/body   (str "Treść artykułu " (inc idx))
                             :article/author (rand-nth authors)}})))
  ;; sprawdzam pojedynczy artykuł
  (-> ((:app/handler @main-system)
       {:request-method :get
        :uri            "/api/article/3000001"})
      (update :body slurp))

  ;; aktualizacja danych
  (-> ((:app/handler @main-system)
       {:request-method :post
        :uri            "/api/article"
        :body-params    {:article/id     "3000001"
                         :article/title  "Title zmieniony ponownie"
                         :article/body   "Treść po zmiania"
                         :article/author "pkoza"}})
      (update :body slurp))

  ;; wszystkie arty
  (-> ((:app/handler @main-system)
       {:request-method :get
        :uri            "/api/articles"})
      :body
      slurp)

  ;; Basa SQL na Datomy!

  (def initial-datoms
    (let [datasource (:storage/sql @main-system)
          query      (sql/format {:select [:article/* :address/* :user/*]
                                  :from   [:article :address :user]
                                  :where  [:and
                                           [:= :article/author :user/id]
                                           [:= :user/address :address/id]]
                                  :limit  20})]
      (->> (jdbc/execute! datasource query)
           datom/resultset-into-datoms)))
  ;; Datascript test db
  (def test-db (db/init-db (mapv #(apply d/datom %) initial-datoms)))

  (d/q '[:find (pull ?e [* {:article/author [:user/name {:user/address [*]}]}])
         :where [?e :article/id _]]
       test-db)


  (do
    (stop-system main-system)
    (start-system main-system config))

  )
