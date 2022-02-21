(ns tpx.message.dispatch.teleporter
  (:require [tpx.message.dispatch.interface :as message]
            [tpx.network :refer [set-network!]]
            [tpx.ipc.command :as ipc.command]
            [tpx.data :as data]
            [tpx.utils :as utils]
            [tpx.network.reporter :as reporter]
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

(defmethod message/dispatch :teleporter.cmd/hangup-all [{{:teleporter/keys [id]} :message/body}]
  (if (data/same-tp? id)
    (do
      (log/debug ::hangup-all)
      (ipc.command/hangup-all))
    (log/debug ::hangup-all-wrong-teleporter {:id id})))

(defmethod message/dispatch :teleporter.cmd/path-reset [{{:teleporter/keys [id]} :message/body}]
  (if (data/same-tp? id)
    (do
      (log/debug ::path-reset)
      (ipc.command/path-reset))
    (log/debug ::path-reset-wrong-teleporter {:id id})))

(defmethod message/dispatch :teleporter.cmd/set-playout-delay [{{:teleporter/keys [id playout-delay]} :message/body}]
  (if (data/same-tp? id)
    (do
      (log/debug ::set-playout-delay playout-delay)
      (ipc.command/set-playout-delay playout-delay))
    (log/debug ::set-playout-delay-wrong-teleporter {:id id})))


(defmethod message/dispatch :teleporter.cmd/report-network-config [{:keys [mqtt-manager]}]
  (reporter/fetch-and-send-current-network-config mqtt-manager))


;; send an informational message to teleporter topics
(defmethod message/dispatch :teleporter.msg/info [{:message/keys [body]
                                                   :keys [mqtt-manager]}]
  (let []
    (log/debug body))

  #_(.publish mqtt-manager topics body))


(defmethod message/dispatch :teleporter.msg/ipv4 [{:message/keys [values]}]
  (log/debug "Got new IPv4 config" values)
  (set-network! (clojure.set/rename-keys values {:ip/address :ip :ip/gateway :gateway :ip/subnet :netmask :ip/dhcp? :dhcp?})))

;; (defmethod message/dispatch :teleporter.cmd/send-apt-version [{:keys [mqtt-manager]}]


;;   (.publish mqtt-manager (str (data/get-tp-id) "/apt-version") {:message/type :teleporter/apt-version
;;                                                                 :message/body {:teleporter/id (data/get-tp-id)
;;                                                                                :teleporter/apt-version (data/get-apt-version)}}))

;; (defmethod message/dispatch :teleporter.cmd/send-heartbeat [{:keys [mqtt-manager]}]
;;   (.publish mqtt-manager (str (data/get-tp-id) "/heartbeat") {:message/type :teleporter/heartbeat
;;                                            :message/body {:teleporter/id (data/get-tp-id)}}))


;; (defmethod message/dispatch :teleporter.cmd/send-upgrade-complete [{:keys [mqtt-manager]}]
;;   (.publish mqtt-manager (str (data/get-tp-id) "/upgrade-status") {:message/type :teleporter/upgrade-status
;;                                                                    :message/body {:teleporter/id (data/get-tp-id)
;;                                                                                   :teleporter/upgrade-status "complete"}}))

(defmethod message/dispatch :teleporter.cmd/upgrade [{{:teleporter/keys [id]} :message/body}]
  (if (data/same-tp? id)
    (do
      (log/debug ::upgrade)
      (utils/upgrade))
    (log/debug ::upgrade-wrong-teleporter {:id id})))

(comment
  ;; MESSAGE FORMAT
  (set-network! {:ip "1.1.1.1", :gateway "3.3.3.3", :netmask "4.4.4.4", :dhcp? false})
  {:message/type :some/key
   :message/body {} ;; from mqtt payload
   ;; example body

   :message/meta {:origin ""
                  :reply-to []
                  :mqtt/topic ""}}

  )
