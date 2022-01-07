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
    (data/set-jam-session! body)
    (ipc.command/jam-start (:jam/members body) (:jam/sip body))))

(defmethod message/dispatch :jam.cmd/stop [{:message/keys [body]
                                            :keys [mqtt-manager]}]
  (ipc.command/jam-stop (:jam/members body) (:jam/sip body))
  
  (future
    ;; sleep for 5 seconds, then close everything
    ;; quick and dirty
    (try
      (log/debug "Sleeping for 5 seconds, before unsubscribing from jam-teleporters channel and clearing jam-id and jam-session")
      (Thread/sleep (* 5 1000))
      (log/debug :dispatch (str "Unsubscribing from " (data/get-jam-teleporters)))
      (.unsubscribe mqtt-manager [(data/get-jam-teleporters)])
      (.publish mqtt-manager (data/get-jam) {:message/type :jam.cmd/ended
                                             :message/body {:teleporter/id (data/get-tp-id)
                                                            :jam/uuid (data/get-jam-id)}})
      (data/clear-jam-id!)
      (data/clear-jam-session!)
      (catch Throwable t
        (log/error "Caught " (str t))))))
