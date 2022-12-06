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

(defmethod handler :stream/volume [data context]
  (log/info :stream/volume (dissoc data :tpx/msg)))

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

(defmethod handler :call_param/reset [data _context]
  (log/info :call_param/reset (dissoc data :tpx/msg)))

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

(defmethod handler :sync/failed [data {:keys [ipc] :as _context}]
  (tpx.ipc/handler ipc :sync/failed true))

(defmethod handler :sync/synced [data {:keys [ipc] :as _context}]
  (tpx.ipc/handler ipc :sync/synced true))

(defmethod handler :sync/responded [data {:keys [ipc] :as _context}]
  (tpx.ipc/handler ipc :sync/responded true))

(defmethod handler :sync/timeout [data {:keys [ipc] :as _context}]
  (tpx.ipc/handler ipc :sync/timeout true))

(defmethod handler :sync/deinit [data {:keys [ipc] :as _context}]
  )

(defmethod handler :sync/stopped [data {:keys [ipc] :as _context}]
  )

(defmethod handler :call/params/reset [data {:keys [ipc] :as _context}]
  )


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

(defn- extract-coredump-data [data]
  (->> data
       (map (fn [[k v]]
              (if (string? v)
                [k (str/replace v #" ms" "")]
                [k v])))
       (into {})))

(comment
  (extract-coredump-data {:LTC -1521595943,
                          :TX-Packets-per-second 0,
                          :RX-Packets-per-second 0,
                          :Latency "1.90 ms",
                          :StreamStatus 1,
                          :DDiffMS "18.99 ms",
                          :RTC -1521597766,
                          :DDiffCC 1823})
  )

(defmethod handler :coredump [data {:keys [ipc]}]
  (let [coredump-data (extract-coredump-data data)]
    (tpx.ipc/handler ipc :jam/coredump coredump-data)))
