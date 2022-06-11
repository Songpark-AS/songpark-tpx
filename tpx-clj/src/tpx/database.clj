(ns tpx.database
  (:require [codax.core :as codax]
            [com.stuartsierra.component :as component]
            [tpx.config :refer [config]]
            [taoensso.timbre :as log]))

(defonce db (atom nil))
(def bit-to-boolean {0 false 1 true nil false})

(defn get-hardware-values []
  (codax/with-read-transaction [@db tx]
    (let [{:keys [analog/relays] :as values}
          (reduce (fn [out k]
                    (assoc out k (or (codax/get-at tx [k])
                                     (get-in config [:hardware/default-values k]))))
                  {} [:volume/global-volume
                      :volume/network-volume
                      :volume/local-volume
                      :jam/playout-delay
                      :analog/input
                      :analog/gain0
                      :analog/gain1
                      :analog/gain2
                      :analog/gain3
                      :analog/relays])]
      (-> values
          (dissoc :analog/relays)
          (assoc :analog/relay0 (bit-to-boolean (nth relays 7)))
          (assoc :analog/relay1 (bit-to-boolean (nth relays 6)))
          (assoc :analog/relay2 (bit-to-boolean (nth relays 5)))
          (assoc :analog/relay3 (bit-to-boolean (nth relays 4)))
          (assoc :analog/relay4 (bit-to-boolean (nth relays 3)))
          (assoc :analog/relay5 (bit-to-boolean (nth relays 2)))))))


(defrecord Database [started?]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting Database")
          (reset! db (codax/open-database! (str (get-in config [:os :data-dir]) "songpark.db")))
          (assoc this
                 :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping Database")
          (codax/close-database! @db)
          (reset! db nil)
          (assoc this
                 :started? false)))))


(defn database [settings]
  (map->Database settings))
