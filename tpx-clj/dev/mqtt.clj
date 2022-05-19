(ns mqtt
  (:require [songpark.mqtt :as mqtt]
            [songpark.mqtt.util :refer [teleporter-topic
                                        broadcast-topic]]
            [taoensso.timbre :as log]
            [tpx.init]
            [tpx.sync :refer [sync-platform!]]))



(defmethod mqtt/handle-message :does-this-break? [{:keys [counter mqtt-client] :as msg}]
  (log/debug :does-this-break? {:counter counter})
  (mqtt/publish mqtt-client (broadcast-topic "testus")
                {:message/type :broken?
                 :counter counter})
  (sync-platform!))


(comment

  (-> @tpx.init/system :mqtt-client :topics)

  (let [mqtt-client (:mqtt-client @tpx.init/system)
        topic (teleporter-topic (:id mqtt-client))]
    (doseq [idx (range 200)]
      (mqtt/publish mqtt-client
                    "testus"
                    {:message/type :foo/brokne?
                     :teleporter/id (:id mqtt-client)
                     :teleporter/volume (rand-int 50)})))

  (let [mqtt-client (:mqtt-client @tpx.init/system)]
    (mqtt/connected? mqtt-client))

  (let [client @(:client (:mqtt-client @tpx.init/system))]
    (.getTimeToWait client)
    #_(.setTimeToWait client 1000))
  )
