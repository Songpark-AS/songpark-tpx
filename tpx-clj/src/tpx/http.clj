(ns tpx.http
  (:require [taoensso.timbre :as log]
            [songpark.common.communication :refer [POST]]))

;;! Daniel's stuff

;; This will be handled via communication in common, such
;; that we only need to use the api endpoint part of the
;; url in our request fns
(defonce url "http://localhost:3000/api/teleporter")

;; this will not be here, just for initial testing
(defonce state (atom {}))


(defn on-error [response]
  (log/error response))

(defn on-success [response]
  (log/debug response))

(defn off [bits]
  (POST url {:teleporter/bits bits
             :teleporter/on false
             :teleporter/available false}
    on-success
    on-error))

(defn on-and-unavailable [bits]
  (POST url {:teleporter/bits bits
             :teleporter/on true
             :teleporter/available false}
    on-success
    on-error))

(defn available [bits]
  (POST url {:teleporter/bits bits
             :teleporter/on true
             :teleporter/available true}
    on-success
    on-error))


(comment

  ;; turn on teleporter
  @(on-and-unavailable "0000")

  ;; make teleporter available
  @(available "0000")

  ;; turn ff teleporter
  @(off "0000"))

;;! --- Sindre's and my stuff ---

;! --- Netcode ---
(def global-conn-map (atom {}))

