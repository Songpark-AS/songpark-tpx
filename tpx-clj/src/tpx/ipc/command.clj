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
  (call-via-sip "sip:9115@voip1.inonit.no")
  (hangup-via-sip "sip:9115@voip1.inonit.no")

  (send-command "" "")

  ;; check active calls
  (do (send-command "m" "")
      (Thread/sleep 200)
      (send-command "q" ""))

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
  )
