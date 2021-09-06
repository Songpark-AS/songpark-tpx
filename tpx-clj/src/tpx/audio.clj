(ns tpx.audio
  (:require [tpx.http :as tpx.http]
            [clojurewerkz.machine-head.client :as mh]))

;! --- Volume controls ---
(defn adjust-volume-musician
  "Relays musician-specific volume control towards CS7.
   Takes musician's ID, and positive or negative integers.
   Assumes CS7 has some kind of logic to deal with 'invalid values'."
  [musician adjusted-value]
  (let
   [stream-number (rand-int 4)
    musician (nth ["Bomb Jovi" "Dummy Borgir" "Brus Sprengstein" "BeyonC++"] stream-number)]
    (println "Asking CS7 politely to adjust volume of '" musician "' to:" adjusted-value ".")
    (println "CS7 politely agreed. Volume of '" musician "' adjusted to:" adjusted-value ".")))

(defn adjust-volume-unit
  "Relays volume control towards CS7.
   Takes positive or negative integers.
   Assumes CS7 has some kind of logic to deal with 'invalid values'."
  [adjusted-value]
  (println "Asking CS7 politely to adjust the unit's output volume to:" adjusted-value ".")
  (println "CS7 politely agreed. Unit's volume adjusted to:"            adjusted-value ".")
  (prn "BEFOOOOOOREEEEEE")
  (prn "does this exist? " @tpx.http/global-conn-map)
  ;;(mqtt-connection/publish (@global-conn-map :connection) (@global-conn-map :topic) [[:phone-app :adjust-volume-unit] [adjusted-value]])
  (try
    (mh/publish (@tpx.http/global-conn-map :connection) (@tpx.http/global-conn-map :topic) (str "POLOOOOOOOOOOOOOO!!!!!!!One1" (rand-int 9)))
    (catch Exception e (prn (str "Caught publish in the act:" (.getMessage e)))))
  (prn "AFTERRRRRRRRRRRR")
  42)

;! --- Gain controls ---
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
    (println "CS7 reports gain levels for input" input-device ":" "TODO.")
    (println "Quering CS7 to set gain level of input" input-device "to" new-value ".")
    (println "Query complete, CS7 has set gain level of input" input-device "to" new-value ".")))

(defn adjust-gain-musician
  "Relays to CS7 to adjust gain value(s) of specific musicians (through 'effects' of musicians?).
   Maps to input streams from different musicians."
  [musician-stream new-value]
  (let ; Pretends an input stream was selected externally, and to contact CS7
   [stream-number (rand-int 4)
    musician-stream (nth ["Bomb Jovi" "Dummy Borgir" "Brus Sprengstein" "BeyonC++"] stream-number)]
    (println "Checking with CS7 about gain levels for input device" musician-stream ".")
    (println "CS7 reports gain levels for input" musician-stream ":" "TODO.")
    (println "Querying CS7 to set gain level of input" musician-stream "to" new-value ".")
    (println "CS7 has set gain level of input" musician-stream "to" new-value ".")))

;! --- Mute controls ---
(defn toggle-mute-unit
  "Relays to CS7 instructions to mute specific musicians.
   The first step is possibly reduntant, 
   but can be nice to have for ensuring correct UI representation on the mobile app.
   1. Asks CS7 if the unit is muted or not already.
   2. Sends instruction to CS7 to mute or unmute."
  [_]
  (println "Checking current mute state of unit.")
  (let ; Contact CS7 here.
   [muted-or-not (nth ["inactive" "active"] (rand-int 2))]
    (println "The mute state of unit is" muted-or-not ".")
    (println "Asking CS7 politely to mute/unmute the unit.")
    (println "CS7 politely toggled the teleport mute/unmute to" muted-or-not ".")))

(defn toggle-mute-musician
  "Relays to CS7 instructions to toggle mute specific musicians through 2 steps.
   The first step is possibly reduntant, 
   but can be nice to have for ensuring correct UI representation on the mobile app.
   1. Asks CS7 if the specific musician is muted or not already.
   2. Sends instruction to CS7 to mute or unmute."
  [musician]
  (println "Checking current mute state of" musician ".")
  (let ; Contact CS7 here.
   [muted-or-not (nth ["inactive" "active"] (rand-int 2))]
    (println "The mute state of" musician "is" muted-or-not ".")
    (println "Asking CS7 politely to mute/unmute" musician ".")
    (println "CS7 politely toggled muted/unmute of" musician "to" muted-or-not ".")))

;! --- SFX controls ---
(defn adjust-dsp-effects
  "Not sure what this is yet"
  [dsp-input]
  (println "Hey, CS7, do the DSP! Just like" dsp-input "!")
  (println "Damn, CS7 did do the DSP done."))

(defn toggle-phantom-power
  "Relays to CS7 to activate/deactivate phantom power."
  [_]
  (let ; Pretend to contact CS7 about the phantom state and switch it #TODO
   [phantom-states (vector "inactive" "active")
    phantom-state-binary (rand-int 2)
    phantom-state (nth phantom-states phantom-state-binary)
    phantom-state-inverse (nth phantom-states (* -1 (- 1 phantom-state-binary)))]
    (prn "Checking with CS7 if Phantom Power is on or not.")
    (prn "CS7 reports Phantom Power to be:" phantom-state)
    (prn "Querying CS7 to toggle the currently Phantom Power to" phantom-state-inverse ".")
    (prn "CS7 has set Phantom Power to" phantom-state-inverse ".")))

;! --- Yet undefined controls ---
(defn audio-stream-control
  "What's this about?"
  [_])


(defn toggle-mems-talkback-mic
  "Apparently, this enables/disables What is this?"
  [_])