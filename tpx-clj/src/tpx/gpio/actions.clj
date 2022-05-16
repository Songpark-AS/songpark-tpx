(ns tpx.gpio.actions
  (:require [taoensso.timbre :as log]
            [tpx.config :refer [config]]))

(defn get-settings []
  {:buttons (atom {:button/push1
                   (fn [gpio delay]
                     (log/debug "Pressed :button/push1" {:delay delay}))})})
