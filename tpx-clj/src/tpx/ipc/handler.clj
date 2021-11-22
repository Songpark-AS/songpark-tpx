(ns tpx.ipc.handler
  (:require [taoensso.timbre :as log]
            [tpx.data :as data]))


(defn handle-sip-registered [data {:keys [mqtt-manager] :as _context}]
  (log/debug :handle-sip-registered data)
  (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/sip
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :teleporter/sip :registered}}))

(defn handle-sip-call [data {:keys [mqtt-manager] :as _context}]
  (log/debug :handle-sip-call data))

(defn handle-gain-input-global-gain [{:keys [loopback network] :as _data}
                                     {:keys [mqtt-manager] :as _context}]
  (log/debug :gain-input-global-gain _data)
  (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/global-volume
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :teleporter/loopback-volume loopback
                                                        :teleporter/network-volume network}}))

(defn handle-gain-input-left-gain [data context]
  ;; we ignore left for now, the right is what matters as we set them to the same value,
  ;; but set the right value last
  (log/debug :gain-input-left-gain data))

(defn handle-gain-input-right-gain [{:keys [loopback network] :as _data}
                                    {:keys [mqtt-manager] :as _context}]
  (log/debug :gain-input-right-gain _data)
  (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/network-volume
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :teleporter/network-volume network}})
  (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/local-volume
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :teleporter/local-volume network}}))

(defn handle-sip-call-started [data {:keys [mqtt-manager] :as _context}]
  (log/debug :handle-sip-call-started data)
  (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/call-started
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :sip/data data}}))

(defn handle-sip-call-stopped [data {:keys [mqtt-manager] :as _context}]
  (log/debug :handle-sip-call-stopped data)
  (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/call-stopped
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :sip/data data}}))

(defn handle-log [data {:keys [mqtt-manager] :as _context}]
  (log/debug :handle-log data)
  (.publish mqtt-manager (data/get-tp-log-topic) {:message/type :teleporter/log
                                                  :message/body (assoc data :teleporter/id (data/get-tp-id))}))
