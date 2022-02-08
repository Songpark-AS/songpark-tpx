(ns tpx.heartbeat
  "Heartbeat sender, send a hearbeat periodically on mqtt"
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [tpx.data :as data]
            [chime.core :as chime])
  (:import (java.time Instant Duration)))

(defonce ^:private store (atom nil))
(defn send-message! [msg]
  (let [mqtt-manager (:mqtt-manager @store)
        injections (-> mqtt-manager
                       (select-keys (:injection-ks mqtt-manager))
                       (assoc :mqtt-manager mqtt-manager))]
    (.send-message! (:message-service injections) (merge msg injections))))

(defn set-interval [callback ms]
  (chime/chime-at (chime/periodic-seq (Instant/now) (Duration/ofMillis ms)) (fn[time] (callback time)))
  #_(future (while true (do (Thread/sleep ms) (callback)))))

(defn send-heartbeat []
  (log/debug ::send-heartbeat "sending heartbeat")
  (send-message! {:message/type :teleporter.cmd/send-heartbeat}))

(defrecord HeartbeatService [injection-ks started? config message-service mqtt-manager]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting HeartbeatService")
          (let [timer (get config :timer (* 60 1000))
                new-this (assoc this
                                :mqtt-manager mqtt-manager
                                :started? true
                                :job (set-interval (fn[_] (send-heartbeat)) timer))]
            (reset! store new-this)
            new-this))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping HeartbeatService")
          (let [new-this (assoc this
                                :started? false)]
            (.close (:job this))
            (reset! store nil)
            new-this)))))

(defn heartbeat-service [settings]
  (map->HeartbeatService settings))
