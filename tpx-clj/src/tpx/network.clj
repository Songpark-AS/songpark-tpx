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

(defn- run-checker [options]
  (let [cmd (get-in options [:network :check-network-status-cmd])
        network-options (get-in options [:network :default-network])
        sleep-timer (get-in options [:network :sleep-timer])
        webserver-settings (get-in options [:network :webserver])
        server (atom nil)]
    (future
      (while @run-checker?
        (if (nil? @server)
          (let [status (check-network-status cmd)]
            (when (= status :down)
              (log/debug ::run-checker "Setting default static IPv4")
              (activate-iface-config (gen-iface-config :default-static network-options))
              (component/start (webserver/webserver {:config webserver-settings
                                                     :set-network! set-network!
                                                     :server server})))))
        (Thread/sleep sleep-timer)))))

(defrecord Network [started? network-up? data]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (let [network-up? (atom true)]
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
          (future-cancel (:future @(:data this))) ;; Not working, perhaps not important?
          (assoc this
                 :started? false)))))

(defn network [settings]
  (map->Network settings))


(comment

  ;; Mac
  (check-network-status "ipconfig getifaddr en0")

  ;; Zedboard
  (check-network-status "ip -o link show eth1 | cut -d ' ' -f 9 | tr -d '\n'")
  
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

  
  (def network-manager (atom nil))
  (reset! network-manager (component/start (network config)))
  (component/stop network-manager)

  (future-cancelled? (:future @(:data @network-manager)))
  (:future @(:data @network-manager))
  (-> network-manager)

  )







