(ns dev
  (:require [tpx.init :as init]
            [taoensso.timbre :as log]))

(defn restart
  "stop and start tpx"
  []
  ;; set the log level to info or jetty will spam your REPL console,
  ;; significantly slowing down responses
  (log/merge-config! {:level        :debug
                      :ns-blacklist ["org.eclipse.jetty.*"
                                     "io.grpc.netty.shaded.io.netty.*"
                                     "org.opensaml.*"]})
  (init/stop)
  (init/init))

(comment
  ;; stop and star
  (restart))
