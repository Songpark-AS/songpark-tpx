(ns tpx.database
  (:require [codax.core :as codax]
            [com.stuartsierra.component :as component]
            [tpx.config :refer [config]]
            [taoensso.timbre :as log]))

(defonce db (atom nil))

(defrecord Database [started?]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting Database")
          (reset! db (codax/open-database! (str (get-in config [:os :data-dir]) "songpark.db")))
          (assoc this
                 :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping Database")
          (codax/close-database! @db)
          (reset! db nil)
          (assoc this
                 :started? false)))))


(defn database [settings]
  (map->Database settings))
