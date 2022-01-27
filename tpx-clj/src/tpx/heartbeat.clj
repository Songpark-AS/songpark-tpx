(ns tpx.heartbeat
  "Heartbeat sender, send a hearbeat periodically on mqtt"
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [tpx.data :as data]))

(defonce ^:private store (atom nil))
(defn send-message! [msg]
  (let [heartbeat @store
        injections (-> heartbeat
                       (select-keys (:injection-ks heartbeat))
                       (assoc :heartbeat heartbeat))]
    (.send-message! (:message-service injections) (merge msg injections))))

(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

(defn send-heartbeat []
  (log/debug ::send-heartbeat "sending heartbeat")
  (send-message! {:message/type :teleporter.cmd/send-heartbeat}))

(defrecord HeartbeatService [injection-ks started? config message-service]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting HeartbeatService")
          (let [new-this (assoc this
                                :started? true
                                :job (set-interval #(send-heartbeat) (get config :timer (* 60 1000))))]
            (reset! store new-this)
            new-this))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping HeartbeatService")
          (let [new-this (assoc this
                                :started? false)]
            (future-cancel (:job this))
            (reset! store nil)
            new-this)))))

(defn heartbeat-service [settings]
  (map->HeartbeatService settings))
