(ns reitit-db-fun.validations
  (:require [malli.core :as m]
            [malli.error :as me]))

(def Article [:map
              [:title [:string {:min 2}]]
              [:body [:string {:min 5}]]
              [:author-id :int]])

(comment
  (m/validate Article
              {:title "tytuł"})
  (-> Article
      (m/explain {:title     "tytuł"
                  :body      "treść"
                  :author-id 1})
      (me/humanize {:errors (-> me/default-errors
                                (assoc ::m/missing-key
                                       {:error/message "Pole wymagane"}))}))

  )
