(ns tpx.config
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [cprop.core :refer [load-config]]
            [cprop.source :refer [from-system-props from-env]]
            [me.raynes.fs :as fs]
            [taoensso.timbre :as log]))

(defonce config nil)

(defrecord ConfigManager [started? config-path]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting ConfigManager")
          (log/info "Loading configuration")
          (let [path (or config-path "config.edn")]
            (alter-var-root #'config (fn [_]
                                       (load-config
                                        :resource path
                                        :file (if (fs/exists? path)
                                                path)
                                        :merge [(from-env)]))))
          (assoc this
                 :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping ConfigManager")
          (log/info "Unloading configuration")
          (alter-var-root #'config (fn [_] nil))
          (assoc this
                 :started? false)))))



(defn dev? [] (= (:mode config) :dev))
(defn prod? [] (= (:mode config) :prod))

(defn config-manager [settings]
  (map->ConfigManager settings))
