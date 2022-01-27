(ns tpx.ipc.handler
  (:require [taoensso.timbre :as log]
            [tpx.data :as data]
            [clojure.string :as str]))


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

(defn handle-sip-call-started [data {:keys [mqtt-manager] start-coredump :start-coredump :as _context}]
  (log/debug :handle-sip-call-started data)
  (start-coredump)
  (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/call-started
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :sip/data data}}))

(defn handle-sip-call-stopped [data {:keys [mqtt-manager] stop-coredump :stop-coredump :as _context}]
  (log/debug :handle-sip-call-stopped data)
  (stop-coredump)
  (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/call-stopped
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :sip/data data}}))

(defn handle-coredump [data {:keys [mqtt-manager] :as _context}]
  (log/debug :handle-coredump data)

  ;; Split the string on " | "
  ;; Replace " " with "=_=_" since we cannot have keywords with spaces
  ;; Split on ":=_=_" to get the key and value pairs
  ;; Keywordize the key with _ instead of spaces
  ;; replace "=_=_" back to " " on the values
  ;; reduce into a single map
  (let [coredump-data (reduce into {}
                              (map (fn [v] {(keyword (str/replace (first v) #"=_=_" "_")) (str/replace (last v) #"=_=_" " ")})
                                   (map #(str/split % #":=_=_")
                                        (map #(-> %
                                                  (str/replace #" " "=_=_"))
                                             (str/split data #" \| ")))))]
    ;; (log/debug :handle-coredump coredump-data)
    (.publish mqtt-manager (data/get-tp-coredump-topic) {:message/type :teleporter/coredump
                                                         :message/body {:teleporter/id (data/get-tp-id)
                                                                        :teleporter/coredump-data coredump-data}})))

(defn handle-log [data {:keys [mqtt-manager] :as _context}]
  (log/debug :handle-log data)
  (.publish mqtt-manager (data/get-tp-log-topic) {:message/type :teleporter/log
                                                  :message/body (assoc data :teleporter/id (data/get-tp-id))}))

(defn handle-local-ip [data _context]
  (log/debug :handle-local-ip data))

(defn handle-gateway-ip [data _context]
  (log/debug :handle-gateway-ip data))

(defn handle-netmask-ip [data _context]
  (log/debug :handle-netmask-ip data))
