(defproject tpx "0.1.1-SNAPSHOT"
  :description "Songpark's Teleporter's communications' logic"
  :url "http://example.com/FIXME"
  :license {:name ""
            :url ""}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 ;; configuration
                 [cprop "0.1.11"]
                 ;; file system
                 [me.raynes/fs "1.4.6"]
                 ;; logging
                 [com.taoensso/timbre "5.1.2"]
                 ;; structure
                 [com.stuartsierra/component "1.0.0"]
                 ;; serial comms
                 [clj-serial "2.0.5"]
                 ;; mqtt implementation
                 [clojurewerkz/machine_head "1.0.0"]
                 [com.cognitect/transit-clj "1.0.324"]
                 ;; songpark's common (will also contain an mqtt implementation later)
                 [songpark/common "0.1.1-SNAPSHOT"]
                 [clojure-interop/java.net "1.0.5"]]
  :main ^:skip-aot tpx.core
  :target-path "target/%s"
  :test-paths ["test"]
  ;; The REPL on the zedboard starts really slow. 2 minutes was too little, so we added 10 minutes
  ;; which seems to do the trick. It's somewhere around the 3 minute mark from what I can see, but
  ;; 10 minutes to be on the safe side. Just be patient, and the REPL will show up
  :repl-options {:timeout 600000}
  :profiles {:dev {:source-paths ["src" "dev"]
                   :resource-paths ["dev-resources" "resources"]
                   :dependencies [[midje "1.9.9"]
                                  [ring/ring-mock "0.4.0"]
                                  [http-kit "2.3.0"]
                                  [hashp "0.2.0"]
                                  [clj-commons/spyscope "0.1.48"]]
                   :injections [(require 'spyscope.core)]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-plantuml "0.1.22"]]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
