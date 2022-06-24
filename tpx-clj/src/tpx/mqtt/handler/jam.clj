(ns tpx.mqtt.handler.jam
  (:require [songpark.mqtt :refer [handle-message]]
            [songpark.jam.tpx :as jam.tpx]
            [taoensso.timbre :as log]
            [tpx.data :as data]))


(defmethod handle-message :jam.cmd/start [{:keys [jam] :as msg}]
  (when (data/allowed? msg)
    (jam.tpx/join jam msg)))

(defmethod handle-message :jam.cmd/stop [{:keys [jam] :as msg}]
  (when (data/allowed? msg)
    (jam.tpx/leave jam)))
