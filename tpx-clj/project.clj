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
                 [com.fzakaria/slf4j-timbre "0.3.21"]
                 [raven-clj "1.6.0"]
                 ;; redis support
                 [com.taoensso/carmine "3.1.0"]
                 ;; structure
                 [com.stuartsierra/component "1.0.0"]
                 ;; mqtt implementation
                 [clojurewerkz/machine_head "1.0.0"]
                 [com.cognitect/transit-clj "1.0.324"]
                 ;; songpark's common (will also contain an mqtt implementation later)
                 [songpark/common "0.1.1-SNAPSHOT"]
                 [clojure-interop/java.net "1.0.5"]]
  :main ^:skip-aot tpx.core
  :target-path "target/%s"
  :test-paths ["test"]
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
