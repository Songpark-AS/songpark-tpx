(ns tpx-clj.core
  (:gen-class)
  (use '[clojure.java.shell :only [sh]]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn talk-to-cs7 [name]
  (let [[exit out err](sh "docker" "run" name)]
    (println out)))
