(ns tpx.ipc.command
  (:require [tpx.data :as data]
            [tpx.ipc.serial :refer [send-command]]
            [taoensso.timbre :as log]))

(defonce allow-start-coredump (atom true))

(defn gather-versions []
  (send-command "pv" ""))

(defn global-volume [value]
  (send-command "vol" value))

(defn local-volume [value]
  (do (send-command "vll" value)
      (send-command "vlr" value)))

(defn input1-volume [value]
  (send-command "vll" value))

(defn input2-volume [value]
  (send-command "vrl" value))

(defn network-volume [value]
  (send-command "vln" value)
  (send-command "vrn" value))

(defn network-mute [value]
  (println "I AM NOT IMPLEMENTED"))


(defn set-playout-delay [value]
  (send-command "playout" value))


(defn set-local-ip [value]
  (send-command "setlip" value))

(defn set-public-ip [value]
  (send-command "setpip" value))

(defn hangup-all []
  ;; hangup all calls
  (send-command "hangup" ""))

(defn path-reset []
  (send-command "pr" ""))

(defn get-local-ip []
  (send-command "getlip" ""))

(defn get-gateway-ip []
  (send-command "getgip" ""))

(defn get-netmask-ip []
  (send-command "getmask" ""))

(defn start-coredump []
  (let [allowed? @allow-start-coredump]
    (if allowed?
      (do
        (reset! allow-start-coredump false)
        (send-command "monitor" "")))))

(defn stop-coredump []
  (reset! allow-start-coredump true)
  (send-command "halt" ""))

(defn receive-call [{:teleporter/keys [local-ip
                                       public-ip
                                       port]}]
  (send-command "setport" port)
  (send-command "setrpip" public-ip)
  (send-command "setrlip" local-ip)
  (send-command "rcall" "")
  (start-coredump))

(defn initiate-call [{:teleporter/keys [local-ip
                                        public-ip
                                        port]}]
  (send-command "setport" port)
  (send-command "setrpip" public-ip)
  (send-command "setrlip" local-ip)
  (send-command "icall" "")
  (start-coredump))

(defn stop-call []
  (stop-coredump)
  (send-command "hangup" ""))


(comment

  (start-coredump)

  (local-volume 75)
  (global-volume 25)

  ;; 0001
  (call-via-sip "sip:39d04c2c-7214-5e2c-a9ae-32ff15405b7f@voip1.songpark.com")
  (hangup-via-sip "sip:39d04c2c-7214-5e2c-a9ae-32ff15405b7f@voip1.songpark.com")

  ;; 0002
  (do (call-via-sip "sip:77756ff0-bb05-5e6a-b7d9-28086f3a07fd@voip1.songpark.com")
      (start-coredump))
  (do (hangup-via-sip "sip:77756ff0-bb05-5e6a-b7d9-28086f3a07fd@voip1.songpark.com")
      (stop-coredump))

  ;; 0003
  (call-via-sip "sip:b5fb6c6c-6707-56a6-88ab-23e4ec416abf@voip1.songpark.com")
  (hangup-via-sip "sip:b5fb6c6c-6707-56a6-88ab-23e4ec416abf@voip1.songpark.com")

  ;; Beatles
  (call-via-sip "sip:9100@voip1.songpark.com")
  (hangup-via-sip "sip:9100@voip1.songpark.com")

  ;; Elvis Presely
  (call-via-sip "sip:9102@voip1.songpark.com")
  (hangup-via-sip "sip:9102@voip1.songpark.com")

  ;; Jimi Hendrix
  (call-via-sip "sip:9104@voip1.songpark.com")
  (hangup-via-sip "sip:9104@voip1.songpark.com")

  ;; Adele
  (call-via-sip "sip:9106@voip1.songpark.com")
  (hangup-via-sip "sip:9106@voip1.songpark.com")

  ;; Madonna
  (call-via-sip "sip:9108@voip1.songpark.com")
  (hangup-via-sip "sip:9108@voip1.songpark.com")

  ;; Bach
  (call-via-sip "sip:9110@voip1.songpark.com")
  (hangup-via-sip "sip:9110@voip1.songpark.com")

  ;; ABBA
  (call-via-sip "sip:9120@voip1.songpark.com")
  (hangup-via-sip "sip:9120@voip1.songpark.com")

  (send-command "" "")
  (send-command "ha" "")
  (set-playout-delay 30)

  ;; check active calls
  (do (send-command "m" "")
      (Thread/sleep 200)
      (send-command "q" ""))

  (do (send-command "pd" "")
      (Thread/sleep 500)
      (send-command "12" "")
      (Thread/sleep 200)
      (send-command "pd" "")
      )

  (set-playout-delay 20)

  (do (send-command "pc" "")
      (send-command "vll" "10")
      (send-command "pc" "")
      (send-command "vlr" "10"))

  (do (send-command "pc" "")
      (send-command "vll" "10"))

  (do (send-command "pc" "")
      (send-command "vol" "50"))

  (do (send-command "pc" "")
      (send-command "vlr" "10"))

  (do (send-command "pc" "")
      (send-command "netvoll" "10")

      (send-command "pc" "")
      (send-command "netvolr" "10"))

  (send-command "cver" "")

  (send-command "bver" "")

  (send-command "delay" "20")
)
