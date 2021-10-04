(ns tpx.message.outgoing
  (:require [taoensso.timbre :as log]))


(defmulti handler :message/type)

(defmethod handler :teleporter.cmd/subscribe [{:message/keys [topics]
                                               :keys [mqtt-manager]}]
  (log/debug :handler (str "Subscribing to " (keys topics)))
  (.subscribe mqtt-manager topics))

(defmethod handler :platform.cmd/unsubscribe [{:message/keys [topics]
                                               :keys [mqtt-manager]}]
  (log/debug :handler (str "Unsubscribing from " (keys topics)))
  (.unsubscribe mqtt-manager topics))

(defmethod handler :teleporter.msg/info [{:message/keys [topic body]
                                          :keys [mqtt-manager]}]
  (.publish mqtt-manager topic body))

(defmethod handler :default [{:message/keys [type] :as message}]
  (throw
   (ex-info (str "No message handler defined for message type " type) message)))
