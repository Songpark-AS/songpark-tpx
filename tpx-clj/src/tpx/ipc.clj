(ns tpx.ipc
  (:require [clojure.java.shell :refer [sh]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [songpark.common.communication :refer [PUT]]))

(defonce ^:private store (atom nil))

(defn- get-device-mac []
  (-> (clojure.string/split (:out (sh "ip" "n")) #"\n")
      first
      (clojure.string/split #"\s")
      (nth 4)))

(defn send-message! [msg]
  (let [ipc @store
        injections (-> ipc
                       (select-keys (:injection-ks ipc))
                       (assoc :ipc ipc))]
    (.send-message! (:message-service injections) (merge msg injections))))

(defn broadcast-presence [config]
  (PUT (str (:platform config) "/api/teleporter")
       {:teleporter/nickname (get-in config [:teleporter :nickname])
        :teleporter/mac (get-device-mac)}
       (fn [{:teleporter/keys [uuid] :as response}]
         (send-message! {:message/type :teleporter.cmd/subscribe
                         :message/meta {:mqtt/topics {(str uuid) 0}}}))))

(defrecord IpcService [injection-ks started? config message-service mqtt-manager]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting IpcService")          
          (let [new-this (assoc this
                                :started? true)]
            (reset! store new-this)
            (broadcast-presence config)
            new-this))))
  
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping IpcService")
          (let [new-this (assoc this
                                :started? false)]
            (reset! store nil)
            new-this)))))

(defn ipc-service [settings]
  (map->IpcService settings))





(comment
  (pr-str (:teleporter (:config @store)))
  @(broadcast-presence (:config @store))
  
  (get-device-mac)



  )

