(ns build
  (:require [clojure.tools.build.api :as b]))

(def os (if (->> (System/getProperty "os.name")
                 (re-matches #"(?i)win.*"))
          :windows
          :other))

(defn clean [_]
  (b/delete {:path "public"}))

(defn cljs [_]
  (let [command ["clojure" "-M:cljs" "release" "app"]
        command (if (= os :windows)
                  (into ["powershell.exe"] command)
                  command)]
    (b/process {:command-args command})))

(defn uber [_]
  (let [command ["clojure" "-X:uberjar"]
        command (if (= os :windows)
                  (into ["powershell.exe"] command)
                  command)]
    (b/process {:command-args command})))

(defn all [_]
  (cljs nil)
  (uber nil))
