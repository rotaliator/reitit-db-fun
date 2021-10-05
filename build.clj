(ns build
  (:require [clojure.tools.build.api :as b]))

(defn clean [_]
  (b/delete {:path "public"}))

(defn cljs [_]
  (b/process {:command-args ["powershell.exe" "clojure" "-M:cljs" "release" "app"]}))

(defn uber [_]
  (b/process {:command-args ["powershell.exe" "clojure" "-X:uberjar"]}))

(defn all [_]
  (cljs nil)
  (uber nil))
