(ns tpx.gpio.actions
  (:require [songpark.common.communication :refer [POST]]
            [taoensso.timbre :as log]
            [tpx.config :refer [config]]
            [tpx.data :as data]
            [tpx.network :refer [set-network!]]
            [tpx.pairing :as pairing]
            [tpx.utils :as util]))

(defn get-settings []
  {:context {:set-network! set-network!}
   :buttons {:button/prompt
             (fn [{:keys [delay set-network! gpio]}]
               (log/debug "Pressed :button/push1" {:delay delay})
               ;; when the delay is larger than 5 seconds or otherwise
               ;; we set the network to use DHCP again
               (cond
                 (> delay (get-in config [:network :button-delay] 5000))
                 (do (log/debug "Resetting network to DHCP")
                     (set-network! {:dhcp? true}))

                 (pairing/pairing?)
                 (let [platform-url (util/get-platform-url "/api/teleporter/pair")]
                   (log/debug "Setting to paired")

                   (POST platform-url
                         {:message/type :pairing/paired
                          :teleporter/id (data/get-tp-id)
                          :auth.user/id (data/get-user-id)}
                         (fn [_]
                           (pairing/set-status gpio :paired))))

                 ;; do nothing
                 :else
                 (do (log/debug "Do nothing")
                     nil)))}})
