(ns tpx.init
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]   
            [clojure.java.shell :as shell]
            [songpark.common.impl.mqtt.manager :as mqtt]
            [songpark.common.impl.message.service :as message]
            [songpark.common.communication :refer [PUT GET]]
            [tpx.config :refer [config]]
            [tpx.logger :as logger]
            [tpx.ipc :as tpx.ipc]
            [tpx.audio :as tpx.audio]))


(defn get-device-mac []
  (-> (clojure.string/split (:out (shell/sh "ip" "n")) #"\n")
      first
      (clojure.string/split #"\s")
      (nth 4)))

(comment 
  
  (PUT "http://localhost:3000/api/teleporter" 
   {:teleporter/mac "00:0a:35:00:00:00"}
   (fn [response]
     (println response))
   nil)
)
   


(defn init []
  ;; TODO get MAC address and send it to the platform, platform gives you UUID store that for MQTT topic
  ;; TODO start MQTT topic 
  ;; TODO initiate BP communication via serial 
  ;; TODO pubish to MQTT and start listening for mesages 
  (println "I AM TPX"))
