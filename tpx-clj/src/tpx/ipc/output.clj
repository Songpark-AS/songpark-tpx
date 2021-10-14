(ns tpx.ipc.output
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(def regexes {:sip-has-started #".*pjsua_acc.c  ....sip:[^:]+: registration success, status=200 \(OK\).*"
              :sip-call-buddy-list #"Buddy list:"
              :sip-call-choices #"Choices:"
              :sip-call-enter #"  <Enter>    Empty input \(or 'q'\) to cancel"
              :gain-input-titles #"\| Parameters \+  Loopback  \+   Network  \|"
              :gain-input-volume-g #"\|  Volume_G\s+\+\s+(\d+)\s+\+\s+(\d+)\s+\|"
              :gain-input-volume-l #"\|  Volume_L\s+\+\s+(\d+)\s+\+\s+(\d+)\s+\|"
              :gain-input-volume-r #"\|  Volume_R\s+\+\s+(\d+)\s+\+\s+(\d+)\s+\|"
              :gain-input-global-gain #"Entered global gain"
              :gain-input-left-gain #"Entered left gain"
              :gain-input-right-gain #"Entered right gain"})

(def controller-steps ^{:doc "Last step in the regex steps above"}
  #{:sip-has-started
    :sip-call-enter
    :gain-input-global-gain
    :gain-input-left-gain
    :gain-input-right-gain})

(defonce lines (atom []))

(defn process-line [k line]
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
    (cond (set/subset? #{:sip-has-started} current-set)
          [:sip-has-started (into {} lines)]

          (set/subset? #{:sip-call-buddy-list
                         :sip-call-choices
                         :sip-call-enter} current-set)
          [:sip-call (into {} lines)]

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
    :gain-input-global-gain (let [[_ loopback network] (:gain-input-volume-g data)]
                              {:loopback loopback
                               :network network})
    :gain-input-left-gain (let [[_ loopback network] (:gain-input-volume-l data)]
                            {:loopback loopback
                             :network network})
    :gain-input-right-gain (let [[_ loopback network] (:gain-input-volume-r data)]
                             {:loopback loopback
                              :network network})
    nil))

(defn handle-output [context fns line]
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
