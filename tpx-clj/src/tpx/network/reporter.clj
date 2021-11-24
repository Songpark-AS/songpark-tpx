(ns tpx.network.reporter
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [tpx.ipc.serial :as serial]
            [tpx.data :as data]
            [tpx.config :refer [config]]
            [tpx.ipc.serial :refer [send-command]]
            [tpx.ipc.command :as command]
            [clojure.java.shell :refer [sh]]
            [tpx.init]))

;; keep track of if we have reported the last netconfig, reset to false when config changes
;; on network + mqtt up, if we have not reported the netconfig we do so


;; TODO: send mock mqtt message
;; TODO: handle mock mqtt message in the app
;; TODO: Check if it is using DHCP or not
;; TODO: test for realsies
;; TODO: make into system-component

(defonce has-reported? (atom false))
(defonce current-network-config (atom {}))

(defn send-network-report [network-config]
  (when (not @has-reported?)
    (log/debug ::send-network-report "I should send a network report")
    (let [mqtt-manager (:mqtt-manager @tpx.init/system)
          topic "replace-me-topic"]
      (.publish mqtt-manager topic {:message/type :teleporter/net-config-report
                                    :message/body {:teleporter/id (data/get-tp-id)
                                                   :teleporter/network-config network-config}}))
    (reset! has-reported? true)))

(defn get-local-ip [strip-cidr?]
  (let [{:keys [exit out err]} (sh "bash" "-c" "nmcli -t -f IP4.ADDRESS dev show eth1 | sed 's/IP4\\.ADDRESS\\[1\\]\\://g'")]
    (when-not (zero? exit)
      (log/error "get-local-ip error" {:exit exit
                                       :err err}))
    (if strip-cidr?
      (first (clojure.string/split (clojure.string/trim-newline out) #"/"))
      (clojure.string/trim-newline out))))

(defn get-netmask-ip []
  (let [ip-subrange [128 64 32 16 8 4 2 1]
        netmask (Integer/parseInt (last (clojure.string/split (get-local-ip false) #"/")))
        netmask-range (partition 8 (get-netmaskrange netmask))]
    (->> netmask-range
         (reduce (fn [out subrange]
                   (conj out (reduce + (map #(if (zero? %2)
                                               0
                                               %1) ip-subrange subrange))))
                 [])
         (clojure.string/join "."))))

(defn get-gateway-ip []
  (let [{:keys [exit out err]} (sh "bash" "-c" "nmcli -t -f IP4.GATEWAY dev show eth1 | sed 's/IP4\\.GATEWAY\\://g'")]
    (when-not (zero? exit)
      (log/error "get-gateway-ip error" {:exit exit
                                       :err err}))
    (clojure.string/trim-newline out)))

(defn get-dhcp? []
  (let [
        network-config-dir (get-in config [:network :config-dir])
        iface (get-in config [:network :iface])
        network-config-filepath (str network-config-dir iface)
        current-iface-config (if (.exists (clojure.java.io/file network-config-filepath))
                               (slurp network-config-filepath))]
    ;; if config file exists, strip comments, check for "inet static"

    ;; current-iface-config is nil => true
    ;; "inet static" not found => true
    ;; "inet static" found => false
    (if (not (nil? current-iface-config))
      (re-matches #"^#.*$" (clojure.string/trim current-iface-config))
      true))
  )

(defn fetch-current-network-config []
  (let [local-ip (get-local-ip true)
        gateway-ip (get-gateway-ip)
        netmask-ip (get-netmask-ip)]
    (reset! current-network-config {:teleporter/local-ip local-ip
                                    :teleporter/gateway-ip gateway-ip
                                    :teleporter/netmask-ip netmask-ip})
    (reset! has-reported? false)))

(defn get-netmaskrange [netmask]
  (map #(bit-and %1 %2) (concat (repeat netmask 1) (repeat (- 32 netmask) 0)) (repeat 32 1)))

(comment
  (get-dhcp?)
  (get-local-ip true)
  (get-gateway-ip)
  (get-netmask-ip)
  (fetch-current-network-config)
  (send-network-report {:teleporter/local-ip "192.168.11.123"
                        :teleporter/gateway-ip "192.168.11.1"
                        :teleporter/mask-ip "255.255.255.0"
                        :teleporter/DHCP? true})


  (let [
        network-config-dir (get-in config [:network :config-dir])
        iface (get-in config [:network :iface])
        network-config-filepath (str network-config-dir iface)
        current-iface-config (if (.exists (clojure.java.io/file network-config-filepath))
                               (slurp network-config-filepath))]
    ;; if config file exists, strip comments, check for "inet static"

    ;; current-iface-config is nil => true
    ;; "inet static" not found => true
    ;; "inet static" found => false
    (if (not (nil? current-iface-config))
      (log/debug
       (clojure.string/replace (clojure.string/trim current-iface-config) #"^#.*$" ""))
      ))


  )
