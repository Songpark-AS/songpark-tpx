(ns dev
  (:require [tpx.init :as tpx.init]
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
  
  (tpx.init/stop)
  (tpx.init/init))


(comment 
  (tpx.init/stop)
  (restart)
  
  )
