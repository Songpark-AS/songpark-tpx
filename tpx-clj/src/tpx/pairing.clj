(ns tpx.pairing
  (:require [tpx.gpio :as gpio]
            [taoensso.timbre :as log]))

(def possible-states #{:unpaired :paired :pairing})
;; pair can be one of #{:not-paired, :paired, :pairing)
(defonce pair-status (atom :unpaired))


(defn pairing? []
  (= @pair-status :pairing))

(defn unpaired? []
  (= @pair-status :unpaired))

(defn paired? []
  (= @pair-status :paired))

(defn set-status [gpio status]
  (assert (possible-states status) (str "status needs to be one of " possible-states))
  (log/debug ::set-status status)
  (reset! pair-status status)
  (case status
    :pairing (gpio/start-blink gpio :led/link 500)
    :paired (do (gpio/stop-blink gpio :led/link)
                (gpio/set-led gpio :led/link :on))
    :unpaired (gpio/start-blink gpio :led/link 3000)
    nil))
