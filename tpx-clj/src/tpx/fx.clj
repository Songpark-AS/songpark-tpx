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


(comment
  (write-fx "input1" :echo/delay-time 10)
  (get-fx "input" :gain)
  (get-input-path "input1" :gain)
  )
