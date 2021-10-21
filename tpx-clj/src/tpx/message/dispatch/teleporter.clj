(ns tpx.message.dispatch.teleporter
  (:require [tpx.message.dispatch.interface :as message]
            [tpx.ipc.command :as ipc.command]
            [tpx.data :as data]
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


(defmethod message/dispatch :teleporter.cmd/global-volume [{{:teleporter/keys [volume id]} :message/body}]
  (if (data/same-tp? id)
    (do
      (log/debug ::set-global-volume {:volume volume})
      (ipc.command/global-volume volume))
    (log/debug :set-global-volume-wrong-teleporter {:id id
                                                    :volume volume})))

(defmethod message/dispatch :teleporter.cmd/local-volume [{{:teleporter/keys [volume id]} :message/body}]
  (if (data/same-tp? id)
    (do
      (log/debug ::set-local-volume {:volume volume})
      (ipc.command/local-volume volume))
    (log/debug :set-local-volume-wrong-teleporter {:id id
                                                   :volume volume})))


(defmethod message/dispatch :teleporter.cmd/network-volume [{{:teleporter/keys [volume id]} :message/body}]
  (if (data/same-tp? id)
    (do
      (log/debug ::set-network-volume {:volume volume})
      (ipc.command/network-volume volume))
    (log/debug :set-network-volume-wrong-teleporter {:id id
                                                     :volume volume})))

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
