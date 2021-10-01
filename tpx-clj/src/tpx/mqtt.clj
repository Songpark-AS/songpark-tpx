(ns tpx.mqtt
  (:require [com.stuartsierra.component :as component]
            [cognitect.transit :as transit]
            [taoensso.timbre :as log]
            [songpark.common.communication :refer [write-handlers]]
            [songpark.common.protocol.mqtt.manager :as protocol.mqtt.manager]
            [tpx.mqtt.client :as mqtt.client]
            [tpx.message :refer [handle-message]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))


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
  (->> (merge (<-transit payload) {:message/topic topic})
       handle-message))

(defn- subscribe* [{:keys [client] :as mqtt-manager} topics]
  (.subscribe client topics on-message))

(defn- unsubscribe* [{:keys [client] :as mqtt-manager} topics]
  (.unsubscribe client topics))

(defn- publish* [{:keys [client] :as mqtt-manager} topic msg]
  (.publish client topic (->transit msg)))

(defrecord MQTTManager [injection-ks started? config]
  component/Lifecycle
  (start [this]
    (if started?
      this      
      (do
        (log/info "Starting MQTTManager")          
        (assoc this
               :started? true
               :client (mqtt.client/create (assoc config :on-message on-message))))))
  
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping MQTTManager")
          (when (.connected? (:client this))            
            (.disconnect (:client this)))
          (assoc this :started? false))))

  protocol.mqtt.manager/IMqttManager
  (subscribe [this topics]
    (subscribe* this topics))
  (unsubscribe [this topics]
    (unsubscribe* this topics))
  (publish [this topic msg]
    (publish* this topic msg)))

(defn mqtt-manager [settings]
  (map->MQTTManager settings))

