(ns tpx-clj.core
<<<<<<< HEAD
  (:gen-class)
  (use '[clojure.java.shell :only [sh]]))
=======
  (:require
    [clojurewerkz.machine-head.client :as mh])
  (:gen-class))
>>>>>>> 28eaaedbee3665c5bd276a2998cf42f5c278d802

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn talk-to-cs7 [name]
  (let [[exit out err](sh "docker" "run" name)]
    (println out)))
