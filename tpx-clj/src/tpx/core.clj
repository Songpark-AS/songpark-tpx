(ns tpx.core
  (:gen-class)
  (:require
   [tpx.init :as init]
   [taoensso.timbre :as log]
   [clojurewerkz.machine-head.client :as mh]
   [common.platform.connect.client :as connect.client]
   [common.platform.connect.tp :as connect.tp]
   [common.mqtt.connection :as mqtt-connection]
   [clojure.java.shell :as shell]))

(def global-conn-map (atom {}))

(defn -main
  "Prepares mqtt connection"
  [& _args]
  ; (println "Hello, World!")
  (log/set-level! :info)
  (init/init)
)

;! Possibly useful later

(defn watch-agent-func 
  "Boilerplate for making a watch-agent.
   Intended for async IPC with CS7, e.g. if pure stdin/out is used for this purpose"
  [_agent context]
  (let [watch-fn (fn [_context _key _ref old-value new-value])]
    (add-watch _agent nil (partial watch-fn context))))

;! Current tpx functionality

;? __All of the below functions__ are activated by MQTT/has some relation to MQTT,
;? and have the purpose of relaying instructions from MQTT to CS7.

; --- Volume controls ---
(defn adjust-volume-musician
  "Relays musician-specific volume control towards CS7.
   Takes musician's ID, and positive or negative integers.
   Assumes CS7 has some kind of logic to deal with 'invalid values'."
  [musician adjusted-value]
  ; #TODO
  (let 
   [stream-number (rand-int 4)
    musician (nth ["Bomb Jovi" "Dummy Borgir" "Brus Sprengstein" "BeyonC++"] stream-number)]
    (println "Asking CS7 politely to adjust volume of '"musician"' to:" adjusted-value ".")
    (println "CS7 politely agreed. Volume of '"musician"' adjusted to:" adjusted-value ".")))

(defn adjust-volume-unit
  "Relays volume control towards CS7.
   Takes positive or negative integers.
   Assumes CS7 has some kind of logic to deal with 'invalid values'."
  [adjusted-value]
  ; #TODO
  (println "Asking CS7 politely to adjust the unit's output volume to:" adjusted-value ".")
  (println "CS7 politely agreed. Unit's volume adjusted to:"            adjusted-value ".")
  (prn "BEFOOOOOOREEEEEE")
  (prn "does this exist? " @global-conn-map)
  ;;(mqtt-connection/publish (@global-conn-map :connection) (@global-conn-map :topic) [[:phone-app :adjust-volume-unit] [adjusted-value]])
  (try
    (mh/publish (@global-conn-map :connection) (@global-conn-map :topic) (str "POLOOOOOOOOOOOOOO!!!!!!!One1" (rand-int 9)))
    (catch Exception e (prn (str "Caught publish in the act:" (.getMessage e)))))
  (prn "AFTERRRRRRRRRRRR")
  42
  )

;--- Mute controls ---
(defn toggle-mute-musician
  "Relays to CS7 instructions to toggle mute specific musicians through 2 steps.
   The first step is possibly reduntant, 
   but can be nice to have for ensuring correct UI representation on the mobile app.
   1. Asks CS7 if the specific musician is muted or not already.
   2. Sends instruction to CS7 to mute or unmute."
  [musician]
  (println "Checking current mute state of" musician ".")
  (let ; Contact CS7 here. #TODO
   [muted-or-not (nth ["inactive" "active"] (rand-int 2))]
    (println "The mute state of" musician "is" muted-or-not ".")
    (println "Asking CS7 politely to mute/unmute" musician ".")
    (println "CS7 politely toggled muted/unmute of" musician "to" muted-or-not".")))

(defn toggle-mute-unit
  "Relays to CS7 instructions to mute specific musicians.
   The first step is possibly reduntant, 
   but can be nice to have for ensuring correct UI representation on the mobile app.
   1. Asks CS7 if the unit is muted or not already.
   2. Sends instruction to CS7 to mute or unmute."
  []
  (println "Checking current mute state of unit.")
  (let ; Contact CS7 here. #TODO
   [muted-or-not (nth ["inactive" "active"] (rand-int 2))]
    (println "The mute state of unit is" muted-or-not ".")
    (println "Asking CS7 politely to mute/unmute the unit.")
    (println "CS7 politely toggled the teleport mute/unmute to" muted-or-not ".")))

; --- SFX controls ---
(defn adjust-dsp-effects
  "Not sure what this is yet"
  [dsp-input]
  (println "Hey, CS7, do the DSP! Just like" dsp-input "!")
  (println "Damn, CS7 did do the DSP done."))

(defn adjust-gain-input
  "Relays to CS7 to adjust gain value(s).
   Maps to inputs:
    2 separate mic [A & B?]
    2 separate line in w/ stereo [C & D]?"
  [input-device new-value]
  (let ; Pretends an input device was selected externally
   [device-number (rand-int 4)
    input-device (nth ["mic A" "mic B" "input C" "input D"] device-number)]
    (println "Checking with CS7 about gain levels for input device" input-device ".")
    (println "CS7 reports gain levels for input" input-device ":" "#TODO.")
    (println "Quering CS7 to set gain level of input" input-device "to" new-value ".")
    (println "Query complete, CS7 has set gain level of input" input-device "to" new-value".")))

(defn adjust-gain-musician
  "Relays to CS7 to adjust gain value(s) of specific musicians (through 'effects' of musicians?).
   Maps to input streams from different musicians."
  [musician-stream new-value]
  (let ; Pretends an input stream was selected externally, and to contact CS7 #TODO
   [stream-number (rand-int 4)
    musician-stream (nth ["Bomb Jovi" "Dummy Borgir" "Brus Sprengstein" "BeyonC++"] stream-number)]
    (println "Checking with CS7 about gain levels for input device" musician-stream ".")
    (println "CS7 reports gain levels for input" musician-stream ":" "#TODO.")
    (println "Querying CS7 to set gain level of input" musician-stream "to" new-value ".")
    (println "CS7 has set gain level of input" musician-stream "to" new-value".")))

(defn toggle-phantom-power
  "Relays to CS7 to activate/deactivate phantom power."
  []
  (let ; Pretend to contact CS7 about the phantom state and switch it #TODO
   [phantom-states (vector "inactive" "active")
    phantom-state-binary (rand-int 2)
    phantom-state (nth phantom-states phantom-state-binary)
    phantom-state-inverse (nth phantom-states (* -1 (- 1 phantom-state-binary)))]
    (prn "Checking with CS7 if Phantom Power is on or not.")
    (prn "CS7 reports Phantom Power to be:" phantom-state)
    (prn "Querying CS7 to toggle the currently Phantom Power to" phantom-state-inverse ".")
    (prn "CS7 has set Phantom Power to" phantom-state-inverse ".")))

; --- Netcode ---
(defn init-ipc-with-cs7
  "Assumes CS7 to be a separate process running in parallell with TPX.
   Initializes Inter-Process Communication with CS7.
   Currently not complete; the methods for IPC have not been decided yet."
  [] 
  ; #TODO
  (println "Facilitating IPC with CS7...")
  (println "Connection with CS7 established! (just kidding! still need to complete that thing)"))

(defn network-configuration
  "Probably for connecting to a friend's jam and such."
  ; #TODO
  [])

; --- IO ---
(defn retrieve-tpID
  "Contains the instructions sent to CS7 to retrieve the tpID."
  []
  (println "Querying CS7 for tpID.")
  (let [tpID "0000"] ; hardcoded to "0000" - used to say World, previously...
    (println "Response from CS7 reads: " tpID)
    tpID))

(defn talk-to-cs7
  "Supposed to talk to CS7 via STDIN"
  [name]
  (let [data (shell/sh "docker" "run" name)]
    (:out data)))

(defn toggle-audio-recording
  "Relays to CS7 for a recording of the audiostream to begin or end"
  []
  (println "Checking with CS7: Is the unit currently recording the audiostream?")
  (let ; Contact CS7 here #TODO
   [recording-or-not (if (= 0 (rand-int 2)) "No" "Yes")]
    (println "CS7 responds:" recording-or-not "!")
    (println (if (= "No" recording-or-not) "Commencing" "Ending") "recording process.")
    (println "Recording" (if (= "No" recording-or-not) "commenced." "ended."))))

(defn audio-file-management
  "Play/delete/upload to cloud"
  [])


; --- Currently unknown functionalities ---
(defn audio-stream-control
  "What's this about?"
  []
  )


(defn toggle-mems-talkback-mic
  "Apparently, this enables/disables What is this?"
  [])



  
;! REPL execution playground

;? Imitating communication between phone and TPX unit

;; (def handler-map {:device {:volume-change #(println "Volume changed by " % " amount")}})
(def handler-map 
  { :tpx-unit {  
                :adjust-dsp-effects      adjust-dsp-effects
                :adjust-gain-input       adjust-gain-input
                :adjust-gain-musician    adjust-gain-musician
                :adjust-volume-musician  adjust-volume-musician
                :adjust-volume-unit      adjust-volume-unit
                :toggle-audio-recording  toggle-audio-recording
                :toggle-mute-musician    toggle-mute-musician
                :toggle-mute-unit        toggle-mute-unit
              }
    ;; :phone-app  { ; Not used in tpx, but is the equivalent method of reference for publishing
    ;;               :dummy dumb }
   })

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
      (let [conn-map (common-connect-init uuid)]
        (prn "Here be the conn-map from init arrrr!: " conn-map)
        (reset! global-conn-map conn-map)
        (prn "Here be global conn-map: " @global-conn-map)
        ;; (mqtt-connection/subscribe conn-map handler-map))
        (common-subscribe conn-map handler-map))
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
  ;(fake-phone-commands)
)


;; ;? Basic MQTT functionality
;; (comment test-area-of-mqtt-setup
;;          (connect.client/init {:nickname "christians.dream"})
;;          (def conn-map (mqtt-connection/init "my-topic2"))
;;          (def handler-map {:volume_control #(println (apply - %&))})
;;          (let [conn-map (mqtt-connection/init "my-topic")
;;                handler-map {:volume_control #(println (apply - %&))}])

;;          (mqtt-connection/subscribe conn-map handler-map)
;;          (mqtt-connection/publish conn-map [[:volume_control] [1 2 3 3 3 3]]))
