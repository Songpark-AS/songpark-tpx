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
  (when (= status :pairing)
    (gpio/start-blink gpio :led/prompt 1000))
  (when (or (= status :not-paired)
            (= status :paired))
    (gpio/stop-blink gpio :led/prompt)
    (gpio/set-led gpio :led/prompt :on)))
