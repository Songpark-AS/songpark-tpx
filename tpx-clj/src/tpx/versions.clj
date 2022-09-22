(ns tpx.versions
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [tpx.config :refer [config]]
            [taoensso.timbre :as log]))

(defonce data (atom {}))

(defn get-tpx-sha []
  (try
    (slurp (io/resource "VERSION.git"))
    (catch Exception _
      "DEV")))

(defn get-tpx-version []
  (try
    (slurp (io/resource "VERSION"))
    (catch Exception _
      "DEV")))

(defn get-bp-version []
  (:bp-version @data "Unknown"))

(defn get-fpga-version []
  (:fpga-version @data "Unknown"))


(defn load-versions []
  (let [path (get-in config [:versions :path] "connect.versions")]
    (try
      (if (fs/exists? path)
        (let [saved-data (edn/read-string (slurp path))]
          (reset! data saved-data))
        (log/info "No previously saved versions found" {:path path}))
      (catch Exception e
        (log/error "Unable to load saved versions" {:path path
                                                    :exception e
                                                    :message (ex-message e)
                                                    :data (ex-data e)})))))

(defn save-versions [versions]
  (reset! data versions)
  (let [path (get-in config [:versions :path] "connect.versions")]
    (try
      (spit path (pr-str versions))
      (catch Exception e
        (log/error "Unable to save versions" {:path path
                                              :versions versions
                                              :exception e
                                              :message (ex-message e)
                                              :data (ex-data e)})))))

(defn get-versions []
  {:teleporter/tpx-version (get-tpx-version)
   :teleporter/tpx-sha (get-tpx-sha)
   :teleporter/bp-version (get-bp-version)
   :teleporter/fpga-version (get-fpga-version)})
