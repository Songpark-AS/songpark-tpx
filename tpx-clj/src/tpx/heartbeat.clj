(ns tpx.heartbeat
  "Heartbeat sender, send a hearbeat periodically on mqtt"
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [tpx.utils :refer [upgrading-flag? delete-upgrading-flag]]
            [tpx.data :as data]
            [chime.core :as chime])
  (:import (java.time Instant Duration)))

(defonce ^:private store (atom nil))

;; We only want to check the upgrade flag once at start
;; not accidentally right after the upgrade process has begun
(defonce ^:private checked-upgrade (atom false))

(defn send-message! [msg]
  (let [mqtt-manager (:mqtt-manager @store)
        injections (-> mqtt-manager
                       (select-keys (:injection-ks mqtt-manager))
                       (assoc :mqtt-manager mqtt-manager))]
    (.send-message! (:message-service injections) (merge msg injections))))

(defn set-interval [callback ms]
  (chime/chime-at (chime/periodic-seq (Instant/now) (Duration/ofMillis ms)) (fn[time] (callback time)))
  #_(future (while true (do (Thread/sleep ms) (callback)))))

(defn upgrade-complete []
  (log/debug ::upgrade-complete "Sending upgrade-complete message on MQTT")
  (send-message! {:message/type :teleporter.cmd/send-upgrade-complete})
  (delete-upgrading-flag))

(defn check-upgrade []
  (log/debug ::check-upgrade "Checking if upgrading-flag is set")
    (when (upgrading-flag?)
      (upgrade-complete))
  (reset! checked-upgrade true))

(defn send-heartbeat []
  (when-not @checked-upgrade (check-upgrade))
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
