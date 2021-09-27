(ns reitit-db-fun.datom-test
  (:require [clojure.test :refer [deftest testing is]]
            [reitit-db-fun.datom :as datom]))


(deftest entity->datoms
  (testing "Creating datoms from entity"
    (let [entity {:user/id    1
                  :user/name  "John"
                  :user/email "j.doe@example.com"}
          datoms #{[1 :user/name "John"] [1 :user/email "j.doe@example.com"] [1 :user/id 1]}]
      (is (= datoms (datom/entity->datoms entity)))))
  (testing "Creating datoms from multi table entity"
    (let [entity {:user/id        1
                  :user/name      "John"
                  :user/email     "j.doe@example.com"
                  :user/address   2
                  :address/id     2
                  :address/street "Second Street"
                  :address/city   "NY"}
          datoms #{[1 :user/name "John"] [1 :user/email "j.doe@example.com"] [1 :user/address 2]
                   [1 :user/id 1]
                   [2 :address/street "Second Street"] [2 :address/city "NY"]
                   [2 :address/id 2]}]
      (is (= datoms (datom/entity->datoms entity)))))

  (testing "Throws exception when lack of :<namespace>/id key"
    (let [entity {:user/name      "John"
                  :user/email     "j.doe@example.com"}]
      (is (thrown? Exception (datom/entity->datoms entity))))))
