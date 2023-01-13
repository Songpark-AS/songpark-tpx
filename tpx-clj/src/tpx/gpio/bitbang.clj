(ns tpx.gpio.bitbang
  ;; (:require [helins.linux.gpio :as gpio]
  ;;           [taoensso.timbre :as log])
  ;; (:import [java.lang AutoCloseable])
  )

;; IMPORTANT INFO FOR SOFTWARE DEVELOPERS
;; This is what the hardware acronyms mean
;; See documentation for more info
;; MCSN - Master Chip Select Negative (active is low)
;; MSCLK - Master Slave Clock
;; MOSI - Master Out Slave In
;; MISO - Master In Slave Out


(defn flag-on? [bit]
  (not (zero? bit)))

(defn flag-off? [bit]
  (zero? bit))

(defn convert-to-binary
  [value]
  (map (comp {\1 1 \0 0} char) (Long/toString value 2)))

(defn convert-from-binary
  [value]
  (Long/parseLong (clojure.string/join value) 2))

(defn- add-or-remove-bits
  "Adds or removes extra bits according to a bit limit"
  [bits limit]
  ;; (let [num (count bits)
  ;;       ov-under (- limit (count bits))]
  ;;   (cond
  ;;     (= limit 8) (cond ;; Data shou
  ;;                   (> num limit) (throw (AssertionError. "Data value is out of range"))
  ;;                   (< num limit) (concat (repeat ov-under 0) bits)
  ;;                   :else bits)
  ;;     (= limit 6) (cond
  ;;                   (> num limit) (drop (* ov-under -1) bits)
  ;;                   (< num limit) (concat (repeat ov-under 0) bits)
  ;;                   :else bits)))
  ) ;; Add exception for bits over limits

(defn- get-data-address-bits
  "creates the bits portion, returns 6 bits for address and 8 for data"
  [value mode]
  ;; (let [bits (convert-to-binary value)
  ;;       num-bits (count (convert-to-binary value))]
  ;;   (if (> mode num-bits)
  ;;     (add-or-remove-bits bits mode)
  ;;     bits))
  )

(defn- generate-cmd-str
  "generates the Instruction that will be provided sent to FPGA"
  [op cmd ?sub-cmd-value & [?value]]
  ;; (let [[_ op-bit :as cmd-bits] (get-data-address-bits cmd 8)
  ;;       sub-cmd (if ?value
  ;;                 ?sub-cmd-value
  ;;                 nil)
  ;;       value (if-not ?value
  ;;               ?sub-cmd-value
  ;;               ?value)
  ;;       sub-cmd-bits (if sub-cmd
  ;;                      (get-data-address-bits sub-cmd 8)
  ;;                      nil)
  ;;       value-bits (get-data-address-bits value 8)
  ;;       bits (concat cmd-bits
  ;;                    sub-cmd-bits
  ;;                    value-bits)]
  ;;   (cond
  ;;     (and (= op :read) (= op-bit 1))
  ;;     (throw (ex-info "Operation should be read, but the R/W bit is set to 1"
  ;;                     {:op op
  ;;                      :cmd cmd
  ;;                      :cmd-bits cmd-bits
  ;;                      :value value
  ;;                      :value-bits value-bits
  ;;                      :bits bits}))
  ;;     (and (= op :write) (= op-bit 0))
  ;;     (throw (ex-info "Operation should be write, but the R/W bit is set to 0"
  ;;                     {:op op
  ;;                      :cmd cmd
  ;;                      :cmd-bits cmd-bits
  ;;                      :value value
  ;;                      :value-bits value-bits
  ;;                      :bits bits}))
  ;;     :else
  ;;     ;; once we have the bits, we need to convert them to true and false,
  ;;     ;; as the GPIO library we use operates with booleans, and not 1 and 0
  ;;     (map {0 false 1 true} bits)))
  )

(defn- debug-cmd-str [bits]
  ;; (let [bits-01 (->> bits
  ;;                    (mapv {false 0 true 1}))
  ;;       [ca op r5 r4 r3 r2 r1 r0 & data] bits-01]
  ;;   (log/debug
  ;;    ::debug-cmd-str
  ;;    {:bits bits-01
  ;;     :ca ca
  ;;     :op op
  ;;     :relay [r5 r4 r3 r2 r1 r0]
  ;;     :data (vec data)}))
  )

(defn- sleep [^Long delay]
  ;; (let [start ^Long (System/nanoTime)]
  ;;   (while (> (+ start delay) (System/nanoTime))))
  )

(defn bit-reader
  "Function that will pass and value to the FPGA and read the result"
  [handle-write handle-read buffer-write buffer-read cmd]
  ;; (let [high true
  ;;       low false
  ;;       out (atom [])
  ;;       bits {false 0 true 1}
  ;;       cmd-bits (generate-cmd-str :read cmd 0)]
  ;;   ;; (debug-cmd-str cmd-bits)
  ;;   (gpio/write handle-write
  ;;               (gpio/set-line+ buffer-write {:chip-select low}))
  ;;   ;; --
  ;;   (doseq [bit cmd-bits]
  ;;     (gpio/write handle-write
  ;;                 (gpio/set-line+ buffer-write {:mosi bit}))
  ;;     (sleep 100)
  ;;     (gpio/write handle-write
  ;;                 (gpio/set-line+ buffer-write {:clock high}))
  ;;     (sleep 100)
  ;;     (gpio/write handle-write
  ;;                 (gpio/set-line+ buffer-write {:clock low}))
  ;;     (sleep 100)
  ;;     (gpio/read handle-read buffer-read)
  ;;     (swap! out conj (get bits (gpio/get-line buffer-read :miso)))
  ;;     (sleep 100))
  ;;   ;; --

  ;;   (sleep 100)
  ;;   (gpio/write handle-write
  ;;               (gpio/set-line+ buffer-write {:chip-select high}))
  ;;   (drop 8 @out))
  )

(defn bit-read
  "Function that will return the FPGA value as decimal it uses bit reader"
  [{:keys [chip-select clock mosi miso]} cmd]
  ;; (convert-from-binary (bit-reader chip-select clock miso mosi cmd))
  )

(defn bit-write
  "Function that will pass values over to the FPGA via bit banging"
  ([handle buffer cmd value]
   ;; (bit-write handle buffer cmd nil value)
   )
  ([handle buffer cmd sub-cmd value]
   ;; (let [high true
   ;;       low false
   ;;       cmd-bits (generate-cmd-str :write cmd sub-cmd value)]
   ;;   ;; (debug-cmd-str cmd-bits)
   ;;   (gpio/write handle
   ;;               (gpio/set-line+ buffer {:chip-select low}))
   ;;   (doseq [bit cmd-bits]
   ;;     (gpio/write handle
   ;;                 (gpio/set-line+ buffer {:mosi bit}))
   ;;     (sleep 100)
   ;;     (gpio/write handle
   ;;                 (gpio/set-line+ buffer {:clock high}))
   ;;     (sleep 100)
   ;;     (gpio/write handle
   ;;                 (gpio/set-line+ buffer {:clock low}))
   ;;     (sleep 100))
   ;;   (sleep 100)
   ;;   (gpio/write handle
   ;;               (gpio/set-line+ buffer {:chip-select high})))
   ))


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
