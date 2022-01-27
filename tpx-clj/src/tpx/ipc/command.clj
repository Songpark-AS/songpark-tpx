(ns tpx.ipc.command
  (:require [tpx.data :as data]
            [tpx.ipc.serial :refer [send-command]]
            [taoensso.timbre :as log]))

(defn global-volume [value]
  (do (send-command "pc" "")
      (send-command "vol" value)))

(defn local-volume [value]
  (do (send-command "pc" "")
      (send-command "vll" value)
      (send-command "pc" "")
      (send-command "vlr" value)))

(defn network-volume [value]
  (do (send-command "pc" "")
      (send-command "netvoll" value)

      (send-command "pc" "")
      (send-command "netvolr" value)))

(defn set-playout-delay [value]
  (send-command "pd" "")
  (Thread/sleep 200)
  (send-command "" value))


(defn- call-via-sip [sip]
  (log/debug ::m)
  (send-command "m" "")
  (Thread/sleep 500)
  (log/debug ::sip)
  (send-command sip ""))

(defn- hangup-via-sip [sip]
  ;; this should take the sip as a command
  ;; but the h command for SIP/BP does not appear to take it
  (send-command "h" ""))

(defn hangup-all []
  ;; hangup all calls
  (send-command "ha" ""))

(defn path-reset []
  (send-command "pr" ""))

(defn get-local-ip []
  (send-command "pc" "")
  (send-command "getlip" ""))

(defn get-gateway-ip []
  (send-command "pc" "")
  (send-command "getgip" ""))

(defn get-netmask-ip []
  (send-command "pc" "")
  (send-command "getmask" ""))

(defn start-coredump []
  (send-command "pc" "")
  (Thread/sleep 200)
  (send-command "monitor" ""))

(defn stop-coredump []
  (send-command "pc" "")
  (Thread/sleep 200)
  (send-command "halt" ""))

(defn- get-call-order [tp-id join-order sips]
  (let [indexed-join-order (map vector join-order (range))
        starting-position (reduce (fn [_ [id idx]]
                                    (if (= tp-id id)
                                      (reduced idx)
                                      nil))
                                  nil indexed-join-order)
        sips-in-order (map #(get sips %) join-order)
        [_ sips] (split-at (inc starting-position) sips-in-order)]
    sips))

(defn jam-start [join-order sips]
  (log/debug :jam-start {:join-order join-order
                         :sips sips})
  ;; (hangup-all)
  ;; Sleep for 200ms to make sure the calls are all ended
  (Thread/sleep 200)
  (let [tp-id (data/get-tp-id)
        other-sips (dissoc sips tp-id)
        sips-call-order (get-call-order tp-id join-order sips)]
    (log/debug :sips-call-order (mapv identity sips-call-order))
    (doseq [sip sips-call-order]
      (call-via-sip sip))))

(defn jam-stop [join-order sips]
  (log/debug :jam-stop {:join-order join-order
                        :sips sips})
  (let [tp-id (data/get-tp-id)
        other-sips (dissoc sips tp-id)
        sips-hangup-order (get-call-order tp-id join-order sips)]
    (log/debug :sips-hangup-order (mapv identity sips-hangup-order))
    (doseq [sip sips-hangup-order]
      (hangup-via-sip sip))))

(comment
  ;; Beatles
  (call-via-sip "sip:9100@voip1.inonit.no")
  (hangup-via-sip "sip:9100@voip1.inonit.no")

  ;; Elvis Presely
  (call-via-sip "sip:9102@voip1.inonit.no")
  (hangup-via-sip "sip:9102@voip1.inonit.no")

  ;; Jimi Hendrix
  (call-via-sip "sip:9104@voip1.inonit.no")
  (hangup-via-sip "sip:9104@voip1.inonit.no")

  ;; Adele
  (call-via-sip "sip:9106@voip1.inonit.no")
  (hangup-via-sip "sip:9106@voip1.inonit.no")

  ;; Madonna
  (call-via-sip "sip:9108@voip1.inonit.no")
  (hangup-via-sip "sip:9108@voip1.inonit.no")

  (send-command "" "")
  (send-command "ha" "")
  (set-playout-delay 30)

  ;; check active calls
  (do (send-command "m" "")
      (Thread/sleep 200)
      (send-command "q" ""))

  (do (send-command "pd" "")
      (Thread/sleep 500)
      (send-command "12" "")
      (Thread/sleep 200)
      (send-command "pd" "")
      )

  (playout-delay "11")

  (do (send-command "pc" "")
      (send-command "vll" "10")
      (send-command "pc" "")
      (send-command "vlr" "10"))

  (do (send-command "pc" "")
      (send-command "vll" "10"))

  (do (send-command "pc" "")
      (send-command "vol" "50"))

  (do (send-command "pc" "")
      (send-command "vlr" "10"))

  (do (send-command "pc" "")
      (send-command "netvoll" "10")

      (send-command "pc" "")
      (send-command "netvolr" "10"))
)
