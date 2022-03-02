(ns tpx.ipc.handler
  (:require [clojure.string :as str]
            [songpark.mqtt :as mqtt]
            [songpark.jam.tpx.ipc :as tpx.ipc]
            [taoensso.timbre :as log]
            [tpx.data :as data]))


(defn handle-sip-register [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-register data)
  (tpx.ipc/handler ipc :sip/register true)
  #_(.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/sip
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :teleporter/sip :registered}}))

(defn handle-sip-making-call [data {:keys [ipc start-coredump] :as _context}]
  (log/debug :handle-sip-making-call data)
  (start-coredump)
  (tpx.ipc/handler ipc :sip/making-call true))

(defn handle-sip-calling [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-calling data)
  (tpx.ipc/handler ipc :sip/calling true))

(defn handle-sip-error-making-call [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-error-making-call data)
  (tpx.ipc/handler ipc :sip/error data))

(defn handle-sip-error-dialog-mutex [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-error-dialog-mutex data)
  (tpx.ipc/handler ipc :sip/error data))


(defn handle-sip-incoming-call [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-incoming-call data)
  (tpx.ipc/handler ipc :sip/incoming-call true))

(defn handle-sip-in-call [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-inc-call data)
  (tpx.ipc/handler ipc :sip/inc-call true))

(defn handle-sip-hangup [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-hangup data)
  (tpx.ipc/handler ipc :sip/hangup true))

(defn handle-sip-call-ended [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-call-ended data)
  (tpx.ipc/handler ipc :sip/call-ended true))

(defn handle-stream-broken [data {:keys [ipc] :as _context}]
  (log/debug :handle-stream-broken data)
  (tpx.ipc/handler ipc :stream/broken true))

(defn handle-stream-syncing [data {:keys [ipc] :as _context}]
  (log/debug :handle-stream-syncing data)
  (tpx.ipc/handler ipc :stream/syncing true))

(defn handle-stream-sync-failed [data {:keys [ipc] :as _context}]
  (log/debug :handle-stream-sync-failed data)
  (tpx.ipc/handler ipc :stream/sync-failed true))

(defn handle-stream-streaming [data {:keys [ipc] :as _context}]
  (log/debug :handle-stream-streaming data)
  (tpx.ipc/handler ipc :stream/streaming true))

(defn handle-stream-stopped [data {:keys [ipc] :as _context}]
  (log/debug :handle-stream-stopped data)
  (tpx.ipc/handler ipc :stream/stopped true))

(defn handle-gain-input-global-gain [{:keys [loopback network] :as _data}
                                     {:keys [ipc] :as _context}]
  (log/debug :gain-input-global-gain _data)
  (tpx.ipc/handler ipc :volume/global-volume "FIXME")
  #_(.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/global-volume
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :teleporter/loopback-volume loopback
                                                        :teleporter/network-volume network}}))

(defn handle-gain-input-left-gain [data {:keys [ipc] :as _context}]
  ;; we ignore left for now, the right is what matters as we set them to the same value,
  ;; but set the right value last
  (log/debug :gain-input-left-gain data))

(defn handle-gain-input-right-gain [{:keys [loopback network] :as _data}
                                    {:keys [ipc] :as _context}]
  (log/debug :gain-input-right-gain _data)
  ;; (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/network-volume
  ;;                                        :message/body {:teleporter/id (data/get-tp-id)
  ;;                                                       :teleporter/network-volume network}})
  ;; (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/local-volume
  ;;                                        :message/body {:teleporter/id (data/get-tp-id)
  ;;                                                       :teleporter/local-volume network}})
  (tpx.ipc/handler ipc :volume/network-volume "FIXME")
  (tpx.ipc/handler ipc :volume/locale-volume "FIXME"))

(defn handle-sip-call-started [data {:keys [ipc] start-coredump :start-coredump :as _context}]
  (log/debug :handle-sip-call-started data)
  (tpx.ipc/handler ipc :sip/calling true)
  (start-coredump)
  ;; (.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/call-started
  ;;                                        :message/body {:teleporter/id (data/get-tp-id)
  ;;                                                       :sip/data data}})
  )

(defn handle-sip-call-stopped [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-call-stopped data)
  (tpx.ipc/handler ipc :sip/stop true)
  #_(.publish mqtt-manager (data/get-jam) {:message/type :teleporter.status/call-stopped
                                         :message/body {:teleporter/id (data/get-tp-id)
                                                        :sip/data data}}))

(defn handle-coredump [data {:keys [ipc] :as _context}]
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
    (tpx.ipc/handler ipc :jam/coredump coredump-data)
    #_(.publish mqtt-manager (data/get-tp-coredump-topic) {:message/type :teleporter/coredump
                                                         :message/body {:teleporter/id (data/get-tp-id)
                                                                        :teleporter/coredump-data coredump-data}})))

(defn handle-log [data {:keys [ipc] :as _context}]
  (log/debug :handle-log data)
  #_(.publish mqtt-manager (data/get-tp-log-topic) {:message/type :teleporter/log
                                                  :message/body (assoc data :teleporter/id (data/get-tp-id))}))

(defn handle-local-ip [data _context]
  (log/debug :handle-local-ip data))

(defn handle-gateway-ip [data _context]
  (log/debug :handle-gateway-ip data))

(defn handle-netmask-ip [data _context]
  (log/debug :handle-netmask-ip data))
