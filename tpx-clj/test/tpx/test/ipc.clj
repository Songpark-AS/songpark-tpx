(ns tpx.test.ipc
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [tpx.ipc.output :refer [process-line
                                    handle-output
                                    lines]]))


(defn- get-lines [file-name]
  (line-seq (io/reader (io/resource (str "serial-output/" file-name)))))

(fact
 "process-line"
 (fact
  "sip-has-started"
  (let [line "10:00:05.954    pjsua_acc.c  ....sip:9114@voip1.inonit.no: registration success, status=200 (OK), will re-register in 300 seconds"]
    (some? (process-line :sip-has-started line))
    => true))
 (fact
  "sip-call"
  (let [line1 "(You currently have 0 calls)"
        line2 "Buddy list:"
        line3 "Choices:"
        line4 "  <Enter>    Empty input (or 'q') to cancel"]
    {:sip-call-buddy-list (some? (process-line :sip-call-buddy-list line2))
     :sip-call-choices (some? (process-line :sip-call-choices line3))
     :sip-call-enter (some? (process-line :sip-call-enter line4))}
    => {:sip-call-buddy-list true
        :sip-call-choices true
        :sip-call-enter true}))
 (fact
  "gain-input-volume-g"
  (let [line1 "|  Volume_G  +       5    +       5    |"
        result (process-line :gain-input-volume-g line1)]
    {:match? (some? result)
     :loopback (second result)
     :network (last result)}
    => {:match? true
        :loopback "5"
        :network "5"}))

 (fact
  "gain-input-volume-l"
  (let [line1 "|  Volume_L  +      30    +      30    |"
        result (process-line :gain-input-volume-l line1)]
    {:match? (some? result)
     :loopback (second result)
     :network (last result)}
    => {:match? true
        :loopback "30"
        :network "30"})))

(defn- reset-lines!
  "Reset the lines in tpx.ipc.output for each test"
  []
  (reset! lines []))

(fact
 "handle-output"
 (let [lines-startup (get-lines "startup.txt")
       lines-sip-call (get-lines "sip-call.txt")
       lines-gain-faulty (get-lines "gain-input-faulty.txt")
       lines-gain-input-global-gain (get-lines "gain-input-global-gain.txt")
       lines-gain-input-left-gain (get-lines "gain-input-left-gain.txt")
       context (atom {:sip-has-started false
                      :sip-call false
                      :gain-input-global-gain nil})
       fns {:sip-has-started (fn [data context]
                               (swap! context assoc :sip-has-started true))
            :sip-call (fn [data context]
                        (swap! context assoc :sip-call true))
            :gain-input-global-gain (fn [data context]
                                      (swap! context assoc :gain-input-global-gain data))
            :gain-input-left-gain (fn [data context]
                                    (swap! context assoc :gain-input-left-gain data))}]
   
   
   (fact
    "sip-has-started"
    (reset-lines!)
    (doseq [line lines-startup]
      (handle-output context fns line))
    (:sip-has-started @context) => true)

   
   (fact
    "sip-call"
    (reset-lines!)
    (doseq [line lines-sip-call]
      (handle-output context fns line))
    (:sip-call @context)
    => true)

   (fact
    "gain-input-global-gain"
    (reset-lines!)
    (doseq [line lines-gain-input-global-gain]
      (handle-output context fns line))
    (:gain-input-global-gain @context) => {:loopback "5"
                                           :network "5"})
   (fact
    "gain-input-left-gain"
    (reset-lines!)
    (doseq [line lines-gain-input-left-gain]
      (handle-output context fns line))
    (:gain-input-left-gain @context) => {:loopback "30"
                                         :network "30"})
   (fact
    "faulty gain-input-global-gain"
    (reset-lines!)
    (swap! context assoc :gain-input-global-gain nil)
    (doseq [line lines-gain-faulty]
      (handle-output context fns line))
    (doseq [line lines-gain-input-global-gain]
      (handle-output context fns line))
    (:gain-input-global-gain @context) => {:loopback "5"
                                           :network "5"})))
