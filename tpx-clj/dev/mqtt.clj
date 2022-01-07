(ns mqtt
  (:require [tpx.data :as data]
            [tpx.init]))

(comment 
  (let [mqtt-manager (:mqtt-manager @tpx.init/system)]
    (.publish mqtt-manager (data/get-tp-log-topic) {:message/type :teleporter/log
                                                    :message/body {:teleporter/id (data/get-tp-id)
                                                                   :log/level :info
                                                                   :log/timestamp "My timestamp"
                                                                   :log/data "This is my test"}}))
  )
