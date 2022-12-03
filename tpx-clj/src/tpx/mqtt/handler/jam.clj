(ns tpx.mqtt.handler.jam
  (:require [songpark.mqtt :refer [handle-message]]
            [songpark.jam.tpx :as jam.tpx]
            [taoensso.timbre :as log]))


(defmethod handle-message :jam.cmd/join [{:keys [tpx] :as msg}]
  (jam.tpx/join tpx msg))

(defmethod handle-message :jam.cmd/start [{:keys [tpx] :as msg}]
  (jam.tpx/start-call tpx msg))

(defmethod handle-message :jam.cmd/stop [{:keys [tpx] :as msg}]
  (jam.tpx/stop-call jam))

(defmethod handle-message :jam.cmd/reset [{:keys [tpx] :as msg}]
  (jam.tpx/reset tpx))
