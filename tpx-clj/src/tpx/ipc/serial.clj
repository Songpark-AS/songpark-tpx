(ns tpx.ipc.serial
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            ;; [serial.core :as serial]
            [taoensso.timbre :as log]
            [tpx.ipc.output :refer [handle-output]]
            [tpx.ipc.handler :as ipc.handler]))

;; This is set on the zedboard, by a systemd
;; service running socat, such that we do not have to deal
;; with arbitrary pseudo terminai numbering,
;; even if it might not pose a problem on the
;; zedboards, given no other services appear
;; to be using /dev/ptmx
(defonce config (atom {:pts "/tmp/ttyTPX"}))

(defonce fake? (atom false))


;; TODO: examine possibility to have open-handler? inside the handler function
;; and let it reset the atom to false itself. The observed behaviour of the BP
;; and clj-serial is that the stream is closed every now and then, and then a new one
;; is created. This means to be fixed.
(def ^:private open-handler? (atom true))

(defn handler "Read input stream from serial"
  [context fns]
  (reset! open-handler? true)
  (fn [io-stream]
    (log/debug "Ready to read stream from BP")
    (future
      (with-open [reader (io/reader io-stream)]
        (while @open-handler?
          (if-let [line (.readLine reader)]
            (try
              (handle-output context fns line)
              (catch Exception e
                (log/warn {:msg (ex-message e)
                           :data (ex-data e)})))
            (log/debug ::unable-to-read-line)))))))

(defn connect-to-port
  ([context fns]
   (connect-to-port context fns (:pts @config)))
  ([context fns pts]
   (log/debug ::connect-to-port "connecting...")
   (let [port (serial/open pts)
         my-handler (handler context fns)        ]
     (swap! config assoc :port port)
     ;; TEMP
     ;; (serial/listen! port my-handler false)
     )))

(defn disconnect
  ([]
   (disconnect (:port @config)))
  ([port]
   (log/debug ::disconnect "bye...")
   (try
     ;; TEMP
     ;; (serial/unlisten! port)
     ;; (serial/close! port)
     (swap! config dissoc :port)
     (reset! open-handler? false)
     (catch Exception e
       (log/error ::disconnect (ex-message e))
       (log/error ::disconnect (ex-data e))))))


(defn- str->ba [s]
  (->> s
       (map (comp byte int))
       byte-array))


(defn send-command
  ([cmd v]
   (send-command (:port @config) cmd v))
  ([port cmd v]
   (log/debug ::send-command {:cmd cmd
                              :v v})
   (when-not @fake?
     ;; TEMP
     ;; (serial/write port (str->ba (str cmd " " v "\n")))
       )))


(comment

  (log/debug @config)

  (connect-to-port {:mycontext true} {:sip-call-started #'ipc.handler/handle-sip-call-started
                                      :sip-call-stopped #'ipc.handler/handle-sip-call-stopped
                                      :sip-registered #'ipc.handler/handle-sip-registered
                                      :sip-call #'ipc.handler/handle-sip-call
                                      :gain-input-global-gain #'ipc.handler/handle-gain-input-global-gain
                                      :gain-input-left-gain #'ipc.handler/handle-gain-input-left-gain
                                      :gain-input-right-gain #'ipc.handler/handle-gain-input-right-gain} (:pts @config))


  (send-command (:port @config) "vol" 100)
  (send-command (:port @config) "m" "")
  (send-command (:port @config) "\n" "")

  (send-command (:port @config) "sip:9115@voip1.inonit.no" "")
  (send-command (:port @config) "h" "")
  (send-command (:port @config) "h" "sip:9115@voip1.inonit.no")

  (send-command (:port @config) "rr" "")

  (serial/open (:pts @config))

  (disconnect (:port @config))

  (swap! config assoc :pts "/dev/ttys013")

  )
