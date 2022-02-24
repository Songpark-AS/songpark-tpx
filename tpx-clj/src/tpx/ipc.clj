(ns tpx.ipc
  "Interactive Process Communication"
  (:require [clojure.core.async :as async]
            [clojure.java.shell :refer [sh]]
            [com.stuartsierra.component :as component]
            [songpark.jam.tpx.ipc :as tpx.ipc]
            [songpark.mqtt :as mqtt]
            [taoensso.timbre :as log]
            [tpx.config :refer [config]]
            [tpx.data :as data]
            [tpx.ipc.command :as ipc.command]
            [tpx.ipc.handler :as ipc.handler]
            [tpx.ipc.serial :as ipc.serial]
            [songpark.common.communication :refer [PUT]]))




(defn- setup-serial-ports! [ipc mqtt-client context-data]
  (log/info "Setting up serial ports")
  (ipc.serial/connect-to-port (merge {:mqtt-client mqtt-client
                                      :ipc ipc}
                                     context-data)
                              {:sip/making-call #'ipc.handler/handle-sip-making-call
                               :sip/calling #'ipc.handler/handle-sip-calling
                               :sip/incoming-call #'ipc.handler/handle-sip-incoming-call
                               :sip/in-call #'ipc.handler/handle-sip-in-call
                               :sip/hangup #'ipc.handler/handle-sip-hangup
                               :sip/call-ended #'ipc.handler/handle-sip-call-ended
                               :sip/register #'ipc.handler/handle-sip-register

                               :stream/broken #'ipc.handler/handle-stream-broken
                               :stream/syncing #'ipc.handler/handle-stream-syncing
                               :stream/sync-failed #'ipc.handler/handle-stream-sync-failed
                               :stream/streaming #'ipc.handler/handle-stream-streaming
                               :stream/stopped #'ipc.handler/handle-stream-stopped
                               
                               :jam/coredump #'ipc.handler/handle-coredump
                               
                               :gain-input-global-gain #'ipc.handler/handle-gain-input-global-gain
                               :gain-input-left-gain #'ipc.handler/handle-gain-input-left-gain
                               :gain-input-right-gain #'ipc.handler/handle-gain-input-right-gain}))

(defn- command* [_ipc what data]
  (log/debug ::command* {:what what
                         :data data})
  (case what
    :sip/call (ipc.command/call-via-sip data)
    :sip/hangup (ipc.command/hangup-all)
    :volume/global-volume (ipc.command/global-volume data)
    :volume/network-volume (ipc.command/network-volume data)
    :volume/local-volume (ipc.command/local-volume data)
    :jam/path-reset (ipc.command/path-reset)
    :jam/playout-delay (ipc.command/set-playout-delay data)
    (log/error "Unknown command" {:what what
                                  :data data})))

(defn- handler* [c what data]
  ;; let TPX Jam handle the rest
  (let [value {:event/type what
               :event/value data}]
    (log/debug value)
    (async/put! c value)))

(defrecord IpcService [started? config mqtt-client c]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting IpcService")
          (let [this* (assoc this
                             :started? true
                             :c (async/chan (async/sliding-buffer 10)))]
            (setup-serial-ports! this* mqtt-client {:start-coredump #'ipc.command/start-coredump})
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

