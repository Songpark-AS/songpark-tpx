(ns tpx.network
  "Detects networks that are down and offers ways of resetting the network"
  (:require [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [tpx.config :refer [config]]
            [tpx.network.webserver :as webserver]
            [tpx.network.configurator :refer [gen-iface-config]]
            [clojure.java.shell :refer [sh]]
            [taoensso.timbre :as log]))


(defonce run-checker? (atom false))

(defn iface-config-equals-current-config? [iface-config]
  (let [
        network-config-dir (get-in config [:network :config-dir])
        iface (get-in config [:network :iface])
        network-config-filepath (str network-config-dir iface)
        current-iface-config (if (.exists (clojure.java.io/file network-config-filepath))
                               (slurp network-config-filepath))]
    (= iface-config current-iface-config)))

(defn activate-iface-config [iface-config]
  (let [fake-reset? (get-in config [:network :fake-reset?])
        network-config-dir (get-in config [:network :config-dir])
        iface (get-in config [:network :iface])
        network-config-filepath (str network-config-dir iface)
        current-iface-config (if (.exists (clojure.java.io/file network-config-filepath))
                               (slurp network-config-filepath))]
    (if fake-reset?
      (do (log/debug ::activate-iface-config "Updating interface configuration")
          (spit "/tmp/net-eth1" iface-config))
      (do (log/debug ::activate-iface-config "Updating interface configuration")
          (sh "bash" "-c" (str "ifdown " iface))
          (spit network-config-filepath iface-config)
          ;; Give it some time to write the file
          (Thread/sleep 200)
          (sh "bash" "-c" (str "ifup " iface))))))

(defn set-network! [{:keys [ip netmask gateway dhcp?] :as opts}]
  (let [fake-reset? (get-in config [:network :fake-reset?])]
    (log/info "Set network" opts)
    (if fake-reset?
      (do (log/debug "I AM FAKE RESETTING THE NETWORK" opts)
          (if dhcp?
            (activate-iface-config (gen-iface-config :dhcp opts))
            (activate-iface-config (gen-iface-config :static opts))))
      (if dhcp?
        (activate-iface-config (gen-iface-config :dhcp opts))
        (activate-iface-config (gen-iface-config :static opts))))))

(defn check-network-status-return-code [cmd]
  (let [{:keys [exit]} (sh "bash" "-c" cmd)]
    (cond
      (zero? exit) :up
      :else :down)))


(comment

  (check-network-status-return-code "curl http://127.0.0.1:3000/health")
  (set-network! {})

  (check-network-status-ping "ping -c 3 google.com")
  )
