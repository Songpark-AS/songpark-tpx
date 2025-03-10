(ns tpx.mqtt.handler.analog
  (:require [songpark.mqtt :as mqtt :refer [handle-message]]
            [songpark.mqtt.util :refer [broadcast-topic]]
            [taoensso.timbre :as log]
            [tpx.analog :as analog]
            [tpx.data :as data]
            [tpx.sync :refer [sync-platform!]]))


(defmethod handle-message :teleporter.cmd/gain [{:teleporter/keys [gain value id]
                                                 :keys [gpio mqtt-client]
                                                 :as msg}]
  (if (data/allowed? msg)
    (do (log/debug ::teleporter-cmd-gain {:gain gain
                                          :value value})
        (try
          (analog/write-gain gpio gain value)
          (sync-platform! {:mqtt-client mqtt-client
                           :topic (broadcast-topic id)
                           :message {:message/type :teleporter/gain
                                     :teleporter/id id
                                     :teleporter/gain gain
                                     :teleporter/value value}})
          (catch Exception e
            (log/error ::teleporter-cmd-gain e))))
    (log/debug ::teleporter-cmd-gain-wrong-teleporter {:teleporter/id id})))

(defmethod handle-message :teleporter.cmd/switch [{:teleporter/keys [switch value id]
                                                   :keys [gpio mqtt-client]
                                                   :as msg}]
  (if (data/allowed? msg)
    (do (log/debug ::teleporter-cmd-switch {:swith switch
                                            :value value})
        (try
          (analog/switch-input gpio switch value)
          (sync-platform! {:mqtt-client mqtt-client
                           :topic (broadcast-topic id)
                           :message {:message/type :teleporter/switch
                                     :teleporter/id id
                                     :teleporter/switch switch
                                     :teleporter/value value}})
          (catch Exception e
            (log/error ::teleporter-cmd-switch e))))
    (log/debug ::teleporter-cmd-switch-teleporter {:teleporter/id id})))


(defmethod handle-message :teleporter.cmd/relay [{:teleporter/keys [id relay value]
                                                  :keys [gpio mqtt-client]
                                                  :as msg}]
  (if (data/allowed? msg)
    (do (log/debug ::teleporter-cmd-relay {:relay relay
                                           :value value})
        (try
          (analog/write-relay gpio relay value)
          (sync-platform! {:mqtt-client mqtt-client
                           :topic (broadcast-topic id)
                           :message {:message/type :teleporter/relay
                                     :teleporter/id id
                                     :teleporter/relay relay
                                     :teleporter/value value}})
          (catch Exception e
            (log/error ::teleporter-cmd-relay e))))
    (log/debug ::teleporter-cmd-relay-wrong-teleporter {:teleporter/id id})))
