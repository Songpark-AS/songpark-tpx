(ns tpx-clj.core
  (:require
   [clojurewerkz.machine-head.client :as mh]
   [common.platform.connect.client :as connect.client]
   [common.platform.connect.tp :as connect.tp]
   [common.mqtt.connection :as mqtt-connection])
  (:gen-class))


(defn -main
  "Prepares mqtt connection"
  [& args]
  ; (println "Hello, World!")
  ; (init-ipc-with-cs7)
  ; (retrieve-tpID))
)

;! Possibly useful later

(defn watch-agent-func [_agent context]
  "Boilerplate for making a watch-agent
   Intended for async IPC with CS7, e.g. if pure stdin/out is used for this purpose"
  (let [watch-fn (fn [_context _key _ref old-value new-value])]
    (add-watch _agent nil (partial watch-fn context))))

;! Current tpx functionality

(defn init-ipc-with-cs7
  "Initializes Inter-Process Communication with CS7.
   Currently not complete; the methods for IPC have not been decided yet."
  [& args] ; it did not want to evaluate without this definition?
  (println "Facilitating IPC with CS7...")
  (println "Connection with CS7 established! (just kidding! still need to complete that thing)"))

(defn retrieve-tpID
  "Contains the instructions sent to CS7 to retrieve the tpID."
  [& args]
  (println "Querying CS7 for tpID.")
  (let [tpID "world"]
    (println "Response from CS7 reads: "tpID)
    tpID))

(defn musician-specific-volume-control
  "Relays musician-specific volume control towards CS7.
   Takes musician's ID, and positive or negative integers.
   "
  [musician adjusted-amount]
  (println "Asking CS7 politely to adjust volume of '"musician"' by: "adjusted-amount)
  (println "CS7 politely agreed. Volume of '"musician"' adjusted by: "adjusted-amount))

(defn unit-volume-control
  "Relays volume control towards CS7
   Takes positive or negative integers"
  [adjusted-amount]
  (println "Asking CS7 politely to adjust the unit's output volume by: "adjusted-amount)
  (println "CS7 politely agreed. Unit's volume adjusted by: "adjusted-amount))

;! REPL execution playground

;? Imitating communication between phone and TPX unit

;; (def handler-map {:device {:volume-change #(println "Volume changed by " % " amount")}})
(def handler-map {:device {:volume-change musician-specific-volume-control}})

(defn initiate-communications
  "Initiates communcations with the backend,
   telling the backend its tpID,
   then initiates communications to MQTT's pub/sub"
  [& args]
  (let [tpid "0000"
        plat-response (connect.tp/init {:tpid tpid})
        uuid (:uuid plat-response)
        status (:status plat-response)]
    (if (and status uuid)
      (let [conn-map (mqtt-connection/init uuid)
            handler-map handler-map]
        (mqtt-connection/subscribe conn-map handler-map))
      (println "Teleporter connection to platform, failed"))
    (println "This is uuid: " uuid)))

(defn fake-phone-commands
  "Pretends to be a phone that hooks into the background,
   associates itself with a specific unit's tpID,
   then queries TPX via MQTT to interact with CS7."
  [& args]
  (let [nickname "christians.dream"
        plat-response (connect.client/init {:nickname "christians.dream"})
        uuid (:uuid plat-response)
        status (:status plat-response)]

    (if (and status uuid)
      (let [conn-map (mqtt-connection/init uuid)]
        (mqtt-connection/publish conn-map [[:device :volume-change] ["self" 123]])
        (mqtt-connection/publish conn-map [[:device :volume-change] ["self" 123]])
        (mqtt-connection/publish conn-map [[:device :volume-change] ["self" (rand)]])
        (mqtt-connection/publish conn-map [[:device :volume-change] ["self" (rand)]])
        (mqtt-connection/publish conn-map [[:device :volume-change] ["self" (rand)]])
        (mqtt-connection/publish conn-map [[:device :volume-change] ["self" (rand)]])
        (mqtt-connection/publish conn-map [[:device :volume-change] ["self" 321]])
        (println "Client publishes finished"))
      (println "Client connection to platform, failed"))))

(comment ;? Commence imitation
  "; Evaluate the forms in this comment to see proof of concept, where tp tells platform that it's on,
   ; platform creates and returns uuid, tp creates and subscribes to topic with uuid-name,
   ; client requests 'christians.dream', client is returned the same uuid, and client 
   ; published a number of requests on topic.
   ; (Feel free to delete/change this in any way you want)"
  (initiate-communications)
  (fake-phone-commands)
)


;? Basic MQTT functionality
(comment test-area-of-mqtt-setup
         (connect.client/init {:nickname "christians.dream"})
         (def conn-map (mqtt-connection/init "my-topic2"))
         (def handler-map {:volume_control #(println (apply - %&))})
         (let [conn-map (mqtt-connection/init "my-topic")
               handler-map {:volume_control #(println (apply - %&))}])

         (mqtt-connection/subscribe conn-map handler-map)
         (mqtt-connection/publish conn-map [[:volume_control] [1 2 3 3 3 3]]))