(ns gpio
  (:require [tpx.gpio :as gpio]))


(comment

  (let [_gpio (:gpio @tpx.init/system)]
    (gpio/bitbang-write _gpio 0x4 100))

  (let [_gpio (:gpio @tpx.init/system)]
    (gpio/bitbang-read _gpio 0x4))
 )
