(ns tpx.mqtt
  (:require [clojurewerkz.machine-head.client :as mh]
            [songpark.common.communication]
            [tpx.config :as tpx.config]))

;;! --- Daniel's stuff ---

(defprotocol IMqttClient
  (connect [this uri])
  (connected? [this])
  (subscribe [this topic])
  (publish [this topic message])
  (disconnect [this])
  (disconnect-and-close [this]))


(defrecord Client
           IMqttClient
  (connect [this uri]
  (mh/connect uri {:client-id (get-in tpx.config/config [:client-id])}))
  (subscribe [this topic on-receive-fn])
  (publish [this topic message])
  (disconnect [this])
  (disconnect-and-close [this]))


(def client [tpx.config/config]
  (map->Client {:uri "http://127.0.0.0:1883"
                :topic {"World" 0}}))

;;! --- Sindre's and my stuff (adapted) ---
(defn network-configuration
  "Probably for connecting to a friend's jam and such."
  ; #TODO
  [])

(defn common-connect-init ; Needed a temporary in-project substitute for (Sindre-)common's own
  "This function initiates a simple mqtt connection using machine head.
   Expects a topic name.
   Uses default URI: \"tcp://127.0.0.1:1883\", unless other URI is specified
   Example uses:
   
   (init \"MY-TOPIC\")
   (init \"MY-TOPIC\" \"URI\")

   Returns a connection-map on format:
   
   {:connection CONNECTION
    :topic TOPIC}"

  ([topic uri]
   (let [connection (mh/connect uri {:on-connect-complete (fn [& args] (println "Connection completed." args))
                                     :on-connection-lost (fn [& args] (println "Connection lost." args))
                                     :on-delivery-complete (fn [& args] (println "Delivery completed." args))
                                     :on-unhandled-message (fn [& args] (println "Unhandled message encountered." args))
                                     :opts {:auto-reconnect true
                                            :keep-alive-interval 60}})]
     {:connection connection
      :topic topic}))
  ([topic]
   (common-connect-init topic "tcp://127.0.0.1:1883")))

(defn common-subscribe
  "Subscribes to a topic on an mqtt server, and waits for instructions.
   Expects a mh-connection, a topic-name, and a handler-map.
   Example use:
   
   (subscribe CONNECTION TOPIC HANDLER-MAP)
   (subscribe CONNECTION-MAP HANDLER-MAP)"
  ([connection topic handler-map]
   (mh/subscribe connection {topic 0} (fn [^String topic _ ^bytes payload]
                                        (try
                                          (let [payload (String. payload "UTF-8")
                                                _ (prn "I am payload, destroyer of MQTTs" payload)
                                                payload-map (clojure.core/read-string payload)
                                                _ (prn :mapkeys (keys payload-map))
                                                {pointer :pointer
                                                 arguments :arguments} payload-map]
                                            (prn
                                             "pointerr:" pointer
                                             "arrrrrrg:" arguments)
                                            (apply (get-in handler-map pointer) arguments))
                                          (catch Exception e (prn (str "Caught error in handler mapping: " (.getMessage e))))))))
  ([{connection :connection
     topic :topic}
    handler-map]
   (common-subscribe connection topic handler-map)))