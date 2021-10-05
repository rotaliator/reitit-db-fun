(ns reitit-db-fun.storage
  (:require [integrant.core :as ig]
            [clojure.java.io :as io]
            ;; SQL
            [hikari-cp.core :as hikari-cp]
            [ragtime.jdbc]
            [ragtime.repl]))


(defmethod ig/init-key :storage/sql [_ {:keys [conn-options migrations-dir]}]
  (let [datasource (hikari-cp/make-datasource conn-options)]
    (ragtime.repl/migrate {:datastore  (ragtime.jdbc/sql-database {:connection-uri (:jdbc-url conn-options)})
                           :migrations (ragtime.jdbc/load-resources migrations-dir)})
    datasource))

(defmethod ig/halt-key! :storage/sql [_ datasource]
  (hikari-cp/close-datasource datasource))
