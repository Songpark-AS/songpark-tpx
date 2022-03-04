(ns tpx.ipc.handler
  (:require [clojure.string :as str]
            [songpark.mqtt :as mqtt]
            [songpark.jam.tpx.ipc :as tpx.ipc]
            [taoensso.timbre :as log]
            [tpx.data :as data]))


(defn handle-sip-register [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-register data)
  (tpx.ipc/handler ipc :sip/register true))

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

(defn handle-sync-syncing [data {:keys [ipc] :as _context}]
  (log/debug :handle-sync-syncing data)
  (tpx.ipc/handler ipc :stream/syncing true))

(defn handle-sync-sync-failed [data {:keys [ipc] :as _context}]
  (log/debug :handle-sync-sync-failed data)
  (tpx.ipc/handler ipc :stream/sync-failed true))

(defn handle-stream-streaming [data {:keys [ipc] :as _context}]
  (log/debug :handle-stream-streaming data)
  (tpx.ipc/handler ipc :stream/streaming true))

(defn handle-stream-stopped [data {:keys [ipc] :as _context}]
  (log/debug :handle-stream-stopped data)
  (tpx.ipc/handler ipc :stream/stopped true))


(defn handle-sip-call-started [data {:keys [ipc] start-coredump :start-coredump :as _context}]
  (log/debug :handle-sip-call-started data)
  (tpx.ipc/handler ipc :sip/calling true)
  (start-coredump))

(defn handle-sip-call-stopped [data {:keys [ipc] :as _context}]
  (log/debug :handle-sip-call-stopped data)
  (tpx.ipc/handler ipc :sip/stop true))

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
    (tpx.ipc/handler ipc :jam/coredump coredump-data)))
