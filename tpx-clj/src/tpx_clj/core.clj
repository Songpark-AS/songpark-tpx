(ns tpx-clj.core
  (:require
   [clojurewerkz.machine-head.client :as mh]
   [common.platform.connect.client :as connect.client]
   [common.mqtt.connection :as mqtt-connection])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(comment test area

         (connect.client/init {:nickname "heyho!"})
         (let [conn-map (mqtt-connection/init "my-topic")
               handler-map {:a #(println (apply - %&))}]
           
           (mqtt-connection/subscribe conn-map handler-map)
           
           (mqtt-connection/publish conn-map [[:a][1 2 3]])
           (mqtt-connection/publish conn-map [[:a] [3 3 3]])
           (mqtt-connection/publish conn-map [[:a] [33 33 33]])
           (mqtt-connection/publish conn-map [[:a] [-1 2 3]])
           (mqtt-connection/publish conn-map [[:a] [0 0 0]])
           (mqtt-connection/publish conn-map [[:a] [5 -1 -4]])
           (mqtt-connection/publish conn-map [[:a] [33 -22 5]])))
