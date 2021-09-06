(ns tpx.init
  (:require ;! Fundamentals
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
<<<<<<< HEAD
            #_[common.platform.connect.tp :as connect.tp]
=======
>>>>>>> cd659b4154d9e0ceec8ffd0a818f041a6e0c3f01
            ;! TPX
            [tpx.audio :as tpx.audio]
            #_[tpx.config :as tpx.config]
            [tpx.config :refer [config]]
            [tpx.http :as tpx.http]
            [tpx.ipc :as tpx.ipc]
            [tpx.logger :as logger]
            [tpx.mqtt :as tpx.mqtt]))
<<<<<<< HEAD

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

=======

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

(defn initiate-communications
  "Initiates communications with the backend,
   telling the backend its tpID,
   then initiates communications to MQTT's pub/sub"
  [handler-map]
  (let [tpid (tpx.ipc/retrieve-tpID)
        plat-response (connect.tp/init {:tpid tpid})
        uuid (:uuid plat-response)
        status (:status plat-response)]
    (if (and status uuid)
      (let [conn-map (tpx.mqtt/common-connect-init uuid)]
        (prn "Here be the conn-map from init arrrr!: " conn-map)
        (reset! tpx.http/global-conn-map conn-map)
        (prn "Here be global conn-map: " @tpx.http/global-conn-map)
        ;; (mqtt-connection/subscribe conn-map handler-map))
        (tpx.mqtt/common-subscribe conn-map handler-map))
      (println "Teleporter connection to platform, failed"))
    (println "This is uuid: " uuid)))

>>>>>>> cd659b4154d9e0ceec8ffd0a818f041a6e0c3f01
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
