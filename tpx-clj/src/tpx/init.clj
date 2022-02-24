(ns tpx.init
  (:require [com.stuartsierra.component :as component]
            [songpark.common.communication :refer [PUT]]
            [songpark.jam.tpx :as jam.tpx]
            [songpark.mqtt :as mqtt]
            [songpark.mqtt.util :refer [teleporter-topic]]
            [taoensso.timbre :as log]
            [tpx.config :refer [config]]
            [tpx.data :as data]
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
  (let [data {:teleporter/nickname (get-in config [:ipc :teleporter :nickname])
              :teleporter/mac (get-device-mac)
              :teleporter/tpx-version (:tpx/version config)
              :teleporter/bp-version (:bp/version config)
              :teleporter/fpga-version (:fpga/version config)
              :teleporter/apt-version (data/get-apt-version)}
        platform-url (str (get-in config [:ipc :platform]) "/api/teleporter")]
    (log/debug "Broadcasting to URL" platform-url data)
    (PUT platform-url data success-cb error-cb)))

(defn- system-map [extra-components]
  (let [;; logger and config are started this way so that we can ensure
        ;; things are logged as we want and that the config is loaded
        ;; before all the other modules
        core-config (component/start (tpx.config/config-manager {}))
        logger (component/start (logger/logger (:logger config)))]
    (broadcast-presence
     (fn [{:teleporter/keys [id] :as _result}]
       ;; set teleporter-id for data
       (data/set-tp-id! id)
       ;; start the rest of system
       (reset! system (component/start
                       (apply component/system-map
                              (into [:logger logger
                                     :mqtt-client (mqtt/mqtt-client (assoc-in (:mqtt config) [:config :id] id))
                                     :config core-config
                                     :network (network/network (:network config))
                                     :ipc (component/using (ipc/ipc-service {:config (:ipc config)})
                                                           [:mqtt-client])
                                     :jam (component/using (jam.tpx/get-jam (merge {:tp-id id}
                                                                                   (:jam config)))
                                                           [:ipc :mqtt-client])
                                     :heartbeat (component/using (heartbeat/heartbeat-service {:config (:heartbeat config)})
                                                                 [:mqtt-client])]
                                    extra-components))))
       ;; setup mqtt client further with injections and topics
       (let [{:keys [mqtt-client ipc jam]} @system]
         ;; injections of ipc and jam first
         (mqtt/add-injection mqtt-client :ipc ipc)
         (mqtt/add-injection mqtt-client :jam jam)
         ;; add topic of its own id
         (mqtt/subscribe mqtt-client {(teleporter-topic id) 0})))
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
  
  (PUT "http://localhost:3000/api/teleporter" 
       {:teleporter/mac "00:0a:35:00:00:00"}
       (fn [response]
         (println response))
       nil)

  (defn init []
    ;; TODO get MAC address and send it to the platform, platform gives you UUID store that for MQTT topic
    ;; TODO start MQTT topic 
    ;; TODO initiate BP communication via serial 
    ;; TODO pubish to MQTT and start listening for mesages 
    (println "I AM TPX"))

  )
