(ns jam
  (:require [songpark.jam.tpx :as jam.tpx]
            [songpark.mqtt :as mqtt]
            [songpark.mqtt.util :refer [teleporter-topic]]
            [tpx.init]))


(comment

  (java.util.UUID/randomUUID)

  (-> @tpx.init/system :mqtt-client :topics)
  
  (let [mqtt-client (:mqtt-client @tpx.init/system)
        topic (teleporter-topic (:id mqtt-client))
        jam-start {:message/type :jam.cmd/start
                   :jam/id #uuid "c8bd2fd6-3146-475e-9f23-b47ca434a89c"
                   :jam/members [#uuid "7fdf0551-b5fc-557d-bddc-2ca5b1cdfaa6" #uuid "c700abce-c109-58fc-a3e7-86bff412a872"]
                   :jam/sip {#uuid "7fdf0551-b5fc-557d-bddc-2ca5b1cdfaa6" "sip:9104@voip1.inonit.no",
                             #uuid "c700abce-c109-58fc-a3e7-86bff412a872" "sip:9108@voip1.inonit.no"}}
        jam-stop {:message/type :jam.cmd/stop
                  :jam/id #uuid "c8bd2fd6-3146-475e-9f23-b47ca434a89c"}]
    (mqtt/publish mqtt-client topic jam-stop))

  (let [jam (:jam @tpx.init/system)]
    ;;(jam.tpx/get-state jam)
    (keys @(:data jam))
    (dissoc @(:data jam) :mqtt-client :jam :ipc)
    ;;(select-keys @(:data jam) [:jam/id :jam/members :jam/sip])
    )

  )
