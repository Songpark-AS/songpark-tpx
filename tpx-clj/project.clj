(defproject tpx "0.1.1-SNAPSHOT"
  :description "Songpark's Teleporter's communications' logic"
  :url "http://example.com/FIXME"
  :license {:name ""
            :url ""}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clojurewerkz/machine_head "1.0.0"]
                 [songpark/common "0.1.1-SNAPSHOT"]
                 
                 ]
  :main ^:skip-aot tpx.core
  :target-path "target/%s"
  :test-paths ["test"]
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
