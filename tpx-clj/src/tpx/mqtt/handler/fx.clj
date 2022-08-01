(ns tpx.mqtt.handler.fx
  (:require [taoensso.timbre :as log]
            [tpx.data :as data]
            [tpx.fx :as fx]
            [tpx.sync :refer [sync-platform!]]
            [songpark.mqtt :as mqtt :refer [handle-message]]))

(defmethod handle-message :fx.preset/set [{:keys [fx/fxs] :as msg}]
  (when (data/allowed? msg)
    (fx/set-fxs fxs)
    (sync-platform!)))
