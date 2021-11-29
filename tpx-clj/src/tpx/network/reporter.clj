(ns tpx.network.reporter
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [tpx.ipc.serial :as serial]
            [tpx.data :as data]
            [tpx.config :refer [config]]
            [tpx.ipc.serial :refer [send-command]]
            [tpx.ipc.command :as command]
            [clojure.java.shell :refer [sh]]))

;; keep track of if we have reported the last netconfig, reset to false when config changes
;; on network + mqtt up, if we have not reported the netconfig we do so


;; TODO: send mock mqtt message
;; TODO: handle mock mqtt message in the app
;; TODO: test for realsies
;; TODO: make into system-component

(defonce has-reported? (atom false))
(defonce current-network-config (atom {}))

(defn send-network-report [network-config mqtt-manager]
  (when (not @has-reported?)
    (log/debug ::send-network-report "I should send a network report")
    (let [topic (data/get-tp-report-net-config-topic)]
      (.publish mqtt-manager topic {:message/type :teleporter/net-config-report
                                    :message/body {:teleporter/id (data/get-tp-id)
                                                   :teleporter/network-config network-config}}))
    (reset! has-reported? true)))

(defn send-current-network-report [mqtt-manager]
  (send-network-report @current-network-config mqtt-manager))

(defn get-local-ip [strip-cidr?]
  (let [iface (get-in config [:network :iface])
        {:keys [exit out err]} (sh "bash" "-c" (str "nmcli -t -f IP4.ADDRESS dev show " iface " | sed 's/IP4\\.ADDRESS\\[1\\]\\://g'"))]
    (when-not (zero? exit)
      (log/error "get-local-ip error" {:exit exit
                                       :err err}))
    (if strip-cidr?
      (first (clojure.string/split (clojure.string/trim-newline out) #"/"))
      (clojure.string/trim-newline out))))


(defn get-netmaskrange [netmask]
  (map #(bit-and %1 %2) (concat (repeat netmask 1) (repeat (- 32 netmask) 0)) (repeat 32 1)))

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
  (let [iface (get-in config [:network :iface])
        {:keys [exit out err]} (sh "bash" "-c" (str "nmcli -t -f IP4.GATEWAY dev show " iface " | sed 's/IP4\\.GATEWAY\\://g'"))]
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

    ;; "inet static" found => false
    ;; "inet static" not found => true
    ;; current-iface-config is nil => true
    (if (not (nil? current-iface-config))
      (let [stripped-config (clojure.string/trim (clojure.string/replace current-iface-config #"(?m)^#.*$" ""))]
        (if (clojure.string/includes? stripped-config "inet static")
          false
          true))
      true)))

(defn fetch-current-network-config []
  (let [local-ip (get-local-ip true)
        gateway-ip (get-gateway-ip)
        netmask-ip (get-netmask-ip)
        dhcp? (get-dhcp?)]
    (reset! current-network-config {:ip/address local-ip
                                    :ip/gateway gateway-ip
                                    :ip/subnet netmask-ip
                                    :ip/dhcp? dhcp?})
    (reset! has-reported? false)))

(defn fetch-and-send-current-network-config [mqtt-manager]
  (fetch-current-network-config)
  (send-current-network-report mqtt-manager))

(comment
  (get-dhcp?)
  (get-local-ip true)
  (get-gateway-ip)
  (get-netmask-ip)
  (fetch-current-network-config)
  (send-network-report @current-network-config)
  )
