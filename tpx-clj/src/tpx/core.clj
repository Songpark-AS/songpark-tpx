(ns tpx.core
  (:gen-class)
  (:require [tpx.init :as tpx.init]))

(defn -main
  [& _args]
  (tpx.init/init))

