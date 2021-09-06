(ns tpx.ipc
  (:require [clojure.java.shell :as shell]))


;! --- IPC specific ---
(defn talk-to-cs7
  "Supposed to talk to CS7 via STDIN"
  [name]
  (let [data (shell/sh "docker" "run" name)]
    (:out data)))

(defn retrieve-tpID
  "Contains the instructions sent to CS7 to retrieve the tpID."
  []
  (println "Querying CS7 for tpID.")
  (let [tpID "0000"] ; hardcoded to "0000" - used to say World, previously...
    (println "Response from CS7 reads: " tpID)
    tpID))


;! --- Files & Recordings ---
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

#_(defn init-ipc-with-cs7
  "Assumes CS7 to be a separate process running in parallell with TPX.
   Initializes Inter-Process Communication with CS7.
   Currently not complete; the methods for IPC have not been decided yet."
  [] 
  ;
  (println "Facilitating IPC with CS7...")
  (println "Connection with CS7 established! (just kidding! still need to complete that thing)"))

;! --- Watch agent boilerplate ---
(defn watch-agent-func
  "Boilerplate for making a watch-agent.
   Intended for async IPC with CS7, e.g. if pure stdin/out is used for this purpose"
  [_agent context]
  (let [watch-fn (fn [_context _key _ref old-value new-value])]
    (add-watch _agent nil (partial watch-fn context))))
