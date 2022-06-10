(ns tpx.scheduler
  (:require [chime.core :as chime]
            [com.stuartsierra.component :as component]
            [songpark.mqtt :as mqtt]
            [songpark.mqtt.util :refer [broadcast-topic]]
            [taoensso.timbre :as log]
            [tpx.data :as data]
            [tpx.gpio :as gpio]
            [tpx.gpio.bitbang :as bitbang])
  (:import [java.time Instant Duration]))

(defn start-overload-checks [callback ms]
  (chime/chime-at
   (chime/periodic-seq (Instant/now)
                       (Duration/ofMillis ms))
   (fn [time]
     (callback time))
   {:error-handler (fn [e]
                     (log/error ::start-overload-checks
                                {:exception e
                                 :message (ex-message e)
                                 :data (ex-data e)})
                     false)}))

(defn- get-overload-callback [{:keys [gpio mqtt-client] :as _context}]
  (let [overflow (atom {:overflow1+2 0
                        :overflow3+4 0})]
    (fn [time]
      ;; this looks really convoluted, but the idea is to make
      ;; changes only when the overflow bits changes, and only then
      ;; this is to make sure that if the overflow is on for a long time
      ;; (like someone screaming), the network doesn't send the same data
      ;; all the time, but sends it only once, and then sends a false on the
      ;; overload when the person stops screaming
      (let [{overflow1+2-old :overflow1+2
             overflow3+4-old :overflow3+4} @overflow
            [overflow1+2 overflow3+4] (->> (gpio/bitbang-read gpio 0x0a)
                                           (bitbang/convert-to-binary)
                                           (drop 6))
            status1+2 (cond (> overflow1+2 overflow1+2-old) :up
                             (< overflow1+2 overflow1+2-old) :down
                             :else :same)
            status3+4 (cond (> overflow3+4 overflow3+4-old) :up
                             (< overflow3+4 overflow3+4-old) :down
                             :else :same)
            tp-id (data/get-tp-id)
            topic (broadcast-topic tp-id)]
        (reset! overflow {:overflow1+2 overflow1+2
                          :overflow3+4 overflow3+4})
        ;; (log/debug {:status3+4 status3+4
        ;;             :status1+2 status1+2
        ;;             :overflow @overflow})
        (when (or (= :up status1+2)
                  (= :up status3+4))
          (gpio/set-led gpio :led/red :on))
        (when (or (= :down status1+2)
                  (= :down status3+4))
          (gpio/set-led gpio :led/red :off))
        (when (= :up status1+2)
          (mqtt/publish mqtt-client topic {:message/type :teleporter/overload
                                           :teleporter/id tp-id
                                           :teleporter/overload :analog/overload1+2?
                                           :teleporter/value true}))
        (when (= :down status1+2)
          (mqtt/publish mqtt-client topic {:message/type :teleporter/overload
                                           :teleporter/id tp-id
                                           :teleporter/overload :analog/overload1+2?
                                           :teleporter/value false}))
        (when (= :up status3+4)
          (mqtt/publish mqtt-client topic {:message/type :teleporter/overload
                                           :teleporter/id tp-id
                                           :teleporter/overload :analog/overload3+4?
                                           :teleporter/value true}))
        (when (= :down status3+4)
          (mqtt/publish mqtt-client topic {:message/type :teleporter/overload
                                           :teleporter/id tp-id
                                           :teleporter/overload :analog/overload3+4?
                                           :teleporter/value false}))))))


(defrecord Scheduler [started? job mqtt-client gpio]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (let [callback (get-overload-callback {:gpio gpio
                                             :mqtt-client mqtt-client})]
        (log/info "Starting Scheduler")
        (assoc this
               :job (start-overload-checks callback 100)
               :started? true))))
  (stop [this]
    (if-not started?
      this
      (do
        (log/info "Stopping Scheduler")
        (.close job)
        (assoc this
               :job nil
               :started? false)))))

(defn get-scheduler [settings]
  (map->Scheduler settings))
