(ns tpx.ipc.handler
  (:require [clojure.string :as str]
            [songpark.mqtt :as mqtt]
            [songpark.jam.tpx.ipc :as tpx.ipc]
            [taoensso.timbre :as log]
            [tpx.data :as data]
            [tpx.versions :as versions]))

(defmulti handler (fn [data context] (:tpx/msg data)))

(defmethod handler :default [data context]
  (log/warn (:tpx/msg data) "is not handled" (dissoc data :tpx/msg)))

(defmethod handler :bp/ready [data context]
  (log/debug :bp/ready))

(defmethod handler :bp/param [data context]
  (log/debug :bp/param (dissoc data :tpx/msg)))

(defmethod handler :setrpip [data _context]
  (log/info "Remote public ip has been set to" (:rpip data)))

(defmethod handler :setrlip [data _context]
  (log/info "Remote local ip has been set to" (:rlip data)))

(defmethod handler :setpip [data _context]
  (log/info "Public ip has been set to" (:pip data)))

(defmethod handler :setlip [data _context]
  (log/info "Local ip has been set to" (:lip data)))

(defmethod handler :setport [data _context]
  (log/info "Port has been set to" (:port data)))

(defmethod handler :call/params [data _context]
  (log/info :call/params (dissoc data :tpx/msg)))

(defmethod handler :call/param [data _context]
  (log/info :call/param (dissoc data :tpx/msg)))

(defmethod handler :call/state [data _context]
  (log/info :call/state (dissoc data :tpx/msg)))

(defmethod handler :port_in_use [data _context]
  nil)

(defmethod handler :stream/broken [data {:keys [ipc] :as _context}]
  (tpx.ipc/handler ipc :stream/broken true))

(defmethod handler :stream/streaming [data {:keys [ipc] :as _context}]
  (tpx.ipc/handler ipc :stream/streaming true))

(defmethod handler :stream/stopped [data {:keys [ipc] :as _context}]
  (tpx.ipc/handler ipc :stream/stopped true))

(defmethod handler :sync/syncing [{:keys [wait]} {:keys [ipc] :as _context}]
  (tpx.ipc/handler ipc :sync/syncing (or wait true)))

(defmethod handler :sync/sync-failed [data {:keys [ipc] :as _context}]
  (tpx.ipc/handler ipc :sync/sync-failed true))

(defmethod handler :sync/synced [data {:keys [ipc] :as _context}]
  (tpx.ipc/handler ipc :sync/synced true))

(defmethod handler :sync/responded [data {:keys [ipc] :as _context}]
  (tpx.ipc/handler ipc :sync/responded true))


(defn handle-sip-making-call [data {:keys [ipc start-coredump] :as _context}]
  (log/debug :handle-sip-making-call data)
  (tpx.ipc/handler ipc :sip/making-call true)
  (start-coredump))


(defn handle-sip-call-started [data {:keys [ipc start-coredump] :as _context}]
  (log/debug :handle-sip-call-started data)
  (tpx.ipc/handler ipc :sip/calling true)
  (start-coredump))

(defn- extract-coredump-data [data]
  (->> (str/split data #" \| ")
       (map (fn [token] (str/replace token #" " "=_=_")))
       (map (fn [token] (str/split token #":=_=_")))
       (map (fn [v] {(keyword (str/replace (first v) #"=_=_" "_"))
                     (-> (last v)
                         (str/replace #"=_=_" " ")
                         (str/replace #" ms" ""))}))
       (into {})))

(def ^:private reported-versions (atom {}))

(defn handle-versions [data {:versions/keys [save-versions
                                             current-versions
                                             get-versions]
                             broadcast-presence :broadcast-presence/fn
                             :as _context}]
  (log/debug :versions data)

  (when-let [fpga-version (:fpga-version data)]
    (log/debug "swapping FPGA version")
    (swap! reported-versions assoc :fpga-version fpga-version))
  (when-let [bp-version (:bp-version data)]
    (log/debug "swapping BP version")
    (swap! reported-versions assoc :bp-version bp-version))

  (when (and (contains? @reported-versions :fpga-version)
             (contains? @reported-versions :bp-version))
    (let [{:teleporter/keys [fpga-version bp-version]} current-versions]
      (when (or (not= fpga-version (:fpga-version @reported-versions))
                (not= bp-version (:bp-version @reported-versions)))
        (save-versions @reported-versions)
        (broadcast-presence (fn [_]
                              (log/info "Broadcasted change in FPGA/BP version"))
                            (fn [_]
                              (log/error "Failed to broadcast change in FPGA/BP version")))))))

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
