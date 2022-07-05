(ns tpx.fx
  (:require [clojure.string :as str]
            [codax.core :as codax]
            [taoensso.timbre :as log]
            [tpx.database :refer [db]]))

(defn get-path [input k]
  (let [ns* (namespace k)
        n* (name k)]
    (keyword (str/join "." (flatten (remove str/blank? ["fx" input ns*])))
             n*)))

(defn write-fx [input k v]
  (log/debug :write-fx {:input input
                        :k k
                        :v v})
  (let [path (get-path input k)]
    (codax/assoc-at! @db [path] v)))

(defn get-fx [input k]
  (let [path (get-path input k)]
    (codax/get-at! @db[path])))


(comment
  (write-fx "input1" :echo/delay-time 10)
  (get-fx "input" :gain)
  (get-path "input1" :gain)
  )
