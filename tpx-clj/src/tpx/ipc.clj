(ns tpx.ipc
  "Interactive Process Communication"
  (:require [clojure.java.shell :refer [sh]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [tpx.config :refer [config]]
            [tpx.data :as data]
            [tpx.ipc.command :as ipc.command]
            [tpx.ipc.handler :as ipc.handler]
            [tpx.ipc.serial :as ipc.serial]
            [songpark.common.communication :refer [PUT]]))

(defonce ^:private store (atom nil))

(defn- get-device-mac []
  (:mac config))

(defn send-message! [msg]
  (let [ipc @store
        injections (-> ipc
                       (select-keys (:injection-ks ipc))
                       (assoc :ipc ipc))]
    (.send-message! (:message-service injections) (merge msg injections))))

(defn broadcast-presence [config]
  (log/info "Broadcasting presence to Platform")
  (PUT (str (:platform config) "/api/teleporter")
       {:teleporter/nickname (get-in config [:teleporter :nickname])
        :teleporter/mac (get-device-mac)}
       (fn [{:teleporter/keys [uuid] :as response}]
         (data/set-tp-id! uuid)
         (send-message! {:message/type :teleporter.cmd/subscribe
                         :message/meta {:mqtt/topics {(str uuid) 0}}}))))

(defn- setup-serial-ports! [mqtt-manager]
  (log/info "Setting up serial ports")
  (ipc.serial/connect-to-port {:mqtt-manager mqtt-manager}
                              {:sip-call-started #'ipc.handler/handle-sip-call-started
                               :sip-call-stopped #'ipc.handler/handle-sip-call-stopped
                               :sip-registered #'ipc.handler/handle-sip-registered
                               :sip-call #'ipc.handler/handle-sip-call
                               :log #'ipc.handler/handle-log
                               :gain-input-global-gain #'ipc.handler/handle-gain-input-global-gain
                               :gain-input-left-gain #'ipc.handler/handle-gain-input-left-gain
                               :gain-input-right-gain #'ipc.handler/handle-gain-input-right-gain}))

(defn- set-hw-defaults! []
  ;; set 
  (ipc.command/set-playout-delay (get-in config [:defaults :playout-delay] 20)))

(defrecord IpcService [injection-ks started? config message-service mqtt-manager]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting IpcService")          
          (let [new-this (assoc this
                                :started? true)]
            (reset! store new-this)
            (broadcast-presence config)
            (setup-serial-ports! mqtt-manager)
            (set-hw-defaults!)
            new-this))))
  
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping IpcService")
          (let [new-this (assoc this
                                :started? false)]
            (reset! store nil)
            (ipc.serial/disconnect)
            new-this)))))

(defn ipc-service [settings]
  (map->IpcService settings))


(comment
  (pr-str (:teleporter (:config @store)))
  @(broadcast-presence (:config @store))
  
  (get-device-mac)



  )

