(ns tpx.fx
  (:require [clojure.string :as str]
            [codax.core :as codax]
            [taoensso.timbre :as log]
            [tpx.database :refer [db]]
            [tpx.utils :refer [get-input-path]]))

(defn write-fx [input k v]
  (log/debug :write-fx {:input input
                        :k k
                        :v v})
  (let [path (get-input-path input k)]
    (codax/assoc-at! @db [path] v)))

(defn get-fx [input k]
  (let [path (get-input-path input k)]
    (codax/get-at! @db[path])))

(defn set-fxs [fxs]
  (log/debug ::set-fxs fxs)
  (let [merged-fx-values (dissoc (apply merge fxs) :fx/type)]
    (log/debug ::set-fxs-merged-maps merged-fx-values)
    (doseq [[k v] merged-fx-values]
      (codax/assoc-at! @db [k] v))))


(comment
  (write-fx "input1" :echo/delay-time 10)
  (get-fx "input" :gain)
  (get-input-path "input1" :gain)

  (get-fx "input1" :reverb/mix)
  (get-input-path "input1" :reverb/mix)

  (set-fxs [{:fx/type :fx/reverb, :fx.input1.reverb/mix 41, :fx.input1.reverb/damp 0, :fx.input1.reverb/room-size 0}
            {:fx/type :fx/amplify, :fx.input1.amplify/drive 14, :fx.input1.amplify/tone 42}
            {:fx/type :fx/equalizer, :fx.input1.equalizer/low 1, :fx.input1.equalizer/medium-low 2, :fx.input1.equalizer/medium-high 1, :fx.input1.equalizer/high 5}
            {:fx/type :fx/compressor, :fx.input1.compressor/threshold -16, :fx.input1.compressor/ratio 12, :fx.input1.compressor/attack 39, :fx.input1.compressor/release 2546}])
  )
