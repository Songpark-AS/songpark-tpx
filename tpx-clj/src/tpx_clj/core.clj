(ns tpx-clj.core
  (:require
   [clojurewerkz.machine-head.client :as mh]
   [common.platform.connect.client :as connect.client]
   [common.mqtt.connection :as mqtt-connection])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!")
  (init-ipc-with-cs7)
  (retrieve-tpID))

(defn watch-agent-func [_agent context]
  "Boilerplate for making a watch-agent
   Intended for async IPC with CS7, e.g. if pure stdin/out is used for this purpose"
  (let [watch-fn (fn [_context _key _ref old-value new-value])]
  (add-watch _agent nil (partial watch-fn context))))

(defn init-ipc-with-cs7
  "Initializes Inter-Process Communication with CS7.
   Currently not complete; the methods for IPC have not been decided yet."
  (println "Facilitating IPC with CS7...")
  (println "Connection with CS7 established! (just kidding! still need to complete that thing)"))

(defn retrieve-tpID
  "Contains the instructions sent to CS7 to retrieve the tpID."
  (println "Querying CS7 for tpID.")
  (let [tpID "world"]
       (println "Response from CS7 reads:" tpID)))


(defn musician-specific-volume-control
  "Relays musician-specific volume control towards CS7.
   Takes musician's ID, and positive or negative integers.
   "
  [musician adjusted-amount]
  (println "Asking CS7 politely to adjust volume of '"musician"' by: "adjusted-amount)
  (println "CS7 politely agreed. Volume of '"musician"' adjusted by: "adjusted-amount))

(defn unit-volume-control
  "Relays volume control towards CS7
   Takes positive or negative integers"
  [adjusted-amount]
  (println "Asking CS7 politely to adjust the unit's output volume by: " adjusted-amount)
  (println "CS7 politely agreed. Unit's volume adjusted by:" adjusted-amount))



(comment test area
         (connect.client/init {:nickname "heyho!"})
         (let [conn-map (mqtt-connection/init "my-topic")
               handler-map {:volume_control #(println (apply - %&))}]
           
           (mqtt-connection/subscribe conn-map handler-map)
           
           (mqtt-connection/publish conn-map [[:a] [1 2 3]])