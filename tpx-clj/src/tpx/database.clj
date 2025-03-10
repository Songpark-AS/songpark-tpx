(ns tpx.database
  (:require [codax.core :as codax]
            [com.stuartsierra.component :as component]
            [tpx.config :refer [config]]
            [tpx.utils :refer [get-input-path]]
            [taoensso.timbre :as log]))

(defonce db (atom nil))
(def bit-to-boolean {0 false 1 true nil false})

(comment
  (->> (mapv (fn [k]
              [(get-input-path "input1" k)
               (get-input-path "input2" k)])
            [:gate/switch
             :gate/threshold
             :gate/attack
             :gate/release
             :reverb/switch
             :reverb/mix
             :reverb/damp
             :reverb/room-size
             :amplify/switch
             :amplify/drive
             :amplify/tone
             :equalizer/switch
             :equalizer/low
             :equalizer/medium-low
             :equalizer/medium-high
             :equalizer/high
             :echo/switch
             :echo/delay-time
             :echo/level
             :compressor/switch
             :compressor/threshold
             :compressor/ratio
             :compressor/attack
             :compressor/release])
      (flatten)
      (sort)
      (map (fn [k]
             [k 0]))
      (into (sorted-map))
      (clojure.pprint/pprint)))

(defn get-hardware-values []
  (codax/with-read-transaction [@db tx]
    (let [ks (flatten
              [(mapv (fn [k]
                       [(get-input-path "input1" k)
                        (get-input-path "input2" k)])
                     [:pan
                      :gain
                      :gate/switch
                      :gate/threshold
                      :gate/attack
                      :gate/release
                      :reverb/switch
                      :reverb/mix
                      :reverb/damp
                      :reverb/room-size
                      :amplify/switch
                      :amplify/drive
                      :amplify/tone
                      :equalizer/switch
                      :equalizer/low
                      :equalizer/medium-low
                      :equalizer/medium-high
                      :equalizer/high
                      :echo/switch
                      :echo/delay-time
                      :echo/level
                      :compressor/switch
                      :compressor/threshold
                      :compressor/ratio
                      :compressor/attack
                      :compressor/release])
               [:volume/global-volume
                :volume/network-volume
                :volume/local-volume
                :volume/input1-volume
                :volume/input2-volume
                :jam/playout-delay
                :analog/input
                :analog/gain0
                :analog/gain1
                :analog/gain2
                :analog/gain3
                :analog/relays]])
          {:keys [analog/relays] :as values}
          (reduce (fn [out k]
                    (let [v (codax/get-at tx [k])]
                      (assoc out k (if (some? v)
                                     (if (= k :jam/playout-delay)
                                       (max v 10)
                                       v)
                                     (get-in config [:hardware/default-values k])))))
                  {} ks)
          relay0 (bit-to-boolean (nth relays 7))
          relay1 (bit-to-boolean (nth relays 6))
          relay5 (bit-to-boolean (nth relays 2))
          xlr-jack (if (or (false? relay0)
                           (false? relay1))
                     false
                     true)]
      (-> values
          (dissoc :analog/relays)
          ;; (assoc :analog/relay0 (bit-to-boolean (nth relays 7)))
          ;; (assoc :analog/relay1 (bit-to-boolean (nth relays 6)))
          ;; (assoc :analog/relay2 (bit-to-boolean (nth relays 5)))
          ;; (assoc :analog/relay3 (bit-to-boolean (nth relays 4)))
          ;; (assoc :analog/relay4 (bit-to-boolean (nth relays 3)))
          ;; (assoc :analog/relay5 (bit-to-boolean (nth relays 2)))
          (assoc :analog.relay/r48v relay5)
          (assoc :analog.input/xlr-jack xlr-jack)))))


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
