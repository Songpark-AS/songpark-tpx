(ns tpx.message.incoming
  (:require [taoensso.timbre :as log]))


(defmulti handler :message/type)

(defmethod handler :teleporter.cmd/disconnect [{:message/keys [topic]
                                                :keys [message-service mqtt]}]
  ;; remove teleporter from global store
  ;; send a service response to topic (might be jam)
  )


(defmethod handler :debug/info [{:keys [message/topic message/body]}]
  (log/debug :handler.debug [topic body]))

(defmethod handler :default [message]
  (let [msg-type (:message/type message)]
    (throw (ex-info (str "No message handler exist for message type " msg-type) message))))




(comment
  

  )
