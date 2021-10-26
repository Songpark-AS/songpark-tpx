(ns tpx.network.middleware)

(defrecord TPXData [])

;; hide the data when printing to stdout or anywhere else
;; this is primarily to avoid having sensitive data like credentials
;; being written to logs
(defmethod clojure.core/print-method TPXData [data ^java.io.Writer writer]
  (.write writer "#<TPXData>"))

(defn inject-data [handler data]
  (let [data (map->TPXData data)]
    (fn [request]
      (handler (assoc request :data data )))))
