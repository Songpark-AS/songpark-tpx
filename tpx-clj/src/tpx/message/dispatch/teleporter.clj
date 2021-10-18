(ns tpx.message.dispatch.teleporter
  (:require [tpx.message.dispatch.interface :as message]
            [tpx.ipc.serial :as serial]
            [tpx.utils :refer [scale-value]]
            [taoensso.timbre :as log]))



(defmethod message/dispatch :teleporter.cmd/subscribe [{:message/keys [meta]
                                                        :keys [mqtt-manager]}]
  (let [topics (:mqtt/topics meta)]
    (log/debug :handler (str "Subscribing to " (keys topics)))
    (.subscribe mqtt-manager topics)))

(defmethod message/dispatch :teleporter.cmd/disconnect [{:message/keys [body]
                                                         :keys [message-service mqtt-manager]}]
  ;; remove teleporter from global store
  ;; send a service response to topic (might be jam)
  )


(defmethod message/dispatch :teleporter.cmd.volume/global [{:teleporter/keys [volume]}]
  (let [volume (int (* volume 100))] 
    (log/debug ::set-global-volume "volume: " volume)
    #_(serial/send-command (:port @serial/config) "vol" volume)))

(defmethod message/dispatch :teleporter.cmd/balance [{:teleporter/keys [balance]}]
  (let [balance (int (scale-value balance [0 1] [-50 50]))] 
    (log/debug ::set-balance "balance: " balance)
    #_(serial/send-command (:port @serial/config) "bal" balance))
  )

;; send an informational message to teleporter topics
(defmethod message/dispatch :teleporter.msg/info [{:message/keys [body]
                                                   :keys [mqtt-manager]}]
  (let []
    (log/debug body))

  #_(.publish mqtt-manager topics body))


(defmethod message/dispatch :teleporter.msg/ipv4 [{:message/keys [values]}]
  (log/debug "Got new IPv4 config" values))


(comment
  ;; MESSAGE FORMAT
  {:message/type :some/key

   :message/body {} ;; from mqtt payload
   ;; example body

   :message/meta {:origin ""
                  :reply-to []
                  :mqtt/topic ""}}

  )
