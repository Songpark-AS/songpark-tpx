(ns tpx.gpio.actions
  (:require [taoensso.timbre :as log]
            [tpx.config :refer [config]]
            [tpx.network :refer [set-network!]]))

(defn get-settings []
  {:context {:set-network! set-network!}
   :buttons {:button/push1
             (fn [{:keys [delay set-network!]}]
               (log/debug "Pressed :button/push1" {:delay delay})
               ;; when the delay is larger than 5 seconds or otherwise
               ;; we set the network to use DHCP again
               (when (> delay (get-in config [:network :button-delay] 5000))
                 (set-network! {:dhcp? true})))
             :button/rotary
             (fn [{:keys [delay]}]
               (log/debug "Pressed :button/rotary" {:delay delay}))}})
