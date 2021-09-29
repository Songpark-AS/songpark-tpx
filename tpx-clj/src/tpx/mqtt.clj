(ns tpx.mqtt
  (:require [clojurewerkz.machine-head.client :as mh]
            ;; [songpark.common.communication]
            [tpx.config :as tpx.config]
            ;; [tpx.init :as tpx.init]
            [cognitect.transit :as transit]
            #_[tpx.http :refer [on-and-unavailable available]]))

;;! --- Daniel's stuff ---

(defprotocol IMqttClient
  (connect [this uri])
  (connected? [this])
  (subscribe [this topic])
  (publish [this topic message])
  (disconnect [this])
  (disconnect-and-close [this]))


#_(defrecord Client [config]
  IMqttClient
  #_(connect [this uri]
    (mh/connect uri {:client-id (get-in tpx.config/config [:client-id])}))
  
  #_(is-connected? [this])
  
  (subscribe ; [this topic]
    [this uri topic]
    (mh/subscribe uri {topic 2} ; QoS level 2? Or is 1 enough?
      (fn [^String topic 
           ^bytes payload]
        (try
          (let [->transit {:status 0 
                           :still-learning-CLJ and-transit,
                           :their-docs-weren't-clear-to-me and-trying-to-google-CLJ-mqtt-and-transit-didn't-lead-me-anywhere-better,
                           :but-finish-this subscription-message/payload-parser}]
            ((:the-parsed-hash-map-key-from-payload tpx.init/handler-map) args-from-payload)) ;; this is the protagonist of the subscription handler-function! :)
          (catch Exception e
            (prn (str "Caught error in handler-function/handle-mapping: \n" (.getMessage e)))
            (prn (str "topic:       " topic       ))
            (prn (str "msg payload: " payload))
           )))))
  
  #_(subscribe ; And old, dual-argument-type version we used to use
    "Subscribes to a topic on an mqtt server, and waits for instructions.
     Expects a mh-connection, a topic-name, and a handler-map.
     Example use:
   
     (subscribe CONNECTION TOPIC HANDLER-MAP)
     (subscribe CONNECTION-MAP HANDLER-MAP)"
    ([connection topic handler-map]
      (mh/subscribe connection {topic 0}
        (fn [^String topic _ ^bytes payload]
          (try
            (let
              [payload     (String. payload "UTF-8")
               _           (prn "I am payload, destroyer of MQTTs" payload)
               payload-map (clojure.core/read-string payload)
               _           (prn :mapkeys (keys payload-map))
               {pointer   :pointer
                arguments :arguments} payload-map]
              (prn
                "pointerr:" pointer
                "arrrrrrg:" arguments)
              (apply (get-in handler-map pointer) arguments))
            (catch Exception e
              (prn (str "Caught error in handler mapping: " (.getMessage e))))))))
    ([{connection :connection topic :topic} handler-map]
      (subscribe connection topic handler-map)))
  
  (publish [this topic message])
  
  (disconnect [this])
  
  (disconnect-and-close [this]))


#_(def client
  (map->Client {:uri "http://192.168.11.193:1883" ; Mathias
                :topic {"World" 0}})
  #_(map->Client {:uri "http://127.0.0.0:1883" ; docker
                :topic {"World" 0}})
)

(comment
  #_@(on-and-unavailable "0000")
  )


;;! --- Sindre's and my stuff (adapted) ---
(defn network-configuration
  "Probably for connecting to a friend's jam and such."
  ; #TODO
  [])

#_(defn common-connect-init ; Needed a temporary in-project substitute for (Sindre-)common's own
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
   (let 
    [connection 
     (mh/connect 
      uri {:on-connect-complete (fn [& args] (println "Connection completed." args))
           :on-connection-lost (fn [& args] (println "Connection lost." args))
           :on-delivery-complete (fn [& args] (println "Delivery completed." args))
           :on-unhandled-message (fn [& args] (println "Unhandled message encountered." args))
           :opts {
                  :auto-reconnect true
                  :keep-alive-interval 60
                  ;; :username "tpx"            ; Connection details towards Mathias
                  ;; :password "SecretPass"
                  }})]
     {:connection connection
      :topic topic}))
  ([topic]
  ;; (common-connect-init topic "tcp://192.168.11.123:1883"))) ; IP of Mathias
   (common-connect-init topic "tcp://127.0.0.1:1883")))       ; IP of local docker

#_(defn common-subscribe
  "Subscribes to a topic on an mqtt server, and waits for instructions.
   Expects a mh-connection, a topic-name, and a handler-map.
   Example use:
   
   (subscribe CONNECTION TOPIC HANDLER-MAP)
   (subscribe CONNECTION-MAP HANDLER-MAP)"
  ([connection topic handler-map]
   (mh/subscribe connection {topic 0} 
      (fn [^String topic _ ^bytes payload]
        (try
          (let 
           [payload (String. payload "UTF-8")
            _ (prn "I am payload, destroyer of MQTTs" payload)
            payload-map (clojure.core/read-string payload)
            _ (prn :mapkeys (keys payload-map))
            {pointer :pointer
              arguments :arguments} payload-map]
            (prn
              "pointerr:" pointer
              "arrrrrrg:" arguments)
            (apply (get-in handler-map pointer) arguments))
          (catch Exception e 
            (prn (str 
                  "Caught error in handler mapping: " (.getMessage e))))))))
  ([{connection :connection
     topic :topic}
    handler-map]
   (common-subscribe connection topic handler-map)))