(ns tpx-clj.core
  (:require
   [clojurewerkz.machine-head.client :as mh]
   [clojure.java.shell :as shell])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn talk-to-cs7 
  "Supposed to talk to CS7 via STDIN"
  [name]
  (let [data (shell/sh "docker" "run" name)]
    (:out data)))