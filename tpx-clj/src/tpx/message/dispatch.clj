(ns tpx.message.dispatch
  (:require [tpx.message.dispatch.platform]
            [tpx.message.dispatch.teleporter]
            [tpx.message.dispatch.jam]
            [tpx.message.dispatch.interface :as interface]))


(defn handler [msg]
  (interface/dispatch msg))
