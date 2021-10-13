(ns tpx.ipc.serial
  (:require [taoensso.timbre :as log]
            [serial.core :as serial]
            [clojure.java.io :as io]))

(def commands
  #{:volume/g :volume/l :volume/r
    :filer/b :filter/l :filter/h})

;; This is set on the zedboard, by a systemd
;; service running socat, such that we do not have to deal
;; with arbitrary pseudo terminai numbering,
;; even if it might not pose a problem on the
;; zedboards, given no other services appear
;; to be using /dev/ptmx
(defonce config (atom {:pts "/tmp/ttyTPX"}))

(defn process [s]
  (future (log/debug ::process (read-string s))))

(defn handler [io-stream]
  "Read input stream from serial"
  (with-open [reader (io/reader io-stream)]
    (if-let [line (.readLine reader)]
      (try
        (process line)
        (catch Exception e
          (log/warn (ex-message e)))
        (finally
          (.close reader))))))

(defn connect-to-port [pts]
  (log/debug ::connect-to-port "connecting...")
  (let [port (serial/open pts)]
    (swap! config assoc :port port)
    (serial/listen! port handler false)))

(defn disconnect [port]
  (log/debug ::disconnect "bye...")
  (try
    (serial/unlisten! port)
    (serial/close! port)
    (swap! config dissoc :port)
    (catch Exception e
      (log/error ::disconnect (ex-message))
      (log/error ::disconnect (ex-data)))))


(defn- str->ba [s]
  (->> s
       (map (comp byte int))
       byte-array))


(defn send-command [port cmd v]
  (serial/write port (str->ba (str cmd " " v "\n"))))


(comment

  (log/debug @config)  
  
  (connect-to-port (:pts @config))

  (send-command (:port @config) "vol" 100)

  (disconnect (:port @config))  

  )

