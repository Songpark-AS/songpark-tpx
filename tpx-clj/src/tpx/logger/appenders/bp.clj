(ns tpx.logger.appenders.bp
  (:require [tpx.ipc.serial :refer [send-command]]))


(defn bp-appender
  "Returns an appender for the Bridge Program"
  [& _]
  {:enabled?   true
   :async?     false
   :min-level  :info
   :rate-limit nil
   :output-fn  :inherit
   :fn
   (fn [data]
     (let [{:keys [output_]} data
           output-str (force output_)]
       (try
         (send-command "log_tpx" output-str)
         (catch Exception e
           (println "Caught an exception in bp-appender" {:data (ex-data e)
                                                          :message (ex-message e)
                                                          :exception e})
           nil))))})
