(ns tpx-clj.core
  (:require
   [clojurewerkz.machine-head.client :as mh]
   [common.platform.connect.client :as connect.client]
   [common.platform.connect.tp :as connect.tp]
   [common.mqtt.connection :as mqtt-connection]
   [clojure.java.shell :as shell])
  (:gen-class))


(defn -main
  "Prepares mqtt connection"
  []
  ; (println "Hello, World!")
  ; (init-ipc-with-cs7)
  ; (retrieve-tpID))
)

;! Possibly useful later

(defn watch-agent-func 
  "Boilerplate for making a watch-agent.
   Intended for async IPC with CS7, e.g. if pure stdin/out is used for this purpose"
  [_agent context]
  (let [watch-fn (fn [_context _key _ref old-value new-value])]
    (add-watch _agent nil (partial watch-fn context))))

;! Current tpx functionality

(defn init-ipc-with-cs7
  "Initializes Inter-Process Communication with CS7.
   Currently not complete; the methods for IPC have not been decided yet."
  [] ; it did not want to evaluate without this definition?
  (println "Facilitating IPC with CS7...")
  (println "Connection with CS7 established! (just kidding! still need to complete that thing)"))

(defn retrieve-tpID
  "Contains the instructions sent to CS7 to retrieve the tpID."
  []
  (println "Querying CS7 for tpID.")
  (let [tpID "0000"] ; hardcoded to "0000" - used to say World, previously...
    (println "Response from CS7 reads: "tpID)
    tpID))

(defn adjust-volume-musician
  "Relays musician-specific volume control towards CS7.
   Takes musician's ID, and positive or negative integers.
   Assumes CS7 has some kind of logic to deal with 'invalid values'."
  [musician adjusted-amount]
  (println "Asking CS7 politely to adjust volume of '"musician"' by: "adjusted-amount)
  (println "CS7 politely agreed. Volume of '"musician"' adjusted by: "adjusted-amount))

(defn adjust-volume-unit
  "Relays volume control towards CS7.
   Takes positive or negative integers.
   Assumes CS7 has some kind of logic to deal with 'invalid values'."
  [adjusted-amount]
  (println "Asking CS7 politely to adjust the unit's output volume by: "adjusted-amount)
  (println "CS7 politely agreed. Unit's volume adjusted by: "adjusted-amount))

(defn toggle-mute-musician
  "Relays to CS7 instructions to toggle mute specific musicians through 2 steps.
   The first step is possibly reduntant, 
   but can be nice to have for ensuring correct UI representation on the mobile app.
   1. Asks CS7 if the specific musician is muted or not already.
   2. Sends instruction to CS7 to mute or unmute."
  [musician]
  (println "Checking current mute state of" musician)
  (let ; Contact CS7 here.
   [muted-or-not (rand-int[2])]
    (println "The mute state of" musician "is" muted-or-not)
    (println "Asking CS7 politely to mute/unmute" musician)
    (println "CS7 politely toggled muted/unmute of" musician "to" muted-or-not)))

(defn toggle-mute-unit
  "Relays to CS7 instructions to mute specific musicians.
   The first step is possibly reduntant, 
   but can be nice to have for ensuring correct UI representation on the mobile app.
   1. Asks CS7 if the unit is muted or not already.
   2. Sends instruction to CS7 to mute or unmute."
  []
  (println "Checking current mute state of unit")
  (let ; Contact CS7 here.
   [muted-or-not (rand-int[2])]
    (println "The mute state of unit is" muted-or-not)
    (println "Asking CS7 politely to mute/unmute the unit")
    (println "CS7 politely toggled the teleport mute/unmute to" muted-or-not)))

;! REPL execution playground

;? Imitating communication between phone and TPX unit

;; (def handler-map {:device {:volume-change #(println "Volume changed by " % " amount")}})
(def handler-map {:tpx-unit   { :adjust-volume-musician adjust-volume-musician
                                :adjust-volume-unit     adjust-volume-unit
                                :toggle-mute-musician   toggle-mute-musician
                                :toggle-mute-unit       toggle-mute-unit}
                  :phone-app  { :adjust-volume-musician adjust-volume-musician
                                :adjust-volume-unit     adjust-volume-unit
                                :toggle-mute-musician   toggle-mute-musician
                                :toggle-mute-unit       toggle-mute-unit}})

(defn initiate-communications
  "Initiates communications with the backend,
   telling the backend its tpID,
   then initiates communications to MQTT's pub/sub"
  [handler-map]
  (let [tpid (retrieve-tpID)
        plat-response (connect.tp/init {:tpid tpid})
        uuid (:uuid plat-response)
        status (:status plat-response)]
    (if (and status uuid)
      (let [conn-map (mqtt-connection/init uuid)]
        (mqtt-connection/subscribe conn-map handler-map))
      (println "Teleporter connection to platform, failed"))
    (println "This is uuid: " uuid)))

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

(comment ;? Commence imitation
  "; Evaluate the forms in this comment to see proof of concept, where tp tells platform that it's on,
   ; platform creates and returns uuid, tp creates and subscribes to topic with uuid-name,
   ; client requests 'christians.dream', client is returned the same uuid, and client 
   ; published a number of requests on topic.
   ; (Feel free to delete/change this in any way you want)"
  (initiate-communications handler-map)
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

(defn talk-to-cs7 
  "Supposed to talk to CS7 via STDIN"
  [name]
  (let [data (shell/sh "docker" "run" name)]
    (:out data)))
