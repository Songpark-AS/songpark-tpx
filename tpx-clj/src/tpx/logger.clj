(ns tpx.logger
  (:require [com.stuartsierra.component :as component]
            [me.raynes.fs :as fs]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.3rd-party.rotor :refer [rotor-appender]]
            [taoensso.timbre.appenders.3rd-party.sentry :refer [sentry-appender]]
            [tpx.config :refer [config]]))


(defrecord Logger [started? sentry-settings]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (let [dir (if-let [data-dir (get-in config [:os :data-dir])]
                  (str data-dir "logs")
                  "logs")
            level (get-in config [:logger :level] :info)]
        (log/debug :dir dir)
        ;; make sure the log directory exists
        (fs/mkdir dir)
        ;; set level of logging
        (log/set-level! level)
        (log/info "Set level of logging to" level)
        ;; add our appenders
        (log/merge-config! {:appenders (merge
                                        {:rotor (rotor-appender {:path (str dir "/tpx.log")
                                                                 :backlog 100})}
                                        (if (:log? sentry-settings)
                                          {:raven (sentry-appender (:dsn sentry-settings))}))})
        (log/info "Starting Logger")
        (assoc this
               :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping Logger")
          (assoc this
                 :started? false)))))


(defn logger [settings]
  (map->Logger settings))
