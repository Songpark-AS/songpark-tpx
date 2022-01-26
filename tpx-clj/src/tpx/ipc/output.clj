(ns tpx.ipc.output
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

;; When a regex is found matching a line it will not test the same line on the rest of the regexes that have not yet been tested.
;; TODO: Make it test/process all regexes regardless if it has found one regex to match the line

;; This map will be sorted by keyword alphabetically before being processed
(def regexes {:coredump #"INFO::COREDUMP - (.*)"
              :sip-registered #".*pjsua_acc.c  ....sip:[^:]+: registration success, status=200 \(OK\).*"
              ;; sip call
              :sip-call-buddy-list #"Buddy list:"
              :sip-call-choices #"Choices:"
              :sip-call-enter #"  <Enter>    Empty input \(or 'q'\) to cancel"
              ;; gains
              :gain-input-titles #"\| Parameters \+  Loopback  \+   Network  \|"
              :gain-input-volume-g #"\|  Volume_G\s+\+\s+(\d+)\s+\+\s+(\d+)\s+\|"
              :gain-input-volume-l #"\|  Volume_L\s+\+\s+(\d+)\s+\+\s+(\d+)\s+\|"
              :gain-input-volume-r #"\|  Volume_R\s+\+\s+(\d+)\s+\+\s+(\d+)\s+\|"
              :gain-input-global-gain #"Entered global gain"
              :gain-input-left-gain #"Entered left gain"
              :gain-input-right-gain #"Entered right gain"


              ;; sip-call-started

              :sip-connect #".*Conf connect: \d+ \-\-\> \d+.*"
              :sip-stream-established #".*Port \d+ \((.*)\) transmitting to port \d+ \((.*)\).*"

              ;; sip-call-stopped
              :sip-call-hangup #".*\!Call \d+ hanging up: code=\d+.*"
              :sip-call-status #"  \[(\w+)\] To: (sip:.*);.*"
              :log #".*(INFO|DEBUG|WARN|ERROR)(::)?(.*)"})


(def controller-steps ^{:doc "Last step in the regex steps above"}
  #{:coredump
    :sip-registered
    :sip-call-enter
    :gain-input-global-gain
    :gain-input-left-gain
    :gain-input-right-gain
    :sip-call-started
    :sip-call-stopped
    :log})

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
    (cond (set/subset? #{:coredump} current-set)
          [:coredump (into {} lines)]

          (set/subset? #{:sip-registered} current-set)
          [:sip-registered (into {} lines)]

          (set/subset? #{:sip-call-buddy-list
                         :sip-call-choices
                         :sip-call-enter} current-set)
          [:sip-call (into {} lines)]

          (set/subset? #{:sip-connect
                         :sip-stream-established} current-set)
          [:sip-call-started (into {} lines)]

          (set/subset? #{:sip-call-hangup
                         :sip-call-status} current-set)
          [:sip-call-stopped (into {} lines)]

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

          (set/subset? #{:log} current-set)
          [:log (into {} lines)]

          :else
          nil)))

(defn pre-process [happening data]
  (case happening
    :coredump (let [[_ data] (:coredump data)] data)
    :gain-input-global-gain (let [[_ loopback network] (:gain-input-volume-g data)]
                              {:loopback loopback
                               :network network})
    :gain-input-left-gain (let [[_ loopback network] (:gain-input-volume-l data)]
                            {:loopback loopback
                             :network network})
    :gain-input-right-gain (let [[_ loopback network] (:gain-input-volume-r data)]
                             {:loopback loopback
                              :network network})
    :sip-call-started (let [[_ from to] (:sip-stream-established data)]
                        {:from from
                         :to to})
    :sip-call-stopped (let [[_ status to] (:sip-call-status data)]
                        {:status status
                         :to to})
    :log (let [[_ level _ data] (:log data)]
           {:log/level (-> level str/lower-case keyword)
            :log/data data})
    nil))

(defn handle-output [context fns line]
  (log/debug :read-line line)
  (when-let [[found match] (reduce (fn [_ [k regex]]
                                     (if-let [match (process-line k line)]
                                       (reduced [k match])
                                       nil))
                                   nil (into (sorted-map) regexes))]
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
