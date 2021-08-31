(defproject tpx "0.1.0-SNAPSHOT"
  :description "Songpark's Teleporter's communications' logic"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clojurewerkz/machine_head "1.0.0"]
                 [songpark/common "0.1.1-SNAPSHOT"]]
  :main ^:skip-aot tpx-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
