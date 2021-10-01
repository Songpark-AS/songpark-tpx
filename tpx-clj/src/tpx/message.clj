(ns tpx.message
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [songpark.common.protocol.message :as protocol.message]
            [tpx.message.incoming :as message.incoming]
            [tpx.message.outgoing :as message.outgoing]))

(defonce ^:private store (atom nil))

(defn handle-message [msg]
  (let [message-service @store
        injections (-> message-service
                       (select-keys (:injection-ks message-service))
                       (assoc :message-service message-service))]   
    (message.incoming/handler (merge msg injections))))

(defn send-message!* [message-service msg]
  (let [message-service @store
        injections (-> message-service
                       (select-keys (:injection-ks message-service))
                       (assoc :message-service message-service))]
    (message.outgoing/handler (merge msg injections))))

(defrecord MessageService [injection-ks started? mqtt-manager]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting MessageService")
          (let [new-this (assoc this
                                :started? true)]
            (reset! store new-this)
            new-this))))
  
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping MessageService")
          (let [new-this (assoc this
                                :started? false)]
            (reset! store nil)
            new-this))))
  
  protocol.message/IMessageService
  (send-message! [this msg]
    (send-message!* this msg)))

(defn message-service [settings]
  (map->MessageService settings))
