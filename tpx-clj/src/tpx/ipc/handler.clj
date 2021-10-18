(ns tpx.ipc.handler
  (:require [taoensso.timbre :as log]))


(defn handle-sip-has-started [data context]
  (log/debug :handle-sip-has-started data))

(defn handle-sip-call [data context]
  (log/debug :handle-sip-call data))

(defn handle-gain-input-global-gain [data context]
  (log/debug :gain-input-global-gain data))

(defn handle-gain-input-left-gain [data context]
  (log/debug :gain-input-left-gain data))

(defn handle-gain-input-right-gain [data context]
  (log/debug :gain-input-right-gain data))

(defn handle-sip-call-started [data context]
  (log/debug :handle-sip-call-started data))

(defn handle-sip-call-stopped [data context]
  (log/debug :handle-sip-call-stopped data))
