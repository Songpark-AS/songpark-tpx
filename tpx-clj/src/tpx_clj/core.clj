(ns tpx-clj.core
  (:require
    [clojurewerkz.machine-head.client :as mh])
  (use '[clojure.java.shell :only [sh]])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn talk-to-cs7 [name]
  (let [[exit out err](sh "docker" "run" name)]
    (println out)))