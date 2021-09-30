(ns reitit-db-fun.datom
  (:require [cognitect.transit :as transit]))

(defn -reduce-kv
  "Variant of reduce-kv that does not unwrap (reduced)"
  [f init coll]
  (reduce-kv #(let [result (f %1 %2 %3)]
                (cond-> result
                  ;; wrap twice because reduce-kv will unwrap one reduced
                  ;; but we want to pass that info down the line
                  (reduced? result) reduced))
             init coll))

(defn- entity->flat
  "Transducer mapujący encję w postaci mapy na listę datomów.
  Mapa z encją powinna zawierać klucz :db/id
  Do rozważenia czy generować datomy z :<namespace>/id.
  Są redundantne i zwiekszają ilość datomów.
  Wystarczy w warunku zmienić z
  (= attr :db/id) -> (= (name attr) \"id\")
  i żadnych idków nie będzie."
  [rf]
  (completing
   (fn [result entity]
     (-reduce-kv
      (fn [result attr val]
        (if (= attr :db/id)
          result
          (rf result [(:db/id entity) attr val])))
      result entity))))


(def -entity-datoms
  (comp entity->flat
        (map #(transit/tagged-value "datascript/Datom" %))))

(defn entity->datoms
  "Konwertuje pojedynczą encję na datomy.
  grupuje wg namespace klucza. wymaga aby każda tabela/namespae miało podane unikalne id
  przykład:

  {:user/id        1
   :user/name      \"John\"
   :user/email     \"j.doe@example.com\"
   :user/address   2
   :address/id     2
   :address/street \"Second Street\"
   :address/city   \"NY\"}"

  [entity]
  (into #{}
        (comp
         (map (fn [[table entity-for-table]]
                (let [id (get entity (keyword table "id"))
                      _  (when-not id
                           (throw (ex-info (format "Missing :%s/id key" table)
                                           {:table  table
                                            :entity entity-for-table})))]
                  (-> {}
                      (into entity-for-table)
                      (assoc :db/id id)))))
         entity->flat)
        (group-by (comp namespace first) entity)))

(defn resultset-into-datoms
  "Konwertuje wynik działania selecta na datomy"
  [resultset]
  (into (sorted-set) (mapcat entity->datoms) resultset))
