(ns tpx.utils
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [taoensso.timbre :as log]
            [tpx.config :refer [config]]))

(defn get-input-path [input k]
  (let [ns* (namespace k)
        n* (name k)]
    (keyword (str/join "." (flatten (remove str/blank? ["fx" input ns*])))
             n*)))

(defn scale-value
  "Linearly transforms x from range input-range to output-range where:

   input-range - a vector like [min max]
   output-range - a vector like [min max]

   "
  [x input-range output-range]
  (let [[a b] input-range
        [c d] output-range]
    (+
     (-> (- x a)
         (/ (- b a))
         - ; negate the result
         inc
         (* c))
     (-> (- x a)
         (/ (- b a))
         (* d)))))

(defn get-apt-package-installed-version
  "Returns the version of an installed apt package.
  Returns nil if the package is not installed"
  [package-name]
  (let [apt-show-command (str "apt-cache policy " package-name)
        apt-string (:out (sh "bash" "-c" apt-show-command))
        version-line (or (first (filter (fn [line] (re-matches #"  Installed.*" line))
                                        (str/split apt-string #"\n")))
                         "(none)")
        version (last (str/split version-line #": "))]
    (if-not (= version "(none)")
      version
      nil)))

(defn upgrading-flag?
  "Checks for the upgrading_flag file"
  []
  (let [data-dir (get-in config [:os :data-dir])]
    (.exists (io/file (str data-dir "upgrading_flag")))))

(defn delete-upgrading-flag []
  (log/debug ::delete-upgrading-flag "deleting flag")
  (let [data-dir (get-in config [:os :data-dir])]
    (io/delete-file (str data-dir "upgrading_flag"))))

(defn upgrade []
  (log/debug ::upgrade "Upgrading teleporter firmware")
  (let [data-dir (get-in config [:os :data-dir])]
    (sh "bash" "-c" (str "./upgrader.sh " data-dir))))


(defn reboot []
  (log/info "Rebooting machine")
  (sh "reboot"))

(defn get-platform-url [path]
  (str (get-in config [:ipc :platform]) path))
