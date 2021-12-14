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

(defn check-network-status [cmd]
  (let [{:keys [exit out err]} (sh "bash" "-c" cmd)]
    (when-not (zero? exit)
      (log/error "Checking network status failed" {:exit exit
                                                   :err err}))
    (cond
      (= out "UP") :up
      (= out "UNKNOWN") :up
      (= out "DOWN") :down
      (= out "") :down
      #_#_(not (str/blank? out)) :up ;; this seemed to be causing trouble, but that was before I trimmed newlined from cut output
      :else :down)))

(defn check-network-status-ping [cmd]
(defn check-network-status-return-code [cmd]
  (let [{:keys [exit]} (sh "bash" "-c" cmd)]
    (cond
      (zero? exit) :up
      :else :down)))

(defn- run-checker [options]
  (let [cmd (get-in options [:network :check-network-status-cmd])
        ping-cmd (get-in options [:network :check-network-status-ping-cmd])
        curl-cmd (get-in options [:network :check-network-status-curl-cmd])
        network-options (get-in options [:network :default-network])
        sleep-timer (get-in options [:network :sleep-timer])
        webserver-settings (get-in options [:network :webserver])
        server (atom nil)
        webserver (atom nil)]
    (future
      (while @run-checker?
        (let [status (check-network-status-return-code curl-cmd)]
          (log/debug ::run-checker "Checking network status")
          (if (nil? @webserver)
            (when (= status :down)
              (log/debug ::run-checker "Network is down, lets start the webserver")
              (log/debug ::run-checker "Setting default static IPv4")
              (activate-iface-config (gen-iface-config :default-static network-options))
              (reset! webserver
                      (component/start (webserver/webserver {:config webserver-settings
                                                             :set-network! set-network!
                                                             :server server}))))
            (when (and (= status :up) (not (iface-config-equals-current-config? (gen-iface-config :default-static network-options))))
              (log/debug ::run-checker "Network is up again, lets shutdown the webserver")
              (component/stop @webserver)
              (reset! webserver nil))))

        (Thread/sleep sleep-timer)))))

(defrecord Network [started? network-up? data]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (let [server (atom nil)
            network-up? (atom true)]
        (log/info "Starting Network detection")
        (reset! run-checker? true)
        (assoc this
               :data (atom {:future (run-checker config)})
               :network-up? network-up?
               :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping Network detection")
          (reset! network-up? false)
          (reset! run-checker? false)
          (future-cancel (:future @(:data this)))
          (assoc this
                 :started? false)))))

(defn network [settings]
  (map->Network settings))


(comment

  ;; Mac
  (check-network-status "ipconfig getifaddr en0")

  ;; Zedboard
  (check-network-status "ip -o link show eth1 | cut -d ' ' -f 9 | tr -d '\n'")

  (check-network-status-return-code "curl http://127.0.0.1:3000/health")
  (set-network! {})

  (def server (atom nil))
  (def webserver (atom nil))
  (let [webserver-settings {:ip "0.0.0.0"
                            :port 8080
                            :thread 1}]
    (reset! webserver
            (component/start (webserver/webserver {:config webserver-settings
                                                   :set-network! set-network!
                                                   :server server}))))
  (component/stop @webserver)
  (get-in config [:network :check-network-status-ping-cmd])

  (def network-manager (atom nil))
  (reset! network-manager (component/start (network (get-in config [:network]))))
  (component/stop @network-manager)
  (check-network-status-ping "ping -c 3 google.com")
  )
