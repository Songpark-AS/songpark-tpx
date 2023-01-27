(ns tpx.mqtt.handler.pairing
  (:require [songpark.common.communication :refer [POST]]
            [taoensso.timbre :as log]
            [tpx.data :as data]
            [tpx.pairing :as pairing]
            [tpx.utils :as util]
            [songpark.mqtt :as mqtt :refer [handle-message]]))

(defmethod handle-message :pairing/pair [{id :teleporter/id
                                          user-id :auth.user/id
                                          gpio :gpio}]
  (log/debug ::pairing-pair {:teleporter/id id
                             :auth.user/id user-id
                             :pairing/paired? (pairing/paired?)})
  (when-not (pairing/paired?)
    (pairing/set-status gpio :pairing)
    (data/set-shadow-user-id! user-id)))


(defmethod handle-message :pairing/unpair [{id :teleporter/id
                                            user-id :auth.user/id
                                            gpio :gpio
                                            :as msg}]
  (log/debug ::pairing-unpair {:teleporter/id id
                               :auth.user/id user-id})
  (when (data/allowed? msg)
    (pairing/set-status gpio :unpaired)
    (data/set-user-id! nil)))
