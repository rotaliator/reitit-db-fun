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
