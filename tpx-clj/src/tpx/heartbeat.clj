(ns tpx.heartbeat
  "Heartbeat sender, send a hearbeat periodically on mqtt"
  (:require [chime.core :as chime]
            [com.stuartsierra.component :as component]
            [songpark.mqtt :as mqtt]
            [songpark.mqtt.util :as mqtt.util]
            [taoensso.timbre :as log]
            [tpx.utils :refer [upgrading-flag? delete-upgrading-flag]]
            [tpx.data :as data])
  (:import [java.time Instant Duration]))

;; We only want to check the upgrade flag once at start
;; not accidentally right after the upgrade process has begun
(defonce ^:private checked-upgrade (atom false))


(defn set-interval [callback ms]
  (chime/chime-at
   (chime/periodic-seq (Instant/now)
                       (Duration/ofMillis ms))
   (fn [time]
     (callback time))))

(defn upgrade-complete [mqtt-client]
  (log/debug ::upgrade-complete "Sending upgrade-complete message on MQTT")
  (let [topic (mqtt.util/broadcast-topic (data/get-tp-id))]
    (mqtt/publish mqtt-client
                  topic
                  {:message/type :teleporter/upgrade-status
                   :teleporter/id (data/get-tp-id)
                   :teleporter/upgrade-status "complete"}))
  (delete-upgrading-flag))

(defn check-upgrade [mqtt-client]
  (log/debug ::check-upgrade "Checking if upgrading-flag is set")
  (when (upgrading-flag?)
    (upgrade-complete mqtt-client))
  (reset! checked-upgrade true))

(defn send-apt-version [mqtt-client]
  (log/debug ::send-apt-version "Sending apt-version")
  (let [topic (mqtt.util/broadcast-topic (data/get-tp-id))]
    (mqtt/publish mqtt-client
                  topic
                  {:message/type :teleporter/apt-version
                   :teleporter/id (data/get-tp-id)
                   :teleporter/apt-version (data/get-apt-version)})))

(defn send-heartbeat [mqtt-client]
  (when-not @checked-upgrade (do (check-upgrade mqtt-client)
                                 (send-apt-version mqtt-client)))
  (log/debug ::send-heartbeat "Sending heartbeat")
  (let [topic (mqtt.util/heartbeat-topic (data/get-tp-id))]
    (mqtt/publish mqtt-client topic
                  {:message/type :teleporter/heartbeat
                   :teleporter/id (data/get-tp-id)})))


(defrecord HeartbeatService [started? config job mqtt-client]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting HeartbeatService")
          (let [timer (get config :timer (* 60 1000))]
            (assoc this
                   :started? true
                   :job (set-interval (fn [_]
                                        (send-heartbeat mqtt-client))
                                      timer))))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping HeartbeatService")
          (.close job)
          (assoc this
                 :started? false)))))

(defn heartbeat-service [settings]
  (map->HeartbeatService settings))


(comment

  (let [mqtt-client (:mqtt-client @tpx.init/system)]
    (mqtt/publish mqtt-client
                  (mqtt.util/heartbeat-topic (data/get-tp-id))
                  {:message/type :teleporter/heartbeat
                   :teleporter/id (data/get-tp-id)}))
  )
