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
            [tpx.versions :as versions]
            [songpark.common.communication :refer [PUT]]))

(defn- setup-serial-ports! [context]
  (log/info "Setting up serial ports")
  (ipc.serial/connect-to-port context))

(defn- command* [_ipc what data]
  (log/debug ::command* {:what what
                         :data data})
  (case what
    :volume/global-volume (do (codax/assoc-at! @db [what] data)
                              (ipc.command/global-volume data))
    :volume/network-volume (do (codax/assoc-at! @db [what] data)
                               (ipc.command/network-volume data))
    :volume/network-mute (ipc.command/network-mute data)
    :volume/local-volume (do (codax/assoc-at! @db [what] data)
                             (ipc.command/local-volume data))
    :volume/input1-volume (do (codax/assoc-at! @db [what] data)
                              (ipc.command/input1-volume data))
    :volume/input2-volume (do (codax/assoc-at! @db [what] data)
                              (ipc.command/input2-volume data))
    :volume/input1+2-volume (do (codax/assoc-at! @db [:volume/input1-volume] data)
                                (codax/assoc-at! @db [:volume/input2-volume] data)
                                (ipc.command/input1-volume data)
                                (ipc.command/input2-volume data))
    :call/receive (ipc.command/receive-call data)
    :call/initiate (ipc.command/initiate-call data)
    :call/stop (ipc.command/stop-call)
    :hangup/all (ipc.command/hangup-all)
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
    (when (= what :stream/stopped)
      (try
        (log/info "Doing a path reset at the end of a stopped stream")
        ;; (ipc.command/path-reset)
        (catch Throwable e
          (log/error "Tried to do a path reset when the stream stopped"
                     {:exception e
                      :message (ex-message e)
                      :data (ex-data e)}))))
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

(defn- set-ips [local-ip public-ip]
  (log/info "Setting local ip values for the FPGA" {:local-ip local-ip
                                                    :public-ip public-ip})
  (ipc.command/set-local-ip local-ip)
  (ipc.command/set-public-ip public-ip))

(defn set-versions!
  "Set BP and FPGA versions. This involves a complex little loop in logic to
  execute due to speed limitations in the integration between BP and TPX."
  []
  (log/info "Gathering versions from BP and FPGA")
  (ipc.command/gather-versions))

(defrecord IpcService [started? config mqtt-client c broadcast-presence]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting IpcService")

          ;; load versions first, in the hope that it has been executed before
          ;; and we have the versions already
          (versions/load-versions)

          (let [this* (assoc this
                             :started? true
                             :c (async/chan (async/sliding-buffer 10)))]
            (setup-serial-ports! {:ipc this*
                                  :mqtt-client mqtt-client
                                  :versions/current-versions @versions/data
                                  :versions/save-versions versions/save-versions
                                  :broadcast-presence/fn broadcast-presence
                                  :start-coredump #'ipc.command/start-coredump})
            (set-ips (data/get-local-ip) (data/get-public-ip))
            (set-versions!)
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
