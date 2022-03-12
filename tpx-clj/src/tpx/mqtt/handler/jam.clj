(ns tpx.mqtt.handler.jam
  (:require [songpark.mqtt :refer [handle-message]]
            [songpark.jam.tpx :as jam.tpx]
            [taoensso.timbre :as log]))


(defmethod handle-message :jam.cmd/start [{:keys [jam] :as msg}]
  (jam.tpx/join jam msg))

(defmethod handle-message :jam.cmd/stop [{:keys [jam]}]
  (jam.tpx/leave jam))
