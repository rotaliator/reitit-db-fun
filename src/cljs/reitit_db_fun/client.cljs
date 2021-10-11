(ns reitit-db-fun.client
  (:require [clojure.core.async :as async]))

(def test-chan (async/chan))

(async/go-loop []
  (when-some [d (async/<! test-chan)]
    (println d)
    (recur)))

(comment

  (async/go
    (async/>! test-chan "test1"))

  (async/close! test-chan)

  )
