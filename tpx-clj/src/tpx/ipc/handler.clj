(ns tpx.ipc.handler
  (:require [taoensso.timbre :as log]
            [tpx.data :as data]))


(defn handle-sip-registered [data {:keys [mqtt-manager] :as _context}]
  (log/debug :handle-sip-registered data)
  (.publish mqtt-manager (data/get-jam) {:message/type :teleporter/sip
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :teleporter/sip :registered}}))

(defn handle-sip-call [data {:keys [mqtt-manager] :as _context}]
  (log/debug :handle-sip-call data))

(defn handle-gain-input-global-gain [data {:keys [mqtt-manager] :as _context}]
  (log/debug :gain-input-global-gain data))

(defn handle-gain-input-left-gain [data context]
  (log/debug :gain-input-left-gain data))

(defn handle-gain-input-right-gain [data context]
  (log/debug :gain-input-right-gain data))

(defn handle-sip-call-started [data {:keys [mqtt-manager] :as _context}]
  (log/debug :handle-sip-call-started data)
  (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/call
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :sip/data data}}))

(defn handle-sip-call-stopped [data {:keys [mqtt-manager] :as _context}]
  (log/debug :handle-sip-call-stopped data)
  (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/hangup
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :sip/data data}}))
