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

;; RELAYS - 0x48:
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

;; CMD(W): CS-PG0 - 0x40
;; CMD(W): CS-PG1 - 0x42
;; CMD(W): CS-PG2 - 0x44
;; CMD(W): CS-PG3 - 0x46
;; CMD(W): CS-RELAY - 0x48
;; CMD(R): CS-OVF - 0x0A


(def registers {:analog/gain0 0x44 ;; sits on PGA-2
                :analog/gain1 0x46 ;; sits on PGA-3
                :analog/gain2 0x40 ;; sits on PGA-0
                :analog/gain3 0x42 ;; sits on PGA-1
                :analog/relays 0x48 ;; sits on DAC-2
                :analog/input 0x4C
                :analog/overflow 0x0A
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

(def bits-to-relays {0 :analog/relay0 ;; RELAY K1 - HZCLN = GND - ON - (HIGH-Z_COMBO_LEFT) P51
                     1 :analog/relay1 ;; RELAY K2 - HZCRN = GND - ON - (HIGH-Z_COMBO_RIGHT) P55
                     2 :analog/relay2 ;; RELAY K3 - MUTEHDN = GND - ON - (~MUTE_HEADPHONES) P74
                     3 :analog/relay3 ;; RELAY K4 - MUTEUBLN = GND - ON - (~MUTE_UNBALANCED_LINE_OUT) P66
                     4 :analog/relay4 ;; RELAY K5 - MUTEBLN = GND - ON - (~MUTE_BALANCED_LINE_OUT) P61
                     5 :analog/relay5 ;; RELAY K6 - R48VPWRN = VCC - OFF - P1 (48V_Ph_PWR) P1
                     })

(def relays-to-bits (assoc (map-invert bits-to-relays)
                           :analog.relay/r48v 5))

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
    (gpio/bitbang-write gpio
                        (:analog/relays registers)
                        (convert-from-binary value-to-write))
    (codax/assoc-at! @db [:analog/relays] value-to-write)
    (log/debug "Writing to relay" {:relay-position relay-position
                                   :value value-to-write
                                   :register (format "0x%X" register)})))

(defn- get-relay-position [relay]
  (dec (Math/abs (- 8 relay))))

(defn- get-relay-value [new old]
  (let [value (if (some? new)
                new
                old)]
    (if (boolean? value)
      (boolean-to-bits value)
      value)))

(defn write-relays [gpio data]
  (log/debug ::write-relays data)
  (let [{new-value0 :analog/relay0
         new-value1 :analog/relay1
         new-value2 :analog/relay2
         new-value3 :analog/relay3
         new-value4 :analog/relay4
         new-value5 :analog/relay5} data
        [_
         _
         old-value5
         old-value4
         old-value3
         old-value2
         old-value1
         old-value0
         :as value-bits] (or (codax/get-at! @db [:analog/relays])
                         (vec (repeat 8 0)))
        ;; position (if (keyword? relay-position)
        ;;            (relays-to-bits relay-position)
        ;;            relay-position)
        ;; reversed-relay-position (dec (Math/abs (- 8 position)))
        value-to-write (-> value-bits
                           (assoc (get-relay-position 5) (get-relay-value new-value5 old-value5))
                           (assoc (get-relay-position 4) (get-relay-value new-value4 old-value4))
                           (assoc (get-relay-position 3) (get-relay-value new-value3 old-value3))
                           (assoc (get-relay-position 2) (get-relay-value new-value2 old-value2))
                           (assoc (get-relay-position 1) (get-relay-value new-value1 old-value1))
                           (assoc (get-relay-position 0) (get-relay-value new-value0 old-value0)))
        register (:analog/relays registers)]
    (gpio/bitbang-write gpio
                        (:analog/relays registers)
                        (convert-from-binary value-to-write))
    (codax/assoc-at! @db [:analog/relays] value-to-write)
    (log/debug "Writing to relay" {:data data
                                   :value-to-write value-to-write
                                   :register (format "0x%X" register)})))

;; the actual range from the HW folks are 8 to 63
;; with 0 being a special value
;; if it's zero we pass along as is, where as anything that is non-zero
;; we add 8 to it
;; this has the peculiar effect that the front end has a range of 0 to 56,
;; and on the TPX the range is 0, 8-63
(def ^:private gain-jump 8)

(defn write-gain
  "Write the gain"
  [gpio gain value]
  (let [value (if (zero? value)
                value
                (+ value gain-jump))]
    (log/debug "Write gain" {:gain gain
                             :value value})
    (cond (= gain :analog/gain0)
          (gpio/bitbang-write gpio (registers :analog/gain0) 0x12 value)

          (= gain :analog/gain1)
          (gpio/bitbang-write gpio (registers :analog/gain1) 0x12 value)

          (= gain :analog/gain2)
          (gpio/bitbang-write gpio (registers :analog/gain2) 0x12 value)

          (= gain :analog/gain3)
          (gpio/bitbang-write gpio (registers :analog/gain3) 0x12 value)

          :else
          (throw (ex-info "Unable to adjust the gain as it is not supported"
                          {:gain gain
                           :value value})))
    (codax/assoc-at! @db gain value)))

(defn read-gain
  "Read the gain"
  [gpio gain]
  (log/debug "Read gain" {:gain gain})
  (let [value (cond (= gain :analog/left-gain)
                    (gpio/bitbang-read gpio (registers :analog/gain0))

                    (= gain :analog/right-gain)
                    (gpio/bitbang-read gpio (registers :analog/gain2))

                    :else
                    (gpio/bitbang-read gpio (registers gain)))]
    (if (zero? value)
      value
      (+ value gain-jump))))


;; CMD(W) DATA - I2S SWITCH - CMD(W) = 0x4C
;; 0x4C 0x00 - 0000 0000 - 0 = LineIn(Con1&2)
;; 0x4C 0x01 - 0000 0001 - 1 = Combo(3&4)

(defn switch-input [gpio switch which-input]
  (assert (boolean? which-input) "Which-input must be true or false")
  (when (= switch :analog/input)
    (if (false? which-input)
      ;; set LineIn (Connector 1 & 2)
      (gpio/bitbang-write gpio (:analog/input registers) 0x00)
      ;; set Combo (Connector 3&4)
      (gpio/bitbang-write gpio (:analog/input registers) 0x01))
    (codax/assoc-at! @db [:analog/input] which-input))
  (when (= switch :analog.input/xlr-jack)
    (if (false? which-input)
      (write-relays gpio {:analog/relay0 false
                          :analog/relay1 false})
      (write-relays gpio {:analog/relay0 true
                          :analog/relay1 true}))))
