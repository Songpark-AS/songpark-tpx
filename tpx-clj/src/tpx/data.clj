(ns tpx.data
  "Dirty handling of data. Gather in one place."
  (:require [tpx.utils :refer [get-apt-package-installed-version]]))

(defonce ^:private jam-id* (atom nil))
(defonce ^:private jam-session* (atom nil))
(defonce ^:private tp-id* (atom nil))
(defonce ^:private apt-version* (atom (get-apt-package-installed-version "teleporter-fw")))

;; TP ID (UUID)
(defn set-tp-id! [tp-id]
  (reset! tp-id* tp-id))

(defn clear-tp-id! []
  (reset! tp-id* nil))

(defn get-tp-id []
  @tp-id*)

(defn get-apt-version []
  (str @apt-version*))

(defn same-tp? [tp-id]
  (and (some? @tp-id*)
       (= (str @tp-id*) (str tp-id))))
