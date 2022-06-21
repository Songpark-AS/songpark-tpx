(ns gpio
  (:require [com.stuartsierra.component :as component]
            [tpx.gpio :as gpio]
            [tpx.gpio.bitbang :as bitbang]
            [taoensso.timbre :as log]))

(defn start-gpio []
  (component/start (gpio/get-gpio {:buttons {:button/push1
                                             (fn [_context]
                                               (println "Button 1 was pressed"))}})))

(defn stop-gpio [gpio]
  (component/stop gpio))

(comment

  (def _gpio (start-gpio))
  (gpio/set-led _gpio :led/red :off)
  (gpio/bitbang-read _gpio 0x0a 0x12)
  (gpio/bitbang-write _gpio 0x40 0x12)
  ;; in this block we write to the same register in two different threads
  ;; as well as read from another register
  ;; This should both be doable, and should be blocking, with attempts to try
  ;; again, and again (for up to 5 times), before giving up
  ;; set sleep in tpx.gpio.bitbang to use Thread/sleep instead of the nano-ms
  ;; based sleep that is there in order to try out that commands time out
  (time
   (do
     (future
       (try
         (gpio/bitbang-write _gpio 0x48 (bitbang/convert-from-binary [0 0 0 0 0 0 1 1]))
         (catch Exception e
           (log/error e))))
     (future
       (try
         (gpio/bitbang-write _gpio 0x48 (bitbang/convert-from-binary [0 0 0 0 0 0 0 0]))
         (catch Exception e
           (log/error e))))
     (gpio/bitbang-read _gpio 0xa0)))

  (stop-gpio _gpio)


  (let [_gpio (:gpio @tpx.init/system)]
    (gpio/set-led _gpio :led/prompt :on))

  (let [_gpio (:gpio @tpx.init/system)]
    (gpio/start-blink _gpio :led/prompt 1000))

  (let [_gpio (:gpio @tpx.init/system)]
    (gpio/stop-blink _gpio :led/prompt))


  (let [_gpio (:gpio @tpx.init/system)]
    (gpio/bitbang-write _gpio 0x48 (bitbang/convert-from-binary [0 0 0 0 0 0 1 1])))
  (let [_gpio (:gpio @tpx.init/system)]
    (gpio/set-led _gpio :led/red :on))

  (do
    (let [_gpio (:gpio @tpx.init/system)]
      (gpio/bitbang-write _gpio 0x40 0x12))
    (let [_gpio (:gpio @tpx.init/system)]
      (gpio/bitbang-write _gpio 0x42 0x12)))

  (let [[ca op r5 r4 r3 r2 r1 r0 & data]
        (->> (bitbang/generate-cmd-str :read 0x48 0)
             (map {false 0 true 1}))]
    {:ca ca
     :op op
     :relay [r5 r4 r3 r2 r1 r0]
     :data data})

  (let [_gpio (:gpio @tpx.init/system)]
    (->> (gpio/bitbang-read _gpio 0x0a)
         (bitbang/convert-to-binary)
         #_(drop 6)))
