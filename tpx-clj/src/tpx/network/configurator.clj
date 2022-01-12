(ns tpx.network.configurator
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [taoensso.timbre :as log]
            [tpx.config :refer [config]]))

(def ^:private template
  {:dhcp "auto $IFACE\niface $IFACE inet dhcp\n\thwaddress ether $HWADDR\n"
   :default-static "auto $IFACE\niface $IFACE inet static\n\thwaddress ether $HWADDR\n\taddress $IP\n\tnetmask $NETMASK\n"
   :static "auto $IFACE\niface $IFACE inet static\n\thwaddress ether $HWADDR\n\taddress $IP\n\tnetmask $NETMASK\n\tgateway $GATEWAY\n"})


;; TODO: Handler persistence of user configurations
(def ^:private netcfg (atom nil #_(read-config)))

(defn- init-config-dir []
  (let [data-dir (get-in config [:os :data-dir])]
    (when-not (.isDirectory (io/file data-dir))
      (try (io/make-parents data-dir)
           (catch Exception e
             (log/warn "Could not create data dir " (ex-data e)))))))

(defn- load-user-config []
  (let [data-dir (get-in config [:os :data-dir])]
    (try
      (reset! netcfg (read-string (slurp (str data-dir "netcfg.edn"))))
      (catch Exception e
        (log/warn "Could not read user network config file " (ex-data e))))))

(defn- save-user-config []
  (let [data-dir (get-in config [:os :data-dir])]
    (try
      (spit (str data-dir "netcfg.edn") @netcfg)
      (catch Exception e
        (log/warn "Could not write config to file " (ex-data e))))))

(defn- add-user-config [proto {:keys [name] :as config}]
  (swap! netcfg update-in [proto] assoc name (select-keys config [:ip :netmask :gateway])))

(defn- delete-user-config [proto name]
  (swap! netcfg update-in [proto] dissoc name))

(defn gen-iface-config
  "Generate config file content for use in /etc/network/interfaces.d"
  [template-k {:keys [ip netmask gateway] :as netconfig}]
  (let [iface (get-in config [:network :iface])
        hwaddr (get-in config [:network :hwaddr])
        new-netconfig (dissoc netconfig :dhcp?)
        replacements (reduce-kv (fn [m k v]
                                  (assoc m (str "$" (str/upper-case (name k))) v))
                                {} (merge {:iface (or iface "eth1")
                                           :hwaddr (or hwaddr "02:01:02:03:04:11")} new-netconfig))]
    (reduce-kv str/replace (template-k template) replacements)))


(comment
  ;; example generating the default static to be used for accessing
  ;; network configuration on teleporter with direct connection
  (let [config-dir "/tmp"]
    (spit (str config-dir "/static-default")
          (gen-iface-config :default-static {:ip "192.168.0.168"
                                             :netmask "255.255.255.0"})))
  
  (let [config-dir "/tmp"]
    (spit (str config-dir "/static-default")
          (gen-iface-config :static {:ip "192.168.11.222"
                                     :netmask "255.255.255.0"
                                     :gateway "192.168.11.1"})))

  (let [config-dir "/tmp"]
    (spit (str config-dir "/static-default")
          (gen-iface-config :dhcp {:iface "eth1"
                                   :hwaddr "00:11:AE:66:32:A5"}))) 
  
  )


