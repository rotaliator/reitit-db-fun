(ns reitit-db-fun.uuid
  (:import (com.github.f4b6a3.uuid UuidCreator)))


;; UUIDs

(defn time-ordered
  "with a static random node ID"
  []
  (UuidCreator/getTimeOrdered))

(defn time-ordered-with-mac
  "with a MAC address node ID"
  []
  (UuidCreator/getTimeOrderedWithMac))


(defn time-ordered-with-hash
  "with a hash of Hostname+MAC+IP node ID"
  []
  (UuidCreator/getTimeOrderedWithHash))


(defn time-ordered-with-random
  "with a changing random node ID"
  []
  (UuidCreator/getTimeOrderedWithRandom))
