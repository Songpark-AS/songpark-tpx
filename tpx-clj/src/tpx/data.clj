(ns tpx.data
  (:require [tpx.utils :refer [get-apt-package-installed-version]]))

(defonce ^:private jam-id* (atom nil))
(defonce ^:private jam-session* (atom nil))
(defonce ^:private tp-id* (atom nil))
(defonce ^:private apt-version* (atom (get-apt-package-installed-version "teleporter-fw")))

;; Jam ID (UUID)
(defn set-jam-id! [jam-id]
  (reset! jam-id* jam-id))

(defn clear-jam-id! []
  (reset! jam-id* nil))

(defn get-jam-id []
  @jam-id*)

(defn get-jam-teleporters []
  (str @jam-id* "/teleporters"))

(defn get-jam []
  (str @jam-id* "/jam"))

(defn set-jam-session! [jam-session]
  (reset! jam-session* jam-session))

(defn clear-jam-session! []
  (reset! jam-session* nil))


;; TP ID (UUID)
(defn set-tp-id! [tp-id]
  (reset! tp-id* tp-id))

(defn clear-tp-id! []
  (reset! tp-id* nil))

(defn get-tp-id []
  @tp-id*)

(defn get-apt-version []
  (str @apt-version*))

(defn get-tp-log-topic []
  (str @tp-id* "/log"))

(defn get-tp-coredump-topic []
  (str @tp-id* "/coredump"))

(defn get-tp-report-net-config-topic []
  (str @tp-id* "/net-config-report"))

(defn get-teleporter-topic []
  (str @tp-id* "/teleporter"))

(defn get-app-topic []
  (str @tp-id* "/app"))


(defn same-tp? [tp-id]
  (and (some? @tp-id*)
       (= (str @tp-id*) (str tp-id))))
