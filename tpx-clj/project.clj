(defproject tpx "0.2.1"
  :description "Teleporter software that is the glue between the FPGA world and the wider system"
  :url ""
  :license {:name ""
            :url ""}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 ;; configuration
                 [cprop "0.1.19"]
                 ;; file system
                 [me.raynes/fs "1.4.6"]
                 ;; logging
                 [com.taoensso/timbre "5.1.2"]
                 [raven-clj "1.6.0"]
                 ;; http server
                 [http-kit "2.3.0"]
                 [ring/ring-core "1.9.4"]
                 ;; structure
                 [com.stuartsierra/component "1.0.0"]
                 ;; serial comms
                 [clj-serial "2.0.5"]

                 ;; songpark specific libraries
                 [songpark/jam "1.0.3"]
                 [songpark/mqtt "1.0.3"]
                 [songpark/common "0.2.0"]

                 ;; GPIO for Clojure
                 [io.helins/linux.gpio "2.0.1"]

                 ;; database
                 [codax "1.3.1"]

                 ;; json
                 [cheshire "5.10.0"]
                 ;; scheduler
                 [jarohen/chime "0.3.3"]

                 [clojure-interop/java.net "1.0.5"]]
  :main tpx.core
  :target-path "target/%s"
  :test-paths ["test"]
  ;; The REPL on the zedboard starts really slow. 2 minutes was too little, so we added 10 minutes
  ;; which seems to do the trick. It's somewhere around the 3 minute mark from what I can see, but
  ;; 10 minutes to be on the safe side. Just be patient, and the REPL will show up
  :repl-options {:timeout 600000}
  :javac-options ["--release" "8"]
  :profiles {:dev {:source-paths ["src" "dev"]
                   :resource-paths ["dev-resources" "resources"]
                   :dependencies [[midje "1.9.9"]
                                  [ring/ring-mock "0.4.0"]

                                  [hashp "0.2.0"]
                                  [clj-commons/spyscope "0.1.48"]]
                   :injections [(require 'spyscope.core)]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-plantuml "0.1.22"]]}
             :profiling {:source-paths ["src" "dev"]
                         :resource-paths ["dev-resources" "resources"]
                         :dependencies [[clj-commons/spyscope "0.1.48"]]
                         :injections [(require 'spyscope.core)]
                         :jvm-opts ["-Dcom.sun.management.jmxremote"
                                    "-Dcom.sun.management.jmxremote.port=1089"
                                    "-Dcom.sun.management.jmxremote.ssl=false"
                                    "-Dcom.sun.management.jmxremote.authenticate=false"
                                    "-Djava.rmi.server.hostname=10.100.200.110"]}
             :uberjar {:aot :all
                       :uberjar-name "tpx.jar"
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
