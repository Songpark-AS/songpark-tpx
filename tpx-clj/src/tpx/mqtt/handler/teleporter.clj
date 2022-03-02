(ns tpx.mqtt.handler.teleporter
  (:require [songpark.mqtt :refer [handle-message]]
            [songpark.jam.tpx.ipc :as tpx.ipc]
            [taoensso.timbre :as log]
            [tpx.data :as data]
            [tpx.network :refer [set-network!]]
            [tpx.network.reporter :as reporter]
            [tpx.utils :as utils]))

(defmethod handle-message :teleporter.cmd/global-volume [{:teleporter/keys [id volume] :keys [ipc]}]
  (if (data/same-tp? id)
    (do (log/debug ::set-global-volume {:volume volume})
        (tpx.ipc/command ipc :volume/global-volume volume))
    (log/debug ::set-global-volume-wrong-teleporter {:id id
                                                     :volume volume})))


(defmethod handle-message :teleporter.cmd/local-volume [{:teleporter/keys [volume id] :keys [ipc]}]
  (if (data/same-tp? id)
    (do
      (log/debug ::set-local-volume {:volume volume})
      (tpx.ipc/command ipc :volume/local-volume volume))
    (log/debug :set-local-volume-wrong-teleporter {:id id
                                                   :volume volume})))


(defmethod handle-message :teleporter.cmd/network-volume [{:teleporter/keys [volume id] :keys [ipc]}]
  (if (data/same-tp? id)
    (do
      (log/debug ::set-network-volume {:volume volume})
      (tpx.ipc/command ipc :volume/network-volume volume))
    (log/debug :set-network-volume-wrong-teleporter {:id id
                                                     :volume volume})))

(defmethod handle-message :teleporter.cmd/path-reset [{:teleporter/keys [id] :keys [ipc]}]
  (if (data/same-tp? id)
    (do
      (log/debug ::path-reset)
      (tpx.ipc/command ipc :jam/path-reset true))
    (log/debug ::path-reset-wrong-teleporter {:id id})))

(defmethod handle-message :teleporter.cmd/set-playout-delay [{:teleporter/keys [id playout-delay] :as ipc}]
  (if (data/same-tp? id)
    (do
      (log/debug ::set-playout-delay playout-delay)
      (tpx.ipc/command ipc :jam/playout-delay playout-delay))
    (log/debug ::set-playout-delay-wrong-teleporter {:id id})))

(defmethod handle-message :teleporter.cmd/report-network-config [{:keys [mqtt-client]}]
  (reporter/fetch-and-send-current-network-config mqtt-client))

(defmethod handle-message :teleporter.cmd/set-ipv4 [{:message/keys [values]}]
  (log/debug "Got new IPv4 config" values)
  (set-network! (clojure.set/rename-keys values {:ip/address :ip :ip/gateway :gateway :ip/subnet :netmask :ip/dhcp? :dhcp?})))

(defmethod handle-message :teleporter.cmd/upgrade [{:teleporter/keys [id]}]
  (if (data/same-tp? id)
    (do
      (log/debug ::upgrade)
      (utils/upgrade))
    (log/debug ::upgrade-wrong-teleporter {:id id})))

