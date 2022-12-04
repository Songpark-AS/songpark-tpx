(ns tpx.ipc.output
  (:require [cheshire.core :as json]
            [clojure.set :as set]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [tpx.ipc.handler :as ipc.handler]))

(defn handle-output [context line]
  (if (str/starts-with? line "{\"tpx_msg\"")
    (try
      (let [data (as-> (json/parse-string line true) $d
                   (assoc $d
                          :tpx/msg (as-> (get $d :tpx_msg) $v
                                     (if (string? $v)
                                       (-> $v
                                           (str/replace #"__" "/")
                                           (keyword))
                                       $v)))
                   (dissoc $d :tpx_msg))
            msg-type (:tpx/msg data)]
        (ipc.handler/handler data context))
      (catch Exception e
        (log/error "Tried parsing output from BP"
                   {:data (ex-data e)
                    :message (ex-message e)
                    :line line})))
    (log/debug :read-line/raw line)))
