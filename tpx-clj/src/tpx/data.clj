(ns tpx.data
  "Dirty handling of data. Gather in one place."
  (:require [tpx.pairing :as pairing]
            [tpx.utils :refer [get-apt-package-installed-version]]
            [taoensso.timbre :as log]))

(defonce ^:private jam-id* (atom nil))
(defonce ^:private jam-session* (atom nil))
(defonce ^:private tp-id* (atom nil))
(defonce ^:private user-id* (atom nil))
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

(defn allowed? [mqtt-message]
  (let [{tp-id :teleporter/id
         user-id :auth.user/id} mqtt-message
        allow? (and ;; (uuid? @tp-id*)
                    ;; (number? @user-id*)
                    ;; (uuid? tp-id)
                    ;; (number? user-id)
                    ;; (= @tp-id* tp-id)
                    ;; (= @user-id* user-id)
                    (pairing/paired?))]
    (when (false? allow?)
      (log/info "Unauthorized access to the Teleporter"
                {:teleporter/id @tp-id*
                 :auth.user/id @user-id*
                 :paired? (pairing/paired?)
                 :mqtt-message {:teleporter/id tp-id
                                :auth.user/id user-id
                                :message/type (:message/type mqtt-message)
                                :message/id (:message/id mqtt-message)}}))
    allow?))

(defn set-user-id! [user-id]
  (reset! user-id* user-id))

(defn clear-user-id! []
  (reset! user-id* nil))

(defn get-user-id []
  @user-id*)
