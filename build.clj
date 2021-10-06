(ns build
  (:require [clojure.tools.build.api :as b]
            [shadow.cljs.devtools.cli]
            [hf.depstar]))

(defn clean [_]
  (b/delete {:path "public"}))

(defn cljs [_]
  (shadow.cljs.devtools.cli/-main "release" "app"))

(defn uber [_]
  (hf.depstar/uberjar {:jar        "reitit-db-run.jar"
                       :aot        true
                       :main-class 'reitit-db-fun.main
                       :exclude    ["config.edn"]}))

(defn all [_]
  (cljs nil)
  (uber nil))
