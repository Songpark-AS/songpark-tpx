(ns tpx.init
  (:require [com.stuartsierra.component :as component]
            [songpark.common.communication :refer [PUT]]
            [tpx.gpio :as gpio]
            [tpx.gpio.actions :as gpio.actions]
            [songpark.jam.tpx :as jam.tpx]
            [songpark.mqtt :as mqtt]
            [songpark.mqtt.util :refer [teleporter-topic]]
            [taoensso.timbre :as log]
            [tpx.config :refer [config]]
            [tpx.data :as data]
            [tpx.database :as database :refer [get-hardware-values]]
            [tpx.heartbeat :as heartbeat]
            [tpx.ipc :as ipc]
            [tpx.logger :as logger]
            [tpx.mqtt.handler.jam]
            [tpx.mqtt.handler.teleporter]
            [tpx.network :as network]))

(defonce system (atom nil))

(defn- get-device-mac []
  (:mac config))

(defn broadcast-presence [success-cb error-cb]
  (let [data (merge {:teleporter/nickname (get-in config [:ipc :teleporter :nickname])
                     :teleporter/mac (get-device-mac)
                     :teleporter/apt-version (data/get-apt-version)}
                    (get-hardware-values))
        platform-url (str (get-in config [:ipc :platform]) "/api/teleporter")]
    (log/debug "Broadcasting to URL" platform-url data)
    (PUT platform-url data success-cb error-cb)))

(defn- system-map [extra-components]
  (let [;; logger and config are started this way so that we can ensure
        ;; things are logged as we want and that the config is loaded
        ;; before all the other modules
        core-config (component/start (tpx.config/config-manager {}))
        logger (component/start (logger/logger (:logger config)))
        db (component/start (database/database (:database config)))]
    (broadcast-presence
     (fn [{:teleporter/keys [id] :as _result}]
       (log/info "Successfully reported Teleporter to Platform")
       (let [;; start mqtt-client third before anything else, so that any messaged that might be needing sending
             ;; can be done so, as mqtt-client has finished connecting
             mqtt-client (component/start (mqtt/mqtt-client (assoc-in (:mqtt config) [:config :id] id)))]
        ;; set teleporter-id for data
         (data/set-tp-id! id)
         ;; 100ms sleep to help mqtt-client
         (Thread/sleep 100)
         ;; start the rest of system
         (log/info "Starting system")
         (reset! system (component/start
                         (apply component/system-map
                                (into [:logger logger
                                       :mqtt-client mqtt-client
                                       :config core-config
                                       :gpio-manager (gpio/gpio-manager (gpio.actions/get-settings))
                                       ;; :network (network/network (:network config))
                                       :database db
                                       :ipc (component/using (ipc/ipc-service {:config (:ipc config)})
                                                             [:mqtt-client :database])
                                       :jam (component/using (jam.tpx/get-jam (merge {:tp-id id}
                                                                                     (:jam config)))
                                                             [:ipc :mqtt-client])
                                       :heartbeat (component/using (heartbeat/heartbeat-service {:config (:heartbeat config)})
                                                                   [:mqtt-client])]
                                      extra-components))))
         (log/info "System startup done"))
       ;; setup mqtt client further with injections and topics
       (let [{:keys [mqtt-client ipc jam]} @system]
         ;; injections of ipc and jam first
         (mqtt/add-injection mqtt-client :ipc ipc)
         (mqtt/add-injection mqtt-client :jam jam)
         ;; add topic of its own id
         (log/info "Subscribing to teleporter topic")
         (mqtt/subscribe mqtt-client {(teleporter-topic id) 2})))
     (fn [error]
       ;; add flashing leds to indicate a restart is required
       (log/error error)))))

(defn stop []
  (when-not (nil? @system)
    (log/info "Shutting down Songpark Teleporter")
    (try (component/stop @system)
         (catch Throwable t
           (log/error "Tried to shut down Songpark Teleporter. Got" t)))
    (log/info "Songpark Teleporter is now shut down")
    (reset! system nil)))

(defn init [& extra-components]
  (if @system
    (log/info "Songpark Teleporter already running")
    (do
      (log/info "Starting Songpark Teleporter")
      ;; start the system
      (system-map extra-components)

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


(comment

  (stop)
  (init)

  )
