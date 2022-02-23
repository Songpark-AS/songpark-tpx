(ns tpx.mqtt.handler.teleporter
  (:require [songpark.mqtt :refer [handle-message]]
            [taoensso.timbre :as log]
            [tpx.data :as data]))

(defmethod handle-message :teleporter.cmd/global-volume [{:teleporter/keys [id volume] :keys [ipc]}]
  (if (data/same-tp? id)
    (do (log/debug ::set-global-volume {:volume volume})
        )))
