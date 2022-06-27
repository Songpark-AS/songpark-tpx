(ns tpx.network.reporter
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [songpark.mqtt :as mqtt]
            [songpark.mqtt.util :as mqtt.util]
            [taoensso.timbre :as log]
            [tpx.ipc.serial :as serial]
            [tpx.data :as data]
            [tpx.config :refer [config]]
            [tpx.ipc.serial :refer [send-command]]
            [tpx.ipc.command :as command]))

;; keep track of if we have reported the last netconfig, reset to false when config changes
;; on network + mqtt up, if we have not reported the netconfig we do so


(defonce has-reported? (atom false))
(defonce current-network-config (atom {}))

(defn send-network-report [network-config mqtt-client]
  (when (not @has-reported?)
    (log/debug ::send-network-report "I should send a network report")
    (let [topic (mqtt.util/broadcast-topic (data/get-tp-id))]
      (mqtt/publish mqtt-client topic {:message/type :teleporter/net-config-report
                                       :teleporter/id (data/get-tp-id)
                                       :teleporter/network-config network-config}))
    (reset! has-reported? true)))

(defn send-current-network-report [mqtt-client]
  (send-network-report @current-network-config mqtt-client))

(defn get-local-ip []
  (let [iface (get-in config [:network :iface])
        {:keys [exit out err]} (->> (sh "bash" "-c" "ifconfig" iface))]
    (when-not (zero? exit)
      (log/error "get-local-ip error" {:exit exit
                                       :err err}))
    (->> out
         (re-find #"(?m)inet\s([0-9\.]+)")
         last)))

(defn get-netmask-ip []
  (let [iface (get-in config [:network :iface])
        {:keys [exit out err]} (->> (sh "bash" "-c" "ifconfig" iface))]
    (when-not (zero? exit)
      (log/error "get-local-ip error" {:exit exit
                                       :err err}))
    (->> out
         (re-find #"(?m)netmask\s([0-9\.]+)")
         last)))

(defn get-gateway-ip []
  (let [iface (get-in config [:network :iface])
        {:keys [exit out err]} (sh "bash" "-c" (str "ip route show 0.0.0.0/0 dev "
                                                    iface
                                                    " | cut -d\\  -f3"))]
    (when-not (zero? exit)
      (log/error "get-gateway-ip error" {:exit exit
                                         :err err}))
    (str/trim out)))

(defn get-dhcp? []
  (let [network-config-dir (get-in config [:network :config-dir])
        iface (get-in config [:network :iface])
        network-config-filepath (str network-config-dir iface)
        current-iface-config (if (.exists (clojure.java.io/file network-config-filepath))
                               (slurp network-config-filepath))]
    ;; if config file exists, strip comments, check for "inet static"

    ;; "inet static" found => false
    ;; "inet static" not found => true
    ;; current-iface-config is nil => true
    (if (not (nil? current-iface-config))
      (let [stripped-config (-> (str/replace current-iface-config #"(?m)^#.*$" "")
                                (str/trim))]
        (if (str/includes? stripped-config "inet static")
          false
          true))
      true)))

(defn fetch-current-network-config []
  (let [local-ip (get-local-ip)
        gateway-ip (get-gateway-ip)
        netmask-ip (get-netmask-ip)
        dhcp? (get-dhcp?)]
    (reset! current-network-config {:ip/address local-ip
                                    :ip/gateway gateway-ip
                                    :ip/subnet netmask-ip
                                    :ip/dhcp? dhcp?})
    (reset! has-reported? false)))

(defn fetch-and-send-current-network-config [mqtt-client]
  (fetch-current-network-config)
  (send-current-network-report mqtt-client))

(comment
  (get-dhcp?)
  (get-local-ip)
  (get-gateway-ip)
  (get-netmask-ip)
  (fetch-current-network-config)
  (send-network-report @current-network-config)
  (->> (sh "bash" "-c" "ifconfig" "eth1")
      :out
      ;; (re-find #"(?m)inet\s([0-9\.]+)")
      ;; last
      )
  )
