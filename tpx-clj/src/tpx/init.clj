(ns tpx.init
  (:require [tpx.config :refer [config]]
            [tpx.logger :as logger]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [tpx.http :as tpx.http]
            [tpx.mqtt :as tpx.mqtt]
            [clojurewerkz.machine-head.client :as mh]
            [common.platform.connect.client :as connect.client]
            [common.platform.connect.tp :as connect.tp]
            [common.mqtt.connection :as mqtt-connection]
            [clojure.java.shell :as shell]))

(defn- system-map [extra-components]
  (let [;; logger and config are started this way so that we can ensure
        ;; things are logged as we want and that the config is loaded
        ;; for all the other modules
        core-config (component/start (tpx.config/config-manager {}))
        logger (component/start (logger/logger (:logger config)))]
    (apply component/system-map
           (into [:logger logger
                  :config core-config]
                 extra-components))))

(defonce system (atom nil))

(defn stop []
  (when-not (nil? @system)
    (log/info "Shutting TPX down")
    (try (component/stop @system)
         (catch Throwable t
           (log/error "Tried to shut down TPX. Got" t)))
    (log/info "TPX is now shut down")
    (reset! system nil)))

(defn init [& extra-components]
  (if @system
    (log/info "TPX already running")
    (do
      (log/info "Starting TPX up")
      ;; start the system
      (reset! system (component/start (system-map extra-components)))

      ;; log uncaught exceptions in threads
      (Thread/setDefaultUncaughtExceptionHandler
       (reify Thread$UncaughtExceptionHandler
         (uncaughtException [_ thread ex]
           (log/error {:what      :uncaught-exception
                       :exception ex
                       :where     (str "Uncaught exception on" (.getName thread))}))))

      ;; add shutdown hook
      (.addShutdownHook
       (Runtime/getRuntime)
       (proxy [Thread] []
         (run []
           (stop)))))))
