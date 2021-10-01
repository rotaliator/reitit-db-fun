(ns reitit-db-fun.storage
  (:require [integrant.core :as ig]
            [clojure.java.io :as io]
            ;;XTDB
            [xtdb.api :as xt]
            ;;Datalevin
            [datalevin.core :as d]
            ;; SQL
            [hikari-cp.core :as hikari-cp]
            [ragtime.jdbc]
            [ragtime.repl]))


;; ===== XTDB ========
(defn start-xtdb! []
  (letfn [(kv-store [dir]
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir      (io/file dir)
                        :sync?       true}})]
    (xt/start-node
     {:xtdb/tx-log         (kv-store "data/dev/tx-log")
      :xtdb/document-store (kv-store "data/dev/doc-store")
      :xtdb/index-store    (kv-store "data/dev/index-store")})))


(defmethod ig/init-key :storage/xtdb [_ opts]
  #_(xt/start-node opts)
  (start-xtdb!))

(defmethod ig/halt-key! :storage/xtdb [_ node]
  (.close node))

;; Datalevin

(defmethod ig/init-key :storage/datalevin [_ {:keys [uri schema]}]
  (d/get-conn uri schema))

(defmethod ig/halt-key! :storage/datalevin [_ conn]
  (d/close conn))

(defmethod ig/init-key :storage/sql [_ {:keys [conn-options migrations-dir]}]
  (let [datasource (hikari-cp/make-datasource conn-options)]
    (ragtime.repl/migrate {:datastore  (ragtime.jdbc/sql-database {:connection-uri (:jdbc-url conn-options)})
                           :migrations (ragtime.jdbc/load-resources migrations-dir)})
    datasource))

(defmethod ig/halt-key! :storage/sql [_ datasource]
  (hikari-cp/close-datasource datasource))
