(ns tpx.message.outgoing
  (:require [taoensso.timbre :as log]))


(defmulti handler :message/type)

(defmethod handler :teleporter.cmd/subscribe [{:message/keys [topics]
                                               :keys [mqtt]}]
  (log/debug :handler (str "Subscribing to " (keys topics)))
  (.subscribe mqtt topics))

(defmethod handler :platform.cmd/unsubscribe [{:message/keys [topics]
                                               :keys [mqtt]}]
  (log/debug :handler (str "Unsubscribing from " (keys topics)))
  (.unsubscribe mqtt topics))

(defmethod handler :teleporter.msg/info [{:message/keys [topic body]
                                          :keys [mqtt]}]
  (.publish mqtt topic body))

(defmethod handler :default [{:message/keys [type] :as message}]
  (throw
   (ex-info (str "No message handler defined for message type " type) message)))
