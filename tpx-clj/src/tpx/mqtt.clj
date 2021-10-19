(ns tpx.mqtt
  (:require [com.stuartsierra.component :as component]
            [cognitect.transit :as transit]
            [taoensso.timbre :as log]
            [songpark.common.communication :refer [write-handlers]]
            [songpark.common.protocol.mqtt.manager :as protocol.mqtt.manager]
            [tpx.mqtt.client :as mqtt.client])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))


(defonce ^:private store (atom nil))

;; transit reader/writer from/to string, since
;; mosquitto does not know anything about transit
(defn- ->transit [v]
  (let [out (ByteArrayOutputStream. 4096)]
    (transit/write (transit/writer out :json {:handlers write-handlers}) v)
    (.toString out "utf-8")))

(defn- <-transit [b]
  (try
    (transit/read (transit/reader (ByteArrayInputStream. b) :json))
    (catch Exception e (do (log/warn "Message not in transit format")
                           (apply str (map char b))))))

(defn- on-message [^String topic _ ^bytes payload]
  (let [mqtt-manager @store
        injections (-> mqtt-manager
                       (select-keys (:injection-ks mqtt-manager))
                       (assoc :mqtt-manager mqtt-manager))]
    (->> (merge {:message/meta {:origin :mqtt :topic topic}
                 :message/topic topic}
                (<-transit payload)
                (merge injections))
         (.send-message! (:message-service injections)))))

(defn- subscribe* [{:keys [client] :as mqtt-manager} topics]
  (.subscribe client topics on-message))

(defn- unsubscribe* [{:keys [client] :as mqtt-manager} topics]
  (.unsubscribe client topics))

(defn- publish* [{:keys [client] :as mqtt-manager} topic msg]
  (.publish client topic (->transit msg)))

(defrecord MQTTManager [injection-ks started? config message-service]
  component/Lifecycle
  (start [this]
    (if started?
      this      
      (do
        (log/info "Starting MQTTManager")          
        (let [new-this (assoc this
                              :started? true
                              :client (mqtt.client/create (assoc config :on-message on-message)))]
          (reset! store new-this)
          new-this))))
  
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping MQTTManager")
          (let [new-this (assoc this :started? false)]
            (when (.connected? (:client this))            
              (.disconnect (:client this)))
            (reset! store this)
            new-this))))

  protocol.mqtt.manager/IMqttManager
  (subscribe [this topics]
    (subscribe* this topics))
  (unsubscribe [this topics]
    (unsubscribe* this topics))
  (publish [this topic msg]
    (publish* this topic msg)))

(defn mqtt-manager [settings]
  (map->MQTTManager settings))

