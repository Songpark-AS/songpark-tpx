(ns tpx-clj.core
  (:require
   [clojurewerkz.machine-head.client :as mh]
   [common.platform.connect.client :as connect.client]
   [common.platform.connect.tp :as connect.tp]
   [common.mqtt.connection :as mqtt-connection])
  (:gen-class))

(def handler-map {:device {:volume-change #(println "Volume changed by " % " amount")}})

(defn -main
  "Prepares mqtt connection"
  [& args]

  ; Evaluate "do" to see proof of concept, where tp tells platform that it's on,
  ; platform creates and returns uuid, tp creates and subscribes to topic with uuid-name,
  ; client requests "christians.dream", client is returned the same uuid, and client 
  ; published a number of requests on topic.
  ; (Feel free to delete/change this in any way you want)
  (do
    (let [tpid "0000"
          plat-response (connect.tp/init {:tpid tpid})
          uuid (:uuid plat-response)
          status (:status plat-response)]
      (if (and status uuid)
        (let [conn-map (mqtt-connection/init uuid)
              handler-map handler-map]
          (mqtt-connection/subscribe conn-map handler-map))
        (println "Teleporter connection to platform, failed"))
      (println "This is uuid: " uuid))

  ; fake phone side:
    (let [nickname "christians.dream"
          plat-response (connect.client/init {:nickname "christians.dream"})
          uuid (:uuid plat-response)
          status (:status plat-response)]

      (if (and status uuid)
        (let [conn-map (mqtt-connection/init uuid)]
          (mqtt-connection/publish conn-map [[:device :volume-change] [123]])
          (mqtt-connection/publish conn-map [[:device :volume-change] [(rand)]])
          (mqtt-connection/publish conn-map [[:device :volume-change] [(rand)]])
          (mqtt-connection/publish conn-map [[:device :volume-change] [(rand)]])
          (mqtt-connection/publish conn-map [[:device :volume-change] [(rand)]])
          (mqtt-connection/publish conn-map [[:device :volume-change] [321]])
          (println "Client publishes finished"))
        (println "Client connection to platform, failed"))))

  ; (println "Hello, World!")
  ; (init-ipc-with-cs7)
  ; (retrieve-tpID))
  )
;; => Syntax error compiling if at (src/tpx_clj/core.clj:32:4).
;;    Too few arguments to if


(defn watch-agent-func [_agent context]
  "Boilerplate for making a watch-agent
   Intended for async IPC with CS7, e.g. if pure stdin/out is used for this purpose"
  (let [watch-fn (fn [_context _key _ref old-value new-value])]
    (add-watch _agent nil (partial watch-fn context))))

(defn init-ipc-with-cs7
  "Initializes Inter-Process Communication with CS7.
   Currently not complete; the methods for IPC have not been decided yet."
  (println "Facilitating IPC with CS7...")
  (println "Connection with CS7 established! (just kidding! still need to complete that thing)"))

(defn retrieve-tpID
  "Contains the instructions sent to CS7 to retrieve the tpID."
  (println "Querying CS7 for tpID.")
  (let [tpID "world"]
    (println "Response from CS7 reads:" tpID)))

(defn musician-specific-volume-control
  "Relays musician-specific volume control towards CS7.
   Takes musician's ID, and positive or negative integers.
   "
  [musician adjusted-amount]
  (println "Asking CS7 politely to adjust volume of '" musician "' by: " adjusted-amount)
  (println "CS7 politely agreed. Volume of '" musician "' adjusted by: " adjusted-amount))

(defn unit-volume-control
  "Relays volume control towards CS7
   Takes positive or negative integers"
  [adjusted-amount]
  (println "Asking CS7 politely to adjust the unit's output volume by: " adjusted-amount)
  (println "CS7 politely agreed. Unit's volume adjusted by:" adjusted-amount))



(comment test area
         (connect.client/init {:nickname "christians.dream"})
         (def conn-map (mqtt-connection/init "my-topic2"))
         (def handler-map {:volume_control #(println (apply - %&))})
         (let [conn-map (mqtt-connection/init "my-topic")
               handler-map {:volume_control #(println (apply - %&))}])

         (mqtt-connection/subscribe conn-map handler-map)
         (mqtt-connection/publish conn-map [[:volume_control] [1 2 3 3 3 3]]))