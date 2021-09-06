(ns dev
  (:require [tpx.core :as tpx.core]
            [tpx.init :as tpx.init]
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
  (init/stop)
  (init/init))

(comment ;? Commence imitation
  "; Evaluate the forms in this comment to see proof of concept, where tp tells platform that it's on,
   ; platform creates and returns uuid, tp creates and subscribes to topic with uuid-name,
   ; client requests 'christians.dream', client is returned the same uuid, and client 
   ; published a number of requests on topic.
   ; (Feel free to delete/change this in any way you want)"
  (tpx.core/-main)
  (tpx.init/initiate-communications tpx.init/handler-map)
  ;(fake-phone-commands)
  )

;? Imitating communication between phone and TPX unit
;; (defn fake-phone-commands
;;   "Pretends to be a phone that hooks into the backend,
;;    associates itself with a specific unit's tpID,
;;    then queries TPX via MQTT to interact with CS7."
;;   []
;;   (let [;nickname "christians.dream"
;;         plat-response (connect.client/init {:nickname "christians.dream"})
;;         uuid (:uuid plat-response)
;;         status (:status plat-response)]

;;     (if (and status uuid)
;;       (let [uuid "3266f6d8-c50e-448b-a7f6-3f860cc47a1e"
;;             conn-map (mqtt-connection/init uuid)]
;;         (mqtt-connection/publish conn-map [[:tpx-unit :adjust-volume-unit] [123]])
;;         (mqtt-connection/publish conn-map [[:tpx-unit :adjust-volume-unit] [123]])
;;         (mqtt-connection/publish conn-map [[:tpx-unit :adjust-volume-unit] [(rand)]])
;;         (mqtt-connection/publish conn-map [[:tpx-unit :adjust-volume-unit] [(rand)]])
;;         (mqtt-connection/publish conn-map [[:tpx-unit :adjust-volume-unit] [(rand)]])
;;         (mqtt-connection/publish conn-map [[:tpx-unit :adjust-volume-unit] [(rand)]])
;;         (mqtt-connection/publish conn-map [[:tpx-unit :adjust-volume-unit] [321]])
;;         (println "Client publishes finished"))
;;       (println "Client connection to platform, failed"))))

;; ;? Basic MQTT functionality
;; (comment test-area-of-mqtt-setup
;;          (connect.client/init {:nickname "christians.dream"})
;;          (def conn-map (mqtt-connection/init "my-topic2"))
;;          (def handler-map {:volume_control #(println (apply - %&))})
;;          (let [conn-map (mqtt-connection/init "my-topic")
;;                handler-map {:volume_control #(println (apply - %&))}])

;;          (mqtt-connection/subscribe conn-map handler-map)
;;          (mqtt-connection/publish conn-map [[:volume_control] [1 2 3 3 3 3]]))

(comment
  ;; stop and star
  (restart))
