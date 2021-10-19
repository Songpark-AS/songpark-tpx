(ns tpx.message.dispatch.jam
  (:require [tpx.data :as data]
            [tpx.message.dispatch.interface :as message]
            [tpx.ipc :refer [send-message!]]
            [tpx.ipc.command :as ipc.command]
            [taoensso.timbre :as log]))

(defmethod message/dispatch :jam.cmd/start [{:message/keys [body] :as _msg}]
  (data/set-jam-id! (:jam/topic body))
  (log/debug :jam.cmd/start (dissoc _msg :message-service :mqtt-manager))
  (let [members (:jam/members body)]
    (log/debug members)
    (log/debug (str "Subscribing to " (data/get-jam-teleporters)))
    (send-message! {:message/type :platform.cmd/subscribe
                    :message/meta {:mqtt/topics {(data/get-jam-teleporters) 0}}})
    (ipc.command/jam-start (:jam/members body) (:jam/sip body))))

(defmethod message/dispatch :jam.cmd/stop [{:message/keys [body]
                                            :keys [mqtt-manager]}]
  (ipc.command/jam-stop (:jam/members body) (:jam/sip body))
  (log/debug :dispatch (str "Unsubscribing from " (data/get-jam-teleporters)))
  (.unsubscribe mqtt-manager [(data/get-jam-teleporters)])
  (data/clear-jam-id!))

