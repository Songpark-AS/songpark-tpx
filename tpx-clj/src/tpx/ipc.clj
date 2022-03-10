(ns tpx.ipc
  "Interactive Process Communication"
  (:require [codax.core :as codax]
            [clojure.core.async :as async]
            [clojure.java.shell :refer [sh]]
            [com.stuartsierra.component :as component]
            [songpark.jam.tpx.ipc :as tpx.ipc]
            [songpark.mqtt :as mqtt]
            [taoensso.timbre :as log]
            [tpx.config :refer [config]]
            [tpx.data :as data]
            [tpx.database :refer [db get-hardware-values]]
            [tpx.ipc.command :as ipc.command]
            [tpx.ipc.handler :as ipc.handler]
            [tpx.ipc.serial :as ipc.serial]
            [songpark.common.communication :refer [PUT]]))

(defn- setup-serial-ports! [context]
  (log/info "Setting up serial ports")
  (ipc.serial/connect-to-port context
                              {:sip/making-call #'ipc.handler/handle-sip-making-call
                               :sip/calling #'ipc.handler/handle-sip-calling
                               :sip/incoming-call #'ipc.handler/handle-sip-incoming-call
                               :sip/in-call #'ipc.handler/handle-sip-in-call
                               :sip/hangup #'ipc.handler/handle-sip-hangup
                               :sip/call-ended #'ipc.handler/handle-sip-call-ended
                               :sip/register #'ipc.handler/handle-sip-register
                               :sip/error-making-call #'ipc.handler/handle-sip-error-making-call
                               :sip/error-dialog-mutex #'ipc.handler/handle-sip-error-dialog-mutex

                               :stream/broken #'ipc.handler/handle-stream-broken
                               :sync/syncing #'ipc.handler/handle-sync-syncing
                               :sync/sync-failed #'ipc.handler/handle-sync-sync-failed
                               :stream/streaming #'ipc.handler/handle-stream-streaming
                               :stream/stopped #'ipc.handler/handle-stream-stopped
                               
                               :jam/coredump #'ipc.handler/handle-coredump}))

(defn- command* [_ipc what data]
  (log/debug ::command* {:what what
                         :data data})
  (case what
    :sip/call (ipc.command/call-via-sip data)
    :sip/hangup (ipc.command/hangup-all)
    :sip/hangup-all (ipc.command/hangup-all)
    :volume/global-volume (do (codax/assoc-at! @db [what] data)
                              (ipc.command/global-volume data))
    :volume/network-volume (do (codax/assoc-at! @db [what] data)
                               (ipc.command/network-volume data))
    :volume/local-volume (do (codax/assoc-at! @db [what] data)
                             (ipc.command/local-volume data))
    :jam/path-reset (ipc.command/path-reset)
    :jam/playout-delay (do (codax/assoc-at! @db [what] data)
                           (ipc.command/set-playout-delay data))
    :jam/stop-coredump (ipc.command/stop-coredump)
    (log/error "Unknown command" {:what what
                                  :data data})))

(defn- handler* [c what data]
  ;; let TPX Jam handle the rest
  (let [value {:event/type what
               :event/value data}]
    (log/debug value)
    (async/put! c value)))

(defn- init-hw-values! []
  (log/info "Setting default values for hardware")
  (doseq [[what value] (get-hardware-values)]
    (if value
      (do
        (log/info "Setting default value" {:what what
                                           :value value})
        (case what
          :volume/global-volume (ipc.command/global-volume value)
          :volume/network-volume (ipc.command/network-volume value)
          :volume/local-volume (ipc.command/local-volume value)
          :jam/playout-delay (ipc.command/set-playout-delay value)
          nil))
      (log/warn "Missing HW value" {:what what
                                    :value value}))))

(defrecord IpcService [started? config mqtt-client c]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting IpcService")
          (let [this* (assoc this
                             :started? true
                             :c (async/chan (async/sliding-buffer 10)))]
            (setup-serial-ports! {:ipc this*
                                  :mqtt-client mqtt-client
                                  :start-coredump #'ipc.command/start-coredump})
            (init-hw-values!)
            this*))))
  
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping IpcService")
          (ipc.serial/disconnect)
          (async/close! c)
          (assoc this
                 :started? false
                 :c nil))))

  tpx.ipc/IIPC
  (command [this what data]
    (command* this what data))
  (handler [this what]
    (throw (ex-info "Not implemented" {:what what})))
  (handler [this what data]
    (handler* c what data)))

(defn ipc-service [settings]
  (map->IpcService settings))


(comment

  )

