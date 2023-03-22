(ns tpx.ipc.command
  (:require [tpx.data :as data]
            [tpx.ipc.serial :refer [send-command]]
            [taoensso.timbre :as log]))

(defonce allow-start-coredump (atom true))

(defn sync-reset []
  (send-command "syncrst" ""))

(defn gather-versions []
  (send-command "bver" "")
  (send-command "cver" ""))

(defn global-volume [value]
  (send-command "vol" value))

(defn local-volume [value]
  (do (send-command "vll" value)
      (send-command "vlr" value)))

(defn input1-volume [value]
  (send-command "vlr" value))

(defn input2-volume [value]
  (send-command "vll" value))

(defn network-volume [value]
  (send-command "vnl" value)
  (send-command "vnr" value))

(defn network-mute [value]
  (println "I AM NOT IMPLEMENTED"))


(defn set-playout-delay [value]
  (send-command "delay" value))


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

  (set-public-ip "217.118.62.14")
  (hangup-all)
  (receive-call #:teleporter{:local-ip "192.168.86.35"
                             :public-ip "217.118.62.14"
                             :port 8421})

  (initiate-call #:teleporter{:local-ip "192.168.86.34"
                              :public-ip "217.118.62.14"
                              :port 8421})

  (start-coredump)

  (local-volume 30)
  (network-volume 30)
  (global-volume 25)


  (set-playout-delay 30)

  ;; check active calls
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

  (send-command "vnr" "10")

  (send-command "cver" "")

  (send-command "bver" "")

  (send-command "delay" "20")
  (gather-versions)
)
