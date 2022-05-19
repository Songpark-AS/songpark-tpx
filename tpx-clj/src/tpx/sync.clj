(ns tpx.sync
  "Namespace for handling sync. Send out the latest values. Mostly used by platform"
  (:require [codax.core :as codax]
            [songpark.common.communication :refer [POST]]
            [songpark.mqtt :as mqtt]
            [taoensso.timbre :as log]
            [tpx.config :refer [config]]
            [tpx.data :as data]
            [tpx.database :refer [db get-hardware-values]]))



(defonce saved-future (atom nil))

(defn sync-platform!
  ([]
   (sync-platform! nil))
  ([{:keys [mqtt-client topic message] :as context}]
   (let [platform-url (str (get-in config [:ipc :platform]) "/api/teleporter")
         tp-id (data/get-tp-id)
         values (get-hardware-values)]
     (when @saved-future
       (future-cancel @saved-future))
     (reset! saved-future
             (future
               ;; wait x seconds until we actually send an update
               ;; this is for spammy updates, where we wait until it's calmed down a bit
               ;; before updating the server
               (Thread/sleep 5000)
               (POST platform-url {:teleporter/id tp-id
                                   :teleporter/settings values}
                     (fn [_]
                       (reset! saved-future nil)
                       (log/info "Synced with platform"))
                     (fn [response]
                       (reset! saved-future nil)
                       (log/warn "Failed to sync with platform" {:response response})))
               (when mqtt-client
                 (mqtt/publish mqtt-client topic message)))))))
