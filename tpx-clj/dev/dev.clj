(ns dev
  (:require [tpx.init :as init]
            [taoensso.timbre :as log]))

(defn restart
  "stop and start tpx"
  []
  ;; set the log level to info or jetty will spam your REPL console,
  ;; significantly slowing down responses
  (log/merge-config! {:min-level :debug
                      :ns-filter {:deny #{"org.eclipse.jetty.*"
                                          "io.grpc.netty.shaded.io.netty.*"
                                          "org.opensaml.*"}
                                  :allow #{"*"}}})

  
  (init/stop)
  (init/init))


(comment 
  (init/stop)
  (restart)
  
  )
