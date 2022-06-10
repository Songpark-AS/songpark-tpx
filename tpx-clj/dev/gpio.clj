(ns gpio
  (:require [tpx.gpio :as gpio]
            [tpx.gpio.bitbang :as bitbang]))


(comment

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
 )
