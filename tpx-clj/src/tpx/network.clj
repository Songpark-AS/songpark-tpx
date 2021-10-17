(ns tpx.network
  "Detects networks that are down and offers ways of resetting the network"
  (:require [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [tpx.config :refer [config]]
            [tpx.network.webserver :as webserver]
            [clojure.java.shell :refer [sh]]
            [taoensso.timbre :as log]))

(defonce run-checker? (atom false))

(defn set-network! [{:keys [ip gateway netmask dhcp?] :as opts}]
  (let [fake-reset? (get-in config [:network :fake-reset?])]
    (log/info "Set network" opts)
    (if fake-reset?
      (log/debug "I AM FAKE RESETTING THE NETWORK" opts)
      )))

(defn check-network-status [cmd]
  (let [cmds (str/split cmd #"\s")
        {:keys [exit out err]} (apply sh cmds)]
    (when-not (zero? exit)
      (log/error "Checking network status failed" {:exit exit
                                                   :err err}))
    (cond
      (= out "UP") :up
      (= out "DOWN") :down
      (= out "") :down
      (not (str/blank? out)) :up
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
              (set-network! network-options)
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
        (assoc this
               :data (atom {})
               :network-up? network-up?
               :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping Network detection")
          (reset! network-up? false)
          (assoc this
                 :started? false)))))

(defn network [settings]
  (map->Network settings))


(comment

  (check-network-status "ipconfig getifaddr en0")
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
  )
