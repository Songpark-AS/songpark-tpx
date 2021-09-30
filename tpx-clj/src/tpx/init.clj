(ns tpx.init
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]   
            [songpark.common.impl.mqtt.manager :as mqtt]
            [songpark.common.impl.message.service :as message]            
            [tpx.config :refer [config]]
            [tpx.logger :as logger]
            [tpx.ipc :as ipc]
            [tpx.message.incoming :as incoming]
            [tpx.message.outgoing :as outgoing]))


(defn- system-map [extra-components]
  (let [;; logger and config are started this way so that we can ensure
        ;; things are logged as we want and that the config is loaded
        ;; for all the other modules
        core-config (component/start (tpx.config/config-manager {}))
        logger (component/start (logger/logger (:logger config)))
        mqtt-config (:mqtt config)]
    (apply component/system-map
           (into [:logger logger
                  :config core-config
                  :mqtt-manager (mqtt/mqtt-manager (:mqtt config))
                  :message-service (component/using (message/message-service
                                                     {:injection-ks [:mqtt]
                                                      :handlers {:incoming incoming/handler
                                                                 :outgoing outgoing/handler}})
                                                    {:mqtt :mqtt-manager})
                  :ipc-service (component/using (ipc/ipc-service {:injection-ks [:message-service]
                                                                  :config (:ipc config)})
                                                [:message-service])]
                 extra-components))))


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
