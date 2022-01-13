(ns mqtt
  (:require [tpx.data :as data]
            [tpx.config :refer [config]]
            [tpx.init]))

(comment 
  (let [mqtt-manager (:mqtt-manager @tpx.init/system)]
    (.publish mqtt-manager (data/get-tp-log-topic) {:message/type :teleporter/log
                                                    :message/body {:teleporter/id (data/get-tp-id)
                                                                   :log/level :info
                                                                   :log/timestamp "My timestamp"
                                                                   :log/data "This is my test"}}))


  (let [mqtt-manager (:mqtt-manager @tpx.init/system)
        fake-reset? (get-in config [:network :fake-reset?])
        network-config-dir (get-in config [:network :config-dir])
        iface (get-in config [:network :iface])
        network-config-filepath (str network-config-dir iface)
        current-iface-config (if (.exists (clojure.java.io/file network-config-filepath))
                               (slurp network-config-filepath))]

    (prn current-iface-config))

  )
