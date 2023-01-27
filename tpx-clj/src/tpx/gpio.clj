(ns tpx.gpio
  (:require [com.stuartsierra.component :as component]
            [clojure.core.match :refer [match]]
            [clojure.set :as set]
            [helins.linux.gpio :as gpio]
            [taoensso.timbre :as log]
            [tpx.config :refer [config]]
            [tpx.gpio.bitbang :as bitbang])
  ;; AutoCloseable needs to be imported, otherwise the with-open macro hangs
  (:import [java.lang AutoCloseable]))

(defn bitbang-write
  ([gpio cmd value]
   ;; (bitbang-write gpio cmd nil value)
   )
  ([{:keys [rw-lock rw-attempts] :as gpio} cmd sub-cmd value]
   ;; (let [handle (:handle-write gpio)
   ;;       buffer (:buffer-write gpio)]
   ;;   (when (and handle
   ;;              buffer)
   ;;     (loop [counter 0]
   ;;       (if (>= counter rw-attempts)
   ;;         ;; We have exhausted our attempts. Throw an exception
   ;;         (throw (ex-info "Unable to write to GPIO as the line is busy" {}))
   ;;         (let [locked? @rw-lock]
   ;;           (if locked?
   ;;             ;; if R/W is locked, we sleep for 10 milliseconds
   ;;             (do (Thread/sleep 10)
   ;;                 (recur (inc counter)))
   ;;             ;; we use compare-and-set! in order to make sure we do
   ;;             ;; not get into a situation where two threads try to
   ;;             ;; read or write at the same time
   ;;             (let [success? (compare-and-set! rw-lock locked? true)]
   ;;               (if-not success?
   ;;                 ;; if we could not successfully lock the R/W we sleep
   ;;                 ;; and then iterate the counter for another try
   ;;                 (do (Thread/sleep 10)
   ;;                     (recur (inc counter)))
   ;;                 ;; we could successfully lock R/W. Continue as normal
   ;;                 (try
   ;;                   (let [value (bitbang/bit-write handle buffer cmd sub-cmd value)]
   ;;                     ;; do not forget to reset the R/W lock to false, to allow
   ;;                     ;; for other R/W attempts
   ;;                     (reset! rw-lock false)
   ;;                     value)
   ;;                   (catch Exception e
   ;;                     ;; catch any exceptions in order to reset the R/W lock to false
   ;;                     (reset! rw-lock false)
   ;;                     ;; re-throw the exception for upper layers
   ;;                     (throw e)))))))))))
   ))

(defn bitbang-read
  ([gpio cmd]
   ;; (bitbang-read gpio cmd true)
   )
  ([gpio cmd decimal?]
   ;; (let [{:keys [handle-write handle-read
   ;;               buffer-write buffer-read
   ;;               rw-lock rw-attempts]} gpio]
   ;;   (when (and handle-write
   ;;              handle-read
   ;;              buffer-write
   ;;              buffer-read)
   ;;     (loop [counter 0]
   ;;       (if (>= counter rw-attempts)
   ;;         ;; We have exhausted our attempts. Throw an exception
   ;;         (throw (ex-info "Unable to read from GPIO as the line is busy" {}))
   ;;         (let [locked? @rw-lock]
   ;;           (if locked?
   ;;             ;; if R/W is locked, we sleep for 10 milliseconds
   ;;             (do (Thread/sleep 10)
   ;;                 (recur (inc counter)))
   ;;             ;; we use compare-and-set! in order to make sure we do
   ;;             ;; not get into a situation where two threads try to
   ;;             ;; read or write at the same time
   ;;             (let [success? (compare-and-set! rw-lock locked? true)]
   ;;               (if-not success?
   ;;                 ;; if we could not successfully lock the R/W we sleep
   ;;                 ;; and then iterate the counter for another try
   ;;                 (do (Thread/sleep 10)
   ;;                     (recur (inc counter)))
   ;;                 ;; we could successfully lock R/W. Continue as normal
   ;;                 (try
   ;;                   (let [value (bitbang/bit-reader handle-write
   ;;                                                   handle-read
   ;;                                                   buffer-write
   ;;                                                   buffer-read
   ;;                                                   cmd)]
   ;;                     ;; do not forget to reset the R/W lock to false, to allow
   ;;                     ;; for other R/W attempts
   ;;                     (reset! rw-lock false)
   ;;                     (if decimal?
   ;;                       (bitbang/convert-from-binary value)
   ;;                       value))
   ;;                   (catch Exception e
   ;;                     ;; catch any exceptions in order to reset the R/W lock to false
   ;;                     (reset! rw-lock false)
   ;;                     ;; re-throw the exception for upper layers
   ;;                     (throw e)))))))))))
   ))

(def ^:private on-off-map {:on true :off false})
(defn set-led [{:keys [handle-write buffer-write leds] :as gpio} led on-off-value]
  (assert (#{:on :off} on-off-value) "on-off-value has to be either :on or :off")
  (if-not (contains? @leds led)
    (log/warn (str "led has to be one of " (keys @leds)))
    (do
      (swap! leds assoc led on-off-value)
      (let [leds* (->> @leds
                       (map (fn [[k v]]
                              [k (on-off-map v)]))
                       (into {}))]
        (when handle-write
          (gpio/write handle-write
                      (gpio/set-line+ buffer-write leds*))))))
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

(defn start-blink [{:keys [blinks leds] :as gpio} led ms]
  (when-not (contains? @blinks led)
    (swap! blinks assoc led
           (future
             (while true
               (if (= :on (get @leds led))
                 (set-led gpio led :off)
                 (set-led gpio led :on))
               (Thread/sleep ms))))))

(defn stop-blink [{:keys [blinks] :as gpio} led]
  (when-let [led-f (get @blinks led)]
    (future-cancel led-f)
    (set-led gpio led :off)
    (swap! blinks dissoc led)))

(defn close! [{:keys [running? device handle-write handle-read watchers] :as _gpio}]
  (reset! running? false)
  (when handle-write
    (gpio/close handle-write))
  (when handle-read
    (gpio/close handle-read))
  (when watchers
    (gpio/close watchers))
  (when device
    (gpio/close device)))

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
                    {:keys [falling rising]} (get @bounce tag)]
                ;; (log/debug {:event event
                ;;             :bounce @bounce})
                (match [(number? falling) (number? rising) edge]

                       [false false :falling]
                       (do (log/debug tag :falling/start-of-cycle nano-timestamp)
                           (swap! bounce assoc tag {:falling nano-timestamp
                                                    :rising nil}))

                       [true true :falling]
                       (let [ms (get-milliseconds (- nano-timestamp
                                                     rising) :double)]
                         (log/debug tag :falling/end-of-cycle nano-timestamp)
                         (when (> ms epsilon)
                           (swap! bounce assoc tag {:falling nano-timestamp
                                                    :rising nil})))

                       [true false :rising]
                       (let [ms (get-milliseconds (- nano-timestamp
                                                     falling) :double)]
                         (log/debug tag :rising nano-timestamp ms)
                         (when (> ms epsilon)
                           (swap! bounce assoc-in [tag :rising] nano-timestamp)
                           (if-let [f (get buttons tag)]
                             (f (assoc context
                                       :gpio gpio
                                       :delay ms))
                             (log/error "GPIO: Could not find a function for " tag))))
                       [_ _ _]
                       :do-nothing)))))
        (log/info "Exiting handle-buttons in GPIO"))
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
            leds (:leds component)
            ;; see bitbang for what these mean
            {:keys [chip-select
                    clock
                    mosi
                    miso
                    led-yellow
                    led-green
                    led-red
                    led-link
                    button-link]
             :or
             {;; chip-select 10
              ;; clock 12
              ;; mosi 11
              ;; miso 13
              ;; led-yellow 15
              ;; led-green 14
              led-link 13
              ;; led-red 9
              button-link 10}
             } (:gpio/pins config)
            write-map (merge
                       (if led-link
                         {led-link  {:gpio/state true
                                     :gpio/tag :led/link}})
                       (if led-yellow
                         {led-yellow  {:gpio/state true
                                       :gpio/tag :led/yellow}})
                       (if led-green
                         {led-green   {:gpio/state true
                                       :gpio/tag :led/green}})
                       (if led-red
                         {led-red     {:gpio/state true
                                       :gpio/tag :led/red}})
                       (if chip-select
                         {chip-select {:gpio/state true
                                       :gpio/tag :chip-select}
                          clock       {:gpio/state false
                                       :gpio/tag :clock}
                          mosi        {:gpio/state false
                                       :gpio/tag :mosi}}))
            handle-write (when write-map
                           (gpio/handle device
                                        write-map
                                        {:gpio/direction :output}))
            read-map (if miso
                       {miso {:gpio/state false
                              :gpio/tag :miso}})
            handle-read (when read-map
                          (gpio/handle device
                                       read-map
                                       {:gpio/direction :input}))
            input-map (merge
                       (if button-link
                         {button-link {:gpio/tag :button/link
                                       :gpio/direction :input}}))
            watchers (when input-map
                       (gpio/watcher device
                                     input-map))
            component (assoc component
                             :rw-lock (atom false)
                             :device device
                             :handle-write handle-write
                             :handle-read handle-read
                             :watchers watchers
                             :buffer-write (when handle-write
                                             (gpio/buffer handle-write))
                             :buffer-read (when handle-read
                                            (gpio/buffer handle-read)))]
        (when watchers
          (handle-buttons component))
        (when led-red
          (swap! leds assoc :led/red :off))
        (when led-yellow
          (swap! leds assoc :led/yellow :off))
        (when led-green
          (swap! leds assoc :led/green :off))
        (when led-link
          (swap! leds assoc :led/link :off))
        component)
      (catch Throwable t
        (log/error :init-gpio/error {:throwable t
                                     :message (ex-message t)
                                     :data (ex-data t)})))))


(defrecord GPIO [started? buttons leds context rw-lock rw-attempts
                 device handle-write handle-read watchers]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting GPIO")
          (let [this* (init-gpio this)]
            ;; (set-led this* :led/green :on)
            (assoc this*
                   :started? true)))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping GPIO")
          ;; (set-led this :led/green :off)
          (Thread/sleep 50)
          (close! this)
          (assoc this
                 :started? false)))))

(defn get-gpio [settings]
  (map->GPIO (merge {:buttons {:button/link nil}
                     :leds (atom {})
                     :blinks (atom {})
                     :device nil
                     :handle-write nil
                     :handle-read nil
                     :watchers nil
                     :rw-attempts 5
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
                 :led/red :off})
  tmp
  (get-led tmp :led/red)
  (close! tmp)

  (def _gpio (component/start (get-gpio {:buttons {:button/push1
                                                   (fn [{:keys [gpio delay]}]
                                                     (let [flip-value ({:on :off
                                                                        :off :on}
                                                                       (get-led gpio :led/green))]
                                                       (log/debug "Flipping :led/green to " flip-value)
                                                       (set-led gpio :led/green flip-value))
                                                     (log/debug "Delay was " delay " milliseconds"))}})))
  (def _gpio2 (component/start (get-gpio nil)))
  (def _gpio3 (component/start (get-gpio nil)))
  (set-led _gpio :led/green :on)
  (set-led _gpio :led/green :off)
  (get-led _gpio :led/green)
  (start-blink _gpio :led/green 1000)
  (stop-blink _gpio :led/green)
  (close! _gpio3)
  (component/stop _gpio)

  (get-milliseconds (- 1550151940942776762 1550151940942354770) :double)

  (def device (gpio/device "/dev/gpiochip0"))
  (.close device)
  )
