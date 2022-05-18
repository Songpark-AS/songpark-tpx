(ns tpx.analog
  "Namespace for handling the analog FPGA"
  (:require [clojure.set :refer [map-invert]]
            [codax.core :as codax]
            [taoensso.timbre :as log]
            [tpx.database :refer [db]]
            [tpx.gpio :as gpio]
            [tpx.gpio.bitbang :refer [convert-from-binary]]))



;; Magnus RPi Python GUI control SW:
;; Address 11H, 19H, 21H, 29H and 34H (GAIN) (Write data)
;; Address 34H (RELAYS) (Write data)
;; Thats what Christian needs to use for testing the analog card at this stage

;; RELAYS - 34H:
;; D0 - REL0 - RELAY K1/K1 - HZCLN - (HIGH-Z_COMBO_LEFT)
;; D1 - REL1 - RELAY K2/K2 - HZCRN - (HIGH-Z_COMBO_RIGHT)
;; D2 - REL2 - RELAY K3/K4 - MUTEHDN - (~MUTE_HEADPHONES)
;; D3 - REL3 - RELAY K4/K5 - MUTEUBLN - (~MUTE_UNBALANCED_LINE_OUT)
;; D4 - REL4 - RELAY K5/K6 - MUTEBLN - (~MUTE_BALANCED_LINE_OUT)
;; D5 - REL5 - RELAY K6/K3 - R48VPWRN - (48V_Ph_PWR)
;; D6 - 0
;; D7 - 0
;; 0 = RELAY OFF
;; 1 = RELAY ON
;; Kx(PCB-Rev0)/Kx(PCB-Rev1)


(def registers {:analog/gain0 0x11 ;; sits on PGA-0
                :analog/gain1 0x19 ;; sits on PGA-1
                :analog/gain2 0x21 ;; sits on PGA-2
                :analog/gain3 0x29 ;; sits on PGA-3
                :analog/relays 0x34 ;; sits on DAC-2
                })

;; Values comes from the HW team
;; The value we read from a Gain register go through this mapping and
;; you would get a corresponding dB value
(def gain-mappings (->> (map vector
                             [0, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
                              22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35,
                              36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
                              50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63]
                             [5.6, 13.6, 14.6, 15.6, 16.6, 17.6, 18.6, 19.6, 20.6, 21.6,
                              22.6, 23.6, 24.6, 25.6, 26.6, 27.6, 28.6, 29.6, 30.6, 31.6,
                              32.6, 33.6, 34.6, 35.6, 36.6, 37.6, 38.6, 39.6, 40.6, 41.6,
                              42.6, 43.6, 44.6, 45.6, 46.6, 47.6, 48.6, 49.6, 50.6, 51.6,
                              52.6, 53.6, 54.6, 55.6, 56.6, 57.6, 58.6, 59.6, 60.6, 61.6,
                              62.6, 63.6, 64.6, 65.6, 66.6, 67.6, 68.6])
                        (into {})))

(def boolean-to-bits {false 0
                      true 1})

(def bits-to-boolean (map-invert boolean-to-bits))

(def bits-to-relays {0 :analog/relay0
                     1 :analog/relay1
                     2 :analog/relay2
                     3 :analog/relay3
                     4 :analog/relay4
                     5 :analog/relay5})

(def relays-to-bits (map-invert bits-to-relays))

(defn write-relay [gpio relay-position value]
  (log/debug ::write-relay relay-position value)
  (let [value (if (boolean? value)
                (boolean-to-bits value)
                value)
        value-bits (or (codax/get-at! @db [:analog/relays])
                       (vec (repeat 8 0)))
        position (if (keyword? relay-position)
                   (relays-to-bits relay-position)
                   relay-position)
        reversed-relay-position (dec (Math/abs (- 8 position)))
        value-to-write (assoc value-bits reversed-relay-position value)
        register (:analog/relays registers)]
    (codax/assoc-at! @db [:analog/relays] value-to-write)
    (gpio/bitbang-write gpio 0x34 (convert-from-binary value-to-write))
    (log/debug "Writing to relay" {:relay-position relay-position
                                   :value value-to-write
                                   :register register})))

(defn write-gain
  "Write the gain"
  [gpio gain value]
  (codax/assoc-at! @db gain value)
  (log/debug "Write gain" {:gain gain
                           :value value})
  (cond (= gain :analog/left-gain)
        (do (gpio/bitbang-write gpio (registers :analog/gain0) value)
            (gpio/bitbang-write gpio (registers :analog/gain1) value))

        (= gain :analog/right-gain)
        (do (gpio/bitbang-write gpio (registers :analog/gain2) value)
            (gpio/bitbang-write gpio (registers :analog/gain3) value))

        :else
        (gpio/bitbang-write gpio (registers gain) value)))

(defn read-gain
  "Read the gain"
  [gpio gain]
  (log/debug "Read gain" {:gain gain})
  (cond (= gain :analog/left-gain)
        (gpio/bitbang-read gpio (registers :analog/gain0))

        (= gain :analog/right-gain)
        (gpio/bitbang-read gpio (registers :analog/gain2))

        :else
        (gpio/bitbang-read gpio (registers gain))))
