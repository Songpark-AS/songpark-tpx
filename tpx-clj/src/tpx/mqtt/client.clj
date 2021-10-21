(ns tpx.mqtt.client
  (:require [clojurewerkz.machine-head.client :as mh]
            [taoensso.timbre :as log]
            [songpark.common.protocol.mqtt.client :as protocol.mqtt.client]))


(defonce ^:private store (atom nil))

(def ^:private default-options
  {:on-connect-complete
   (fn [& args]
     (log/debug ::on-connect-complete "Connection completed.")
     (let [client @store
           topics (:topics client)
           on-message (:on-message (:config client))]
       (log/debug ::on-connect-complete [topics on-message])
       (when-not (empty? @topics)
         (do (.subscribe client @topics on-message)
             (log/debug ::on-connect-complete "Resubscribed to topics: " @topics)))))
   ;; TODO: Tell Platform about MQTT connection loss over HTTP
   :on-connection-lost (fn [& args] (println "Connection lost." args))
   :on-delivery-complete (fn [& args] (println "Delivery completed." args))
   :on-unhandled-message (fn [& args] (println "Unhandled message encountered." args))})

(defn- gen-uri-string [{:keys [scheme host port]}]
  (str scheme "://" host ":" port))

(defn- gen-paho-options [{:keys [client-id options connect-options]}]
  (-> {:client-id (or client-id (mh/generate-id))}
      (merge (or options default-options))
      (merge {:opts connect-options})))

(defrecord MqttClient [config client]
  protocol.mqtt.client/IMqttClient
  (connect [this]
    ;; only used for when/if client was disconnected after initial creation
    (log/info "Connecting to broker")
    (.connect (:client this)))

  (connected? [this]
    (mh/connected? (:client this)))

  (publish [this topic message]
    (mh/publish (:client this) topic message))

  (disconnect [this]
    (log/info "Disconnecting from broker")
    (mh/disconnect (:client this)))

  ;; on-message is placed in config by mqtt manager,
  ;; but protocol signature has on-message as param
  ;; TODO: remove on-message from protocol, and attach
  ;; a generic on-message handler to Paho client instance
  ;; via manager
  (subscribe [this topics on-message]
    (when (.connected? this)
      (do (swap! (:topics this) merge topics)
          (mh/subscribe (:client this) topics (:on-message config)))))

  (unsubscribe [this topics]
    (when (.connected? this)
      (doseq [k topics]
        (swap! (:topics this) dissoc k))
      (mh/unsubscribe (:client this) topics))))

(defn create [config]
  (log/info "Connecting to broker")
  (let [c (map->MqttClient {:config config
                            :topics (atom {})
                            :client (mh/connect (gen-uri-string config)
                                                (gen-paho-options config))})]
    (reset! store c)
    c))
