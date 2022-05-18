(ns tpx.gpio.bitbang
  (:require [helins.linux.gpio :as gpio])
  (:import [java.lang AutoCloseable]))

;; IMPORTANT INFO FOR SOFTWARE DEVELOPERS
;; This is what the hardware acronyms mean
;; See documentation for more info
;; MCSN - Master Chip Select Negative (active is low)
;; MSCLK - Master Slave Clock
;; MOSI - Master Out Slave In
;; MISO - Master In Slave Out


(defn convert-to-binary
  [value]
  (map (comp {\1 1 \0 0} char) (Long/toString value 2)))

(defn convert-from-binary
  [value]
  (Long/parseLong (clojure.string/join value) 2))

(defn- add-or-remove-bits
  "Adds or removes extra bits according to a bit limit"
  [bits limit]
  (let [num (count bits)
        ov-under (- limit (count bits))]
    (cond
      (= limit 8) (cond ;; Data shou
                    (> num limit) (throw (AssertionError. "Data value is out of range"))
                    (< num limit) (concat (repeat ov-under 0) bits)
                    :else bits)
      (= limit 6) (cond
                    (> num limit) (drop (* ov-under -1) bits)
                    (< num limit) (concat (repeat ov-under 0) bits)
                    :else bits)))) ;; Add exception for bits over limits

(defn- get-data-address-bits
  "creates the bits portion, returns 6 bits for address and 8 for data"
  [value mode]
  (let [bits (convert-to-binary value)
        num-bits (count (convert-to-binary value))]
    (if (> mode num-bits)
      (add-or-remove-bits bits mode)
      bits)))

(defn- generate-cmd-str
  "generates the Instruction that will be provided sent to FPGA"
  [op register value]
  (let [rw-bits (if (= :read op)
                  [0 0]
                  [0 1])
        bits (concat rw-bits (get-data-address-bits register 6) (get-data-address-bits value 8))]
    ;; once we have the bits, we need to convert them to true and false,
    ;; as the GPIO library we use operates with booleans, and not 1 and 0
    (map {0 false 1 true} bits)))

(defn- sleep [^Long delay]
  (let [start ^Long (System/nanoTime)]
    (while (> (+ start delay) (System/nanoTime)))))

(defn bit-reader
  "Function that will pass and value to the FPGA and read the result"
  [register handle-write handle-read buffer-write buffer-read]
  (let [high true
        low false
        out (atom [])
        bits {false 0 true 1}]
    (gpio/write handle-write
                (gpio/set-line+ buffer-write {:chip-select low}))
    ;; --
    (doseq [bit (generate-cmd-str :read register 0)]
      (gpio/write handle-write
                  (gpio/set-line+ buffer-write {:mosi bit}))
      (gpio/read handle-read buffer-read)
      (swap! out conj (get bits (gpio/get-line buffer-read :miso)))
      (sleep 100)
      (gpio/write handle-write
                  (gpio/set-line+ buffer-write {:clock high}))
      (sleep 100)
      (gpio/write handle-write
                  (gpio/set-line+ buffer-write {:clock low}))
      (sleep 100))
    ;; --

    (sleep 100)
    (gpio/write handle-write
                (gpio/set-line+ buffer-write {:chip-select high}))
    (drop 8 @out)))

(defn bit-read
  "Function that will return the FPGA value as decimal it uses bit reader"
  [register {:keys [chip-select clock mosi miso]}]
  (convert-from-binary (bit-reader register chip-select clock miso mosi)))

(defn bit-write
  "Function that will pass values over to the FPGA via bit banging"
  [register data-to-write handle buffer]
  (let [high true
        low false]
    (gpio/write handle
                (gpio/set-line+ buffer {:chip-select low}))
    (doseq [bit (generate-cmd-str :write register data-to-write)]
      (gpio/write handle
                  (gpio/set-line+ buffer {:mosi bit}))
      (sleep 100)
      (gpio/write handle
                  (gpio/set-line+ buffer {:clock high}))
      (sleep 100)
      (gpio/write handle
                  (gpio/set-line+ buffer {:clock low}))
      (sleep 100))
    (sleep 100)
    (gpio/write handle
                (gpio/set-line+ buffer {:chip-select high}))))


(comment
  0x34
  (bit-write 0x0 0 {:chip-select 10
                    :clock 12
                    :mosi 11
                    :miso 13})
  (bit-read 0x42 {:chip-select 10
                  :clock 12
                  :mosi 11
                  :miso 13})
  )
