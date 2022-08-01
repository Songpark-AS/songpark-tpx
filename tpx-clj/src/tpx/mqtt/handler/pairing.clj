(ns tpx.mqtt.handler.pairing
  (:require [taoensso.timbre :as log]
            [tpx.data :as data]
            [tpx.pairing :as pairing]
            [songpark.mqtt :as mqtt :refer [handle-message]]))

(defmethod handle-message :pairing/pair [{id :teleporter/id
                                          user-id :auth.user/id
                                          gpio :gpio}]
  (pairing/set-status gpio :pairing)
  (data/set-user-id! user-id))


(defmethod handle-message :pairing/unpair [{id :teleporter/id
                                            user-id :auth.user/id
                                            gpio :gpio
                                            :as msg}]
  (when (data/allowed? msg)
    (pairing/set-status gpio :unpaired)
    (data/set-user-id! nil)))
