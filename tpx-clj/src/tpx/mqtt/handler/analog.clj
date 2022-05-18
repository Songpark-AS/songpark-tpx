(ns tpx.mqtt.handler.analog
  (:require [taoensso.timbre :as log]
            [tpx.analog :as analog]
            [tpx.data :as data]
            [songpark.mqtt :as mqtt :refer [handle-message]]
            [songpark.mqtt.util :refer [broadcast-topic]]
            [tpx.sync :refer [sync-platform!]]))


(defmethod handle-message :teleporter.cmd/gain [{:teleporter/keys [gain value id]
                                                 :keys [gpio mqtt-client]}]
  (if (data/same-tp? id)
    (do (log/debug ::teleporter-cmd-gain {:gain gain
                                          :value value})
        (analog/write-gain gpio gain value)
        (sync-platform! {:mqtt-client mqtt-client
                         :topic (broadcast-topic id)
                         :message {:message/type :teleporter/gain
                                   :teleporter/id id
                                   :teleporter/gain gain
                                   :teleporter/value value}}))
    (log/debug ::teleporter-cmd-gain-wrong-teleporter {:teleporter/id id})))


(defmethod handle-message :teleporter.cmd/relay [{:teleporter/keys [id relay value]
                                                  :keys [gpio mqtt-client]}]
  (if (data/same-tp? id)
    (do (log/debug ::teleporter-cmd-relay {:relay relay
                                           :value value})
        (analog/write-relay gpio relay value)
        (sync-platform! {:mqtt-client mqtt-client
                         :topic (broadcast-topic id)
                         :message {:message/type :teleporter/relay
                                   :teleporter/id id
                                   :teleporter/relay relay
                                   :teleporter/value value}}))
    (log/debug ::teleporter-cmd-relay-wrong-teleporter {:teleporter/id id})))
