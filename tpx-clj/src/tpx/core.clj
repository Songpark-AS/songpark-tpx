(ns tpx.core
  (:gen-class)
  (:require [taoensso.timbre :as log]
            [tpx.init :as tpx.init]))

(defn -main
  "Prepares mqtt connection"
  [& _args]
  ; (println "Hello, World!")
  (log/set-level! :info)
  (tpx.init/init)
)

