(ns dev
  (:require [tpx.core :as tpx.core]
            [tpx.init :as tpx.init]
            [tpx.http :as tpx.http]
            [tpx.mqtt :as tpx.mqtt]
            [common.platform.connect.client :as connect.client]
            [common.mqtt.connection :as mqtt-connection]
            [taoensso.timbre :as log]))

(defn restart
  "stop and start tpx"
  []
  ;; set the log level to info or jetty will spam your REPL console,
  ;; significantly slowing down responses
  (log/merge-config! {:level        :debug
                      :ns-blacklist ["org.eclipse.jetty.*"
                                     "io.grpc.netty.shaded.io.netty.*"
                                     "org.opensaml.*"]})
  
  (tpx.init/stop)
  (tpx.init/init))

;? Imitating communication between phone and TPX unit
(defn establish-fake-phone
  "Pretends to be a phone that hooks into the backend,
   associates itself with a specific unit's tpID,
   returns the constructed items"
  []
  (let [;nickname "christians.dream"
        plat-response (connect.client/init {:nickname "christians.dream"})
        uuid (:uuid plat-response)
        status (:status plat-response)]
    [uuid status]))

(defn fake-phone-commands
  "Pretends to be a phone to send commands over MQTT,
   awaiting a response from the TPX"
  [uuid]
    (let [;uuid "d479bc4e-6a13-4dd2-ab0f-2ce1b3fb4c1f"
          test-conn-map (tpx.mqtt/common-connect-init uuid)]
      (mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [123]])
      #_#_#_#_#_#_(mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [123]])
      (mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [(rand)]])
      (mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [(rand)]])
      (mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [(rand)]])
      (mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [(rand)]])
      (mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [321]])
      #_#_#_#_#_#_#_(mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [123]])
      (mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [123]])
      (mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [(rand)]])
      (mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [(rand)]])
      (mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [(rand)]])
      (mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [(rand)]])
      (mqtt-connection/publish test-conn-map [[:tpx-unit :adjust-volume-unit] [321]])
      (println "Client publishes finished"))
    (println "Client connection to platform, failed"))

(comment ;? Commence imitation
  "; Evaluate the forms in this comment to see proof of concept, where tp tells platform that it's on,
   ; platform creates and returns uuid, tp creates and subscribes to topic with uuid-name,
   ; client requests 'christians.dream', client is returned the same uuid, and client 
   ; published a number of requests on topic.
   ; (Feel free to delete/change this in any way you want)"
  (tpx.core/-main)
  (tpx.http/initiate-communications tpx.init/handler-map)
  (def plat-response-uuid-status (establish-fake-phone))
  (fake-phone-commands plat-response-uuid-status )
  )

;? Basic MQTT functionality
#_(comment test-area-of-mqtt-setup
         (connect.client/init {:nickname "christians.dream"})
         (def conn-map (mqtt-connection/init "my-topic2"))
         (def handler-map {:volume_control #(println (apply - %&))})
         (let [conn-map (mqtt-connection/init "my-topic")
               handler-map {:volume_control #(println (apply - %&))}])

         (mqtt-connection/subscribe conn-map handler-map)
         (mqtt-connection/publish conn-map [[:volume_control] [1 2 3 3 3 3]]))
