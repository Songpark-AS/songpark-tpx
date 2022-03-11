(ns tpx.ipc.handler
  (:require [clojure.string :as str]
            [songpark.mqtt :as mqtt]
            [songpark.jam.tpx.ipc :as tpx.ipc]
            [taoensso.timbre :as log]
            [tpx.data :as data]))


(defn handle-sip-register [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-register data)
  (tpx.ipc/handler ipc :sip/register true))

(defn handle-sip-making-call [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-making-call data)
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

(defn handle-sip-incoming-call [data {:keys [ipc start-coredump] :as _context}]
  (log/debug :handle-sip-incoming-call data)
  (tpx.ipc/handler ipc :sip/incoming-call true)
  (start-coredump))

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

(defn handle-sync-syncing [data {:keys [ipc] :as _context}]
  (log/debug :handle-sync-syncing data)
  (tpx.ipc/handler ipc :sync/syncing true))

(defn handle-sync-sync-failed [data {:keys [ipc] :as _context}]
  (log/debug :handle-sync-sync-failed data)
  (tpx.ipc/handler ipc :sync/sync-failed true))

(defn handle-sync-synced [data {:keys [ipc] :as _context}]
  (log/debug :handle-sync-synced data)
  (tpx.ipc/handler ipc :sync/synced true))


(defn handle-stream-streaming [data {:keys [ipc start-coredump] :as _context}]
  (log/debug :handle-stream-streaming data)
  (tpx.ipc/handler ipc :stream/streaming true)
  (start-coredump))

(defn handle-stream-stopped [data {:keys [ipc] :as _context}]
  (log/debug :handle-stream-stopped data)
  (tpx.ipc/handler ipc :stream/stopped true))


(defn handle-sip-call-started [data {:keys [ipc start-coredump] :as _context}]
  (log/debug :handle-sip-call-started data)
  (tpx.ipc/handler ipc :sip/calling true)
  (start-coredump))

(defn handle-sip-call-stopped [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-call-stopped data)
  (tpx.ipc/handler ipc :sip/stop true))

(defn- extract-coredump-data [data]
  (->> (str/split data #" \| ")
       (map (fn [token] (str/replace token #" " "=_=_")))
       (map (fn [token] (str/split token #":=_=_")))
       (map (fn [v] {(keyword (str/replace (first v) #"=_=_" "_"))
                     (-> (last v)
                         (str/replace #"=_=_" " ")
                         (str/replace #" ms" ""))}))
       (into {})))

(comment
  (extract-coredump-data "Latency: 0.00 ms | LTC: 348043984 | RTC: 348043682 | StreamStatus: 1 | RX Packets-per-second: 0 | TX Packets-per-second: 0 | DDiffMS: 3.15 ms | DDiffCC: 302")
  )

(defn handle-coredump [data {:keys [ipc] :as _context}]
  (log/debug :handle-coredump data)

  ;; Split the string on " | "
  ;; Replace " " with "=_=_" since we cannot have keywords with spaces
  ;; Split on ":=_=_" to get the key and value pairs
  ;; Keywordize the key with _ instead of spaces
  ;; replace "=_=_" back to " " on the values
  ;; reduce into a single map
  (let [coredump-data (extract-coredump-data data)]
    ;; (log/debug :handle-coredump coredump-data)
    (tpx.ipc/handler ipc :jam/coredump coredump-data)))
