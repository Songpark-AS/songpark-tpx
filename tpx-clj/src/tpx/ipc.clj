(ns tpx.ipc
  (:require [clojure.java.shell :refer [sh]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [songpark.common.communication :refer [PUT]]))

(defonce ^:private store (atom nil))

(defn get-device-mac []
  (-> (clojure.string/split (:out (sh "ip" "n")) #"\n")
      first
      (clojure.string/split #"\s")
      (nth 4)))

(defn send-message! [msg]
  (let [ipc @store
        injections (-> ipc
                       (select-keys (:injection-ks ipc))
                       (assoc :ipc ipc))]
    (.send-message! (:message-service injections) msg)))

(defrecord IpcService [injection-ks started? mqtt]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting IpcService")
          (let [new-this (assoc this
                                :started? true)]
            (reset! store new-this)
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


  )

