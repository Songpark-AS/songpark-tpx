(ns tpx.mqtt.handler.teleporter
  (:require [clojure.set :refer [rename-keys]]
            [songpark.mqtt :as mqtt :refer [handle-message]]
            [songpark.mqtt.util :refer [broadcast-topic]]
            [songpark.jam.tpx.ipc :as tpx.ipc]
            [tpx.init :refer [system]]
            [taoensso.timbre :as log]
            [tpx.data :as data]
            [tpx.database :refer [get-hardware-values]]
            [tpx.fx :as fx]
            [tpx.network :refer [set-network!]]
            [tpx.network.reporter :as reporter]
            [tpx.sync :refer [sync-platform!]]
            [tpx.utils :as utils]))

(defmethod handle-message :teleporter.cmd/fx [{:teleporter/keys [id value fx input]
                                               :keys [mqtt-client ipc]
                                               :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::set-fx {:fx fx
                           :input input
                           :value value})
      (fx/write-fx input fx value)
      (sync-platform!))
    (log/debug ::set-fx-wrong-teleporter {:teleporter/id id
                                          :fx fx
                                          :input input
                                          :value value})))

(defmethod handle-message :teleporter.cmd/global-volume [{:teleporter/keys [id volume]
                                                          :keys [mqtt-client ipc]
                                                          :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::set-global-volume {:volume volume})
      (tpx.ipc/command ipc :volume/global-volume volume)
      (sync-platform! {:message {:message/type :teleporter/global-volume
                                 :teleporter/id id
                                 :teleporter/global-volume volume}}))
    (log/debug ::set-global-volume-wrong-teleporter {:teleporter/id id
                                                     :volume volume})))

(defmethod handle-message :teleporter.cmd/local-volume [{:teleporter/keys [volume id]
                                                         :keys [mqtt-client ipc]
                                                         :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::set-local-volume {:volume volume})
      (tpx.ipc/command ipc :volume/local-volume volume)
      (sync-platform! {:message {:message/type :teleporter/local-volume
                                 :teleporter/id id
                                 :teleporter/local-volume volume}}))
    (log/debug :set-local-volume-wrong-teleporter {:teleporter/id id
                                                   :volume volume})))

(defmethod handle-message :teleporter.cmd/input1-volume [{:teleporter/keys [volume id]
                                                          :keys [mqtt-client ipc]
                                                          :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::set-input1-volume {:volume volume})
      (tpx.ipc/command ipc :volume/input1-volume volume)
      (sync-platform! {:message {:message/type :teleporter/input1-volume
                                 :teleporter/id id
                                 :teleporter/input1-volume volume}}))
    (log/debug :set-input1-volume-wrong-teleporter {:teleporter/id id
                                                   :volume volume})))

(defmethod handle-message :teleporter.cmd/input2-volume [{:teleporter/keys [volume id]
                                                          :keys [mqtt-client ipc]
                                                          :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::set-input2-volume {:volume volume})
      (tpx.ipc/command ipc :volume/input2-volume volume)
      (sync-platform! {:message {:message/type :teleporter/input2-volume
                                 :teleporter/id id
                                 :teleporter/input2-volume volume}}))
    (log/debug :set-input2-volume-wrong-teleporter {:teleporter/id id
                                                    :volume volume})))

(defmethod handle-message :teleporter.cmd/input1+2-volume [{:teleporter/keys [volume id]
                                                            :keys [mqtt-client ipc]
                                                            :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::set-input1+2-volume {:volume volume})
      (tpx.ipc/command ipc :volume/input1+2-volume volume)
      (sync-platform! {:message {:message/type :teleporter/input1+2-volume
                                 :teleporter/id id
                                 :teleporter/input1+2-volume volume}}))
    (log/debug :set-input1+2-volume-wrong-teleporter {:teleporter/id id
                                                      :volume volume})))

(defmethod handle-message :teleporter.cmd/network-volume [{:teleporter/keys [volume id]
                                                           :keys [mqtt-client ipc]
                                                           :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::set-network-volume {:volume volume})
      (tpx.ipc/command ipc :volume/network-volume volume)
      (sync-platform! {:message {:message/type :teleporter/network-volume
                                 :teleporter/id id
                                 :teleporter/network-volume volume}}))
    (log/debug :set-network-volume-wrong-teleporter {:teleporter/id id
                                                     :volume volume})))

(defmethod handle-message :teleporter.cmd/network-mute [{:teleporter/keys [mute id]
                                                         :keys [mqtt-client ipc]
                                                         :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::set-network-mute {:mute mute})
      (tpx.ipc/command ipc :volume/network-mute mute)
      (sync-platform! {:message {:message/type :teleporter/network-mute
                                 :teleporter/id id
                                 :teleporter/network-mute mute}}))
    (log/debug :set-network-mute-wrong-teleporter {:teleporter/id id
                                                   :mute mute})))

(defmethod handle-message :teleporter.cmd/path-reset [{:teleporter/keys [id]
                                                       :keys [mqtt-client ipc]
                                                       :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::path-reset)
      (tpx.ipc/command ipc :jam/path-reset true))
    (log/debug ::path-reset-wrong-teleporter {:teleporter/id id})))

(defmethod handle-message :teleporter.cmd/set-playout-delay [{:teleporter/keys [id playout-delay]
                                                              :keys [mqtt-client ipc]
                                                              :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::set-playout-delay playout-delay)
      (tpx.ipc/command ipc :jam/playout-delay playout-delay)
      (sync-platform! {:message {:message/type :teleporter/playout-delay
                                 :teleporter/id id
                                 :teleporter/playout-delay playout-delay}}))
    (log/debug ::set-playout-delay-wrong-teleporter {:teleporter/id id})))

(defmethod handle-message :teleporter.cmd/report-network-config [{:keys [mqtt-client]
                                                                  :as msg}]
  (when (data/allowed? msg)
    (reporter/fetch-and-send-current-network-config mqtt-client)))

(defmethod handle-message :teleporter.cmd/set-ipv4 [{:teleporter/keys [network-values id]
                                                     :keys [mqtt-client]
                                                     :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug "Got new IPv4 config" network-values)
      (set-network! (rename-keys network-values {:ip/address :ip
                                                 :ip/gateway :gateway
                                                 :ip/subnet :netmask
                                                 :ip/dhcp? :dhcp?}))
      (future
        (let [client (:mqtt-client @system)]
          (mqtt/disconnect client)
          (Thread/sleep 100)
          (mqtt/connect client)
          (loop [attempt 4]
            (log/info (format "Attempt %d. MQTT is connected? %s" (- 5 attempt) (str (mqtt/connected? client))))
            (cond
              (zero? attempt)
              (log/error "Attempted to report new IP. Failed after 5 attempts")

              (and client
                   (mqtt/connected? client))
              (reporter/fetch-and-send-current-network-config mqtt-client)

              :else
              (do (log/info "Still not connected. Trying again in one second")
                  (recur (dec attempt))))))))
    (log/debug ::set-ipv4-wrong-teleporter {:id id})))

(defmethod handle-message :teleporter.cmd/upgrade [{:teleporter/keys [id]
                                                    :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::upgrade)
      (utils/upgrade))
    (log/debug ::upgrade-wrong-teleporter {:teleporter/id id})))

(defmethod handle-message :teleporter.cmd/hangup-all [{:teleporter/keys [id]
                                                       :keys [mqtt-client ipc]
                                                       :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::hangup-all)
      (tpx.ipc/command ipc :sip/hangup-all true))
    (log/debug ::hangup-all-wrong-teleporter {:teleporter/id id})))

(defmethod handle-message :teleporter.cmd/values [{:keys [teleporter/id mqtt-client]
                                                   :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::values)
      (let [values (get-hardware-values)]
        (mqtt/publish mqtt-client (broadcast-topic id) {:message/type :teleporter/values
                                                        :teleporter/id id
                                                        :teleporter/values values})))
    (log/debug ::values-wrong-teleporter {:teleporter/id id})))

(defmethod handle-message :teleporter.cmd/reboot [{:teleporter/keys [id]
                                                   :keys [mqtt-client]
                                                   :as msg}]
  (if (data/allowed? msg)
    (do
      (log/debug ::reboot)
      (mqtt/publish mqtt-client (broadcast-topic id) {:message/type :teleporter/reboot
                                                      :teleporter/id id
                                                      :teleporter/reboot true})
      (utils/reboot))
    (log/debug ::reboot-wrong-teleporter {:teleporter/id id})))
