(ns reitit-db-fun.db
  (:require [datascript.core :as ds]))

(def schema {})

(def initial-data
  [{:db/id       -1
    :author/name "Lew Tolstoj"}
   {:db/id       -2
    :author/name "Henryk Sienkiewicz"}])

(defonce conn (let [conn (ds/create-conn schema)]
                (ds/transact! conn initial-data)
                conn))

(defn- add-db-add
  "dodaje :db/add do datomów.... TODO TOFIX"
  [datoms]
  (into [] (map (partial concat [:db/add])) datoms))

(defn save-datoms! [datoms]
  (ds/transact! conn (add-db-add datoms)))

(comment

  (ds/datoms @conn :eavt)

  )
