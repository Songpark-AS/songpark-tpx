(ns tpx.http
  (:require ;; [taoensso.timbre :as log]
            #_[songpark.common.communication :refer [POST]]
            [common.platform.connect.tp :as connect.tp]
            [tpx.mqtt :as tpx.mqtt]
            [tpx.ipc :as tpx.ipc]))

;;! Daniel's stuff

;; This will be handled via communication in common, such
;; that we only need to use the api endpoint part of the
;; url in our request fns
#_#_#_#_#_#_#_(defonce url "http://localhost:3000/api/teleporter")

;; this will not be here, just for initial testing
(defonce state (atom {}))


(defn on-error [error-response]
  (log/error error-response))

(defn on-success [response]
  (log/debug response))

(defn off [bits]
  (POST url {:teleporter/bits bits
             :teleporter/on false
             :teleporter/available false}
    on-success
    on-error))

(defn on-and-unavailable [bits]
  (POST url {:teleporter/bits bits
             :teleporter/on true
             :teleporter/available false}
    on-success
    on-error))

(defn available [bits]
  (POST url {:params {:teleporter/bits bits
                      :teleporter/on true
                      :teleporter/available true}
             :handler on-success
             :error-handler on-error}))

;;! --- Sindre's and my stuff ---
;! --- Netcode ---
(def global-conn-map (atom {}))

(defn initiate-communications
  "Initiates communications with the backend,
   telling the backend its tpID,
   then initiates communications to MQTT's pub/sub"
  [handler-map]
  (let [tpid (tpx.ipc/retrieve-tpID)
        plat-response (connect.tp/init {:tpid tpid}) ; Will be a map
        uuid (:uuid plat-response)
        status (:status plat-response)]
    (if (and status uuid)
      (let [conn-map (tpx.mqtt/common-connect-init uuid)]
        (prn "Here be the conn-map from init arrrr!: " conn-map)
        (reset! global-conn-map conn-map)
        (prn "Here be global conn-map: " @global-conn-map)
        ;; (mqtt-connection/subscribe conn-map handler-map))
        (tpx.mqtt/common-subscribe conn-map handler-map))
      (println "Teleporter connection to platform, failed"))
    (println "This is uuid: " uuid)
    uuid))

(comment
  #_#_#_;; turn on teleporter
  @(on-and-unavailable "0000")

  ;; make teleporter available
  @(available "0000")

  ;; turn ff teleporter
  @(off "0000")

  )


