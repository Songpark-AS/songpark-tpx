(ns dev
  (:require [codax.core :as codax]
            [com.stuartsierra.component :as component]
            [tpx.database :refer [db]]
            [tpx.init :as init]
            [tpx.logger :as logger]
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

(defonce logger (atom nil))

(defn start-logging []
  (if-let [l @logger]
    (log/info "Logger already started")
    (reset! logger (component/start (logger/logger {})))))


(comment 
  (init/stop)
  (restart)
  

  ;; start logging locally on the file system
  ;; useful for multithreaded environments, as not every log is printed to stdout
  (start-logging)

  (codax/assoc-at! @db [:test :foo] :bar)
  (codax/get-at! @db [:test :foo])
  )
