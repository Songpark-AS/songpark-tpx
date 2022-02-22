(ns tpx.init
  (:require [com.stuartsierra.component :as component]
            [songpark.common.communication :refer [PUT]]
            [songpark.jam.tpx :as jam.tpx]
            [songpark.mqtt :as mqtt]
            [taoensso.timbre :as log]   
            [tpx.config :refer [config]]
            [tpx.data :as data]
            [tpx.logger :as logger]
            [tpx.ipc :as ipc]
            [tpx.heartbeat :as heartbeat]
            [tpx.network :as network]))


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
        ;; for all the other modules
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
                                     ;;:message-service (message/message-service (:message config))
                                     :network (network/network (:network config))
                                     ;; :mqtt-manager (component/using (mqtt/mqtt-manager (merge (:mqtt config)
                                     ;;                                                          {:injection-ks [:message-service]}))
                                     ;;                                [:message-service])
                                     :ipc (component/using (ipc/ipc-service {:config (:ipc config)})
                                                           [:mqtt-client])
                                     ;; :jam (component/using (jam.tpx/get-jam (:jam config))
                                     ;;                       [:ipc :mqtt-client])
                                     :heartbeat (component/using (heartbeat/heartbeat-service {:config (:heartbeat config)})
                                                                 [:mqtt-client])]
                                    extra-components)))))
     (fn [error]
       ;; add flashing leds to indicate a restart is required
       (log/error error)))))


(defonce system (atom nil))


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
