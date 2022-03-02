(ns tpx.ipc.output
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

;; When a regex is found matching a line it will not test the same line on the rest of the regexes that have not yet been tested.
;; TODO: Make it test/process all regexes regardless if it has found one regex to match the line

(def regexes (sorted-map
              :jam/coredump #"INFO::COREDUMP - (.*)"
              :sip/register #".*pjsua_acc.c  ....sip:[^:]+: registration success, status=200 \(OK\).*"
              ;; sip menu. required before we make a call
              :sip/menu-buddy-list #"Buddy list:"
              :sip/menu-choices #"Choices:"
              :sip/menu-enter #"  <Enter>    Empty input \(or 'q'\) to cancel"
              ;; volume
              :gain-input-titles #"\| Parameters \+  Loopback  \+   Network  \|"
              :gain-input-volume-g #"\|  Volume_G\s+\+\s+(\d+)\s+\+\s+(\d+)\s+\|"
              :gain-input-volume-l #"\|  Volume_L\s+\+\s+(\d+)\s+\+\s+(\d+)\s+\|"
              :gain-input-volume-r #"\|  Volume_R\s+\+\s+(\d+)\s+\+\s+(\d+)\s+\|"
              :gain-input-global-gain #"Entered global gain"
              :gain-input-left-gain #"Entered left gain"
              :gain-input-right-gain #"Entered right gain"

              :sip/making-call #".*pjsua_call\.c \!Making call with acc.*"
              :sip/calling #".*Call \d+ state changed to CALLING.*"
              :sip/incoming-call #".*INCOMING CALL NO SYNC.*"
              :sip/in-call #".*pjsua_app\.c  ...Call \d+ state changed to CONFIRMED.*"
              :sip/hangup #".*\!Call \d+ hanging up: code=\d+.*"
              :sip/call-ended #".*Call \d+ is DISCONNECTED.*"
              :sip/error-making-call #".*Error making call: Too many objects of the specified type \(([A-Z_]+)\) \[status=(\d+)\]"
              :sip/error-dialog-mutex #".*Timed-out trying to acquire dialog mutex \(possibly system has deadlocked\) in pjsua_call_hangup.*"

              :stream/broken #".*Media stream broken clear all calls.*"
              :stream/syncing-calling-device #".*Enable time sync udp tx.*"
              :stream/syncing-called-device #".*-----------Entering sync wait loop------------.*"
              :stream/sync-failed-calling-device #".*Error initializing hardware sync.*"
              :stream/sync-failed-called-device #".*SYNC FAILED TIMEOUT waiting.*"
              ;;:stream/streaming #""
              :stream/stopped #".*stop_hw_streaming\(\):Stream tx stopped status was.*"

              ;; ;; network-config
              ;; :local-ip #"Local Ip Address:.*"
              ;; :gateway-ip #"Gateway Ip Address:.*"
              ;; :netmask-ip #"Mask Ip Address:.*"
              ))


(def controller-steps ^{:doc "Last step in the regex steps above"}
  #{:jam/coredump

    :sip/register
    :sip/menu-enter
    :sip/making-call
    :sip/calling
    :sip/incoming-call
    :sip/in-call
    :sip/hangup
    :sip/call-ended
    :sip/error-making-call
    :sip/error-dialog-mutex

    :stream/broken
    :stream/syncing-calling-device
    :stream/syncing-called-device
    :stream/sync-failed-calling-device
    :stream/sync-failed-called-device
    :stream/streaming
    :stream/stopped

    :gain-input-global-gain
    :gain-input-left-gain
    :gain-input-right-gain})

(defonce lines (atom []))

(defn process-line
  "Process the line and see if it matches any regex"
  [k line]
  (if-let [regex (get regexes k)]
    (re-matches regex line)))

(defn- reset-lines?
  "Do we need to reset lines? If we find last step in a regex sequence without a matching sequence in lines, we need to reset."
  [found lines]
  (let [current-step (-> @lines last first)]
    (when (controller-steps current-step)
      (reset! lines []))))

(defn- found-happening
  "Check if we have found something"
  [lines]
  (let [lines @lines
        current-set (->> lines
                         (map first)
                         (into #{}))]
    (cond (set/subset? #{:jam/coredump} current-set)
          [:jam/coredump (into {} lines)]

          (set/subset? #{:sip/register} current-set)
          [:sip/register (into {} lines)]

          (set/subset? #{:sip/menu-buddy-list
                         :sip/menu-choices
                         :sip/menu-enter} current-set)
          [:sip/menu (into {} lines)]

          (set/subset? #{:sip/making-call} current-set)
          [:sip/making-call (into {} lines)]

          (set/subset? #{:sip/calling} current-set)
          [:sip/calling (into {} lines)]

          (set/subset? #{:sip/error-making-call} current-set)
          [:sip/error-making-call (into {} lines)]

          (set/subset? #{:sip/error-dialog-mutex} current-set)
          [:sip/error-dialog-mutex (into {} lines)]

          (set/subset? #{:sip/incoming-call} current-set)
          [:sip/incoming-call (into {} lines)]

          (set/subset? #{:sip/in-call} current-set)
          [:sip/in-call (into {} lines)]

          (set/subset? #{:sip/hangup} current-set)
          [:sip/hangup (into {} lines)]

          (set/subset? #{:sip/call-ended} current-set)
          [:sip/call-ended (into {} lines)]

          (set/subset? #{:stream/broken} current-set)
          [:stream/broken (into {} lines)]

          (set/subset? #{:stream/syncing-calling-device} current-set)
          [:stream/syncing (into {} lines)]

          (set/subset? #{:stream/syncing-called-device} current-set)
          [:stream/syncing (into {} lines)]

          (set/subset? #{:stream/sync-failed-calling-device} current-set)
          [:stream/sync-failed (into {} lines)]

          (set/subset? #{:stream/sync-failed-called-device} current-set)
          [:stream/sync-failed (into {} lines)]

          (set/subset? #{:stream/streaming} current-set)
          [:stream/streaming (into {} lines)]

          (set/subset? #{:stream/stopped} current-set)
          [:stream/stopped (into {} lines)]

          (set/subset? #{:gain-input-titles
                         :gain-input-volume-g
                         :gain-input-global-gain}
                       current-set)
          [:gain-input-global-gain (into {} lines)]
          
          (set/subset? #{:gain-input-titles
                         :gain-input-volume-l
                         :gain-input-left-gain}
                       current-set)
          [:gain-input-left-gain (into {} lines)]

          (set/subset? #{:gain-input-titles
                         :gain-input-volume-r
                         :gain-input-right-gain}
                       current-set)
          [:gain-input-right-gain (into {} lines)]

          :else
          nil)))

(defn pre-process [happening data]
  (case happening
    :jam/coredump (let [[_ data] (:jam/coredump data)] data)

    :sip/register true
    :sip/menu-enter true
    :sip/making-call true
    :sip/calling true
    :sip/incoming-call true
    :sip/in-call true
    :sip/hangup true
    :sip/call-ended true
    :sip/error-making-call (let [[_ error-type error-code] (:sip/error-making-call data)]
                             {:error/type error-type
                              :error/code error-code})
    :sip/error-dialog-mutex {:error/type "DIALOG_MUTEX"
                             :error/code -1}

    :stream/broken true
    :stream/syncing true
    :stream/sync-failed true
    :stream/streaming true
    :stream/stopped true
    
    :gain-input-global-gain (let [[_ loopback network] (:gain-input-volume-g data)]
                              {:loopback loopback
                               :network network})
    :gain-input-left-gain (let [[_ loopback network] (:gain-input-volume-l data)]
                            {:loopback loopback
                             :network network})
    :gain-input-right-gain (let [[_ loopback network] (:gain-input-volume-r data)]
                             {:loopback loopback
                              :network network})
    
    :log (let [[_ level _ data] (:log data)]
           {:log/level (-> level str/lower-case keyword)
            :log/data data})
    :local-ip (let [ip (:local-ip data)]
                {:ip (last (clojure.string/split ip #":"))})
    nil))

(defn handle-output [context fns line]
  (log/debug :read-line line)
  (when-let [[found match] (reduce (fn [_ [k regex]]
                                     (if-let [match (process-line k line)]
                                       (reduced [k match])
                                       nil))
                                   nil regexes)]
    (swap! lines conj [found match])
    ;; (log/debug {:found found
    ;;             :match match})
    (let [[happening data] (found-happening lines)]
      (when happening
        ;; (log/debug happening)
        (if-let [f (get fns happening)]
          (f (pre-process happening data) context)
          (log/warn found "has no corresponding fn in " (str fns)))
        (reset! lines []))
      (do ;; (log/debug :reset-lines? found @lines)
          (reset-lines? found lines)))))
