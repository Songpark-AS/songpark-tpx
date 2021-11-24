(ns tpx.network.reporter
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [tpx.ipc.serial :as serial]
            [tpx.data :as data]
            [tpx.ipc.serial :refer [send-command]]
            [tpx.init]))

;; keep track of if we have reported the last netconfig, reset to false when config changes
;; on network + mqtt up, if we have not reported the netconfig we do so


;; TODO: send mock mqtt message
;; TODO: handle mock mqtt message in the app
;; TODO: fetch the data in ipc/output.clj
;; TODO: test for realsies
;; TODO: make into system-component

(defonce has-reported? (atom false))
(defonce current-network-config (atom {}))

(defn send-network-report [network-config]
  (when (not @has-reported?)
    (log/debug ::send-network-report "I should send a network report")
    (let [mqtt-manager (:mqtt-manager @tpx.init/system)
          topic "replace-me-topic"]
      (.publish mqtt-manager topic {:message/type :teleporter/net-config-report
                                    :message/body {:teleporter/id (data/get-tp-id)
                                                   :teleporter/network-config network-config}}))
    (reset! has-reported? true)))


(defn fetch-current-network-config []
  ;; Query BP for network info
  (send-command (:port @serial/config) "getlip" "")
  (send-command (:port @serial/config) "getgip" "")
  (send-command (:port @serial/config) "getmask" "")

  )

(comment
  (fetch-current-network-config)
  (send-network-report {:teleporter/local-ip "192.168.11.123"
                        :teleporter/gateway-ip "192.168.11.1"
                        :teleporter/mask-ip "255.255.255.0"
                        :teleporter/DHCP? true})
  )
