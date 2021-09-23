(ns tpx.init
  (:require ;! Fundamentals
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            #_[common.platform.connect.tp :as connect.tp]
            ;! TPX
            [tpx.logger :as logger]
            #_[tpx.config :as tpx.config]
            [tpx.config :refer [config]]
            [tpx.ipc :as tpx.ipc]
            #_[tpx.http :as tpx.http]
            [tpx.audio :as tpx.audio]))


;! --- Sindre's and my stuff ---
(def handler-map
  {:tpx-unit {
              ;! Gains
              :adjust-gain-input       tpx.audio/adjust-gain-input
              :adjust-gain-musician    tpx.audio/adjust-gain-musician
              ;! Mute states
              :toggle-mute-musician    tpx.audio/toggle-mute-musician
              :toggle-mute-unit        tpx.audio/toggle-mute-unit
              ;! Volumes
              :adjust-volume-musician  tpx.audio/adjust-volume-musician
              :adjust-volume-unit      tpx.audio/adjust-volume-unit
              ;! Other effects
              :adjust-dsp-effects      tpx.audio/adjust-dsp-effects
              :toggle-phantom-power    tpx.audio/toggle-phantom-power
              ;! Other
              :audio-file-management   tpx.ipc/audio-file-management
              :toggle-audio-recording  tpx.ipc/toggle-audio-recording
              }
  ;;  :phone-app  { :dummy dumb }; Not used in tpx
   })


(defonce system (atom nil))

;! --- Daniel's stuff
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

(defn stop []
  (when-not (nil? @system)
    (log/info "---Shutting TPX down---")
    (try (component/stop @system)
         (catch Throwable t
           (log/error "Tried to shut down TPX. Got:" t)))
    (log/info "===TPX is now shut down===")
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
