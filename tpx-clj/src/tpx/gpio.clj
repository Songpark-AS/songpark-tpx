(ns tpx.gpio
  (:require [com.stuartsierra.component :as component]
            [clojure.set :as set]
            [helins.linux.gpio :as gpio]
            [taoensso.timbre :as log])
  ;; AutoCloseable needs to be imported, otherwise the with-open macro hangs
  (:import [java.lang AutoCloseable]))


(def ^:private on-off-map {:on false :off true})
(defn set-led [{:keys [handle-write buffer-write leds] :as gpio} led on-off-value]
  (assert (#{:on :off} on-off-value) "on-off-value has to be either :on or :off")
  (assert (contains? @leds led) (str "led has to be one of " (keys @leds)))
  (swap! leds assoc led on-off-value)
  (let [leds* (->> @leds
                   (map (fn [[k v]]
                          [k (on-off-map v)]))
                   (into {}))]
    (gpio/write handle-write
                (gpio/set-line+ buffer-write leds*)))
  gpio)

(defn get-led [{:keys [leds]} led]
  (get @leds led))

(defn set-leds [gpio leds]
  (assert (map? leds)
          "leds has to be a map with led keys and corrsesponding values of :on or :off")
  (assert (every? #{:on :off} (vals leds))
          "leds have to contain either :on or :off")
  (assert (set/subset? (set (keys leds))
                       (set (keys @(:leds gpio))))
          (str "leds can only contain the keys " (keys @(:leds gpio))))
  (swap! (:leds gpio) merge leds))

(defn close! [{:keys [running? device handle-write handle-read watchers] :as _gpio}]
  (reset! running? false)
  (gpio/close handle-write)
  (gpio/close handle-read)
  (gpio/close watchers)
  (gpio/close device))

(defn- get-milliseconds
  ([nano-timestamp]
   (long (/ nano-timestamp 1000000)))
  ([nano-timestamp cast-to]
   (case cast-to
     :double (double (/ nano-timestamp 1000000))
     (long (/ nano-timestamp 1000000)))))

(defn- get-sleep-time [boundary end-time start-time]
  (max 1 (- boundary (get-milliseconds (- end-time start-time)))))

(defn- handle-buttons [{:keys [buttons watchers running? context] :as gpio}]
  (future
    (try
      (let [bounce (atom {})
            ;; epsilon of 10 ms for bouncing signals
            epsilon 10.0]
        (while @running?
          ;; (log/debug :handle-buttons)
          (let [event (gpio/event watchers 1000)]
            (when event
              (let [{:gpio/keys [tag nano-timestamp edge]} event
                    rising (get-in @bounce [tag :rising])]
                ;; (log/debug {:event event
                ;;             :bounce @bounce})
                (cond (and (nil? rising)
                           (= edge :rising))
                      (swap! bounce assoc-in [tag :rising] nano-timestamp)

                      (and (some? rising)
                           (= edge :falling))
                      (let [ms (get-milliseconds (- nano-timestamp rising) :double)]
                        (when (> ms epsilon)
                          (if-let [f (get buttons tag)]
                            (do (f (assoc context
                                          :gpio gpio
                                          :delay ms))
                                (swap! bounce dissoc tag))
                            (do (log/error "GPIO: Could not find a function for " tag)
                                (swap! bounce dissoc tag)))))

                      :else
                      :do-nothing))))))
      (catch Throwable t
        (log/error :handle-buttons/error t)))))

(defn- init-gpio [component]
  (let [;; for the LEDs false (ie, 0), is the LED turned on
        ;; and true (ie, 1), is the LED turned off
        ;; this is decided by the HW guys, and had to do with how much
        ;; power was drawn from the power card
        on-off-map {:on false :off true}]
    (try
      (let [device (gpio/device "/dev/gpiochip0")
            ;; see bitbang for what these mean
            chip-select 10
            clock 12
            mosi 11
            miso 13
            handle-write (gpio/handle device
                                      {15          {:gpio/state true
                                                    :gpio/tag :led/yellow}
                                       14          {:gpio/state true
                                                    :gpio/tag :led/green}
                                       9           {:gpio/state true
                                                    :gpio/tag :led/red}
                                       chip-select {:gpio/state true
                                                    :gpio/tag :chip-select}
                                       clock       {:gpio/state false
                                                    :gpio/tag :clock}
                                       mosi        {:gpio/state false
                                                    :gpio/tag :mosi}}
                                      {:gpio/direction :output})
            handle-read (gpio/handle device
                                     {miso {:gpio/state false
                                            :gpio/tag :miso}}
                                     {:gpio/direction :input})
            watchers (gpio/watcher device
                                   {0 {:gpio/tag :button/push1
                                       :gpio/direction :input}})
            component (assoc component
                             :device device
                             :handle-write handle-write
                             :handle-read handle-read
                             :watchers watchers
                             :buffer-write (gpio/buffer handle-write)
                             :buffer-read (gpio/buffer handle-read))]
        (handle-buttons component)
        component)
      (catch Throwable t
        (log/error :init-gpio/error t)))))


(defrecord GPIO [started? f buttons leds
                 device handle-write handle-read watchers context]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting GPIO")
          (let [this* (init-gpio this)]
            (set-led this* :led/green :on)
            (assoc this*
                   :started? true)))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping GPIO")
          (set-led this :led/green :off)
          (Thread/sleep 50)
          (close! this)
          (assoc this
                 :started? false)))))

(defn get-gpio [settings]
  (map->GPIO (merge {:buttons {:button/push1 nil}
                     :leds (atom {:led/red :off
                                  :led/yellow :off
                                  :led/green :off})
                     :device nil
                     :handle-write nil
                     :handle-read nil
                     :watchers nil
                     :running? (atom true)}
                    settings)))

(comment

  (let [gpio (-> @tpx.init/system
                 :gpio-manager
                 :gpio)]
    (get-led gpio :led/red)
    #_(set-leds gpio {:led/red :off
                      :led/yellow :off})
    #_(set-led gpio :led/red :off)
    gpio)

  (def tmp (get-gpio {:buttons (atom {:button/push1
                                      (fn [gpio delay]
                                        (let [flip-value ({:on :off
                                                           :off :on}
                                                          (get-led gpio :led/red))]
                                          (log/debug "Flipping :led/red to " flip-value)
                                          (set-led gpio :led/red flip-value))
                                        (log/debug "Delay was " delay " milliseconds"))})}))

  (set-leds tmp {:led/yellow :off
                 :led/green :off
                 :led/red :off
                 })
  tmp
  (get-led tmp :led/red)
  (close! tmp)

  (def _gpio (component/start (get-gpio {:buttons {:button/push1
                                                   (fn [{:keys [gpio delay]}]
                                                     (let [flip-value ({:on :off
                                                                        :off :on}
                                                                       (get-led gpio :led/red))]
                                                       (log/debug "Flipping :led/red to " flip-value)
                                                       (set-led gpio :led/red flip-value))
                                                     (log/debug "Delay was " delay " milliseconds"))}})))
  (def _gpio2 (component/start (get-gpio nil)))
  (def _gpio3 (component/start (get-gpio nil)))
  (set-led _gpio :led/green :on)
  (get-led _gpio :led/green)
  (close! _gpio3)
  (component/stop _gpio)

  (get-milliseconds (- 1550151940942776762 1550151940942354770) :double)

  (def device (gpio/device "/dev/gpiochip0"))
  (.close device)
  )
