(ns tpx.ipc.output
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]))

(def regexes {:sip-has-started #".*pjsua_acc.c  ....sip:[^:]+: registration success, status=200 \(OK\).*"
              :sip-call-buddy-list #"Buddy list:"
              :sip-call-choices #"Choices:"
              :sip-call-enter #"  <Enter>    Empty input \(or 'q'\) to cancel"
              :gain-input-titles #"\| Parameters \+  Loopback  \+   Network  \|"
              :gain-input-volume-g #"\|  Volume_G.*(\d+).*(\d+).*\|"
              :gain-input-global-gain #"Entered global gain"})

(defonce lines (atom []))

(defn process-line [k line]
  (if-let [regex (get regexes k)]
    (re-matches regex line)))

(defn- found-happening
  "Check if we have found something"
  [lines]
  (case (mapv first lines)
    [:sip-has-started] [:sip-has-started (into {} lines)]
    [:sip-call-buddy-list :sip-call-choices :sip-call-enter] [:sip-call (into {} lines)]
    [:gain-input-titles :gain-input-volume-g :gain-input-global-gain] [:gain-input-global-gain (into {} lines)]
    nil))

(defn pre-process [happening data]
  (case happening
    :gain-input-global-gain (let [[_ loopback network] (:gain-input-volume-g data)]
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
    (when-let [[happening data] (found-happening @lines)]
      (if-let [f (get fns happening)]
        (f (pre-process happening data) context)
        (log/warn found "has no corresponding fn in " (str fns)))
      (reset! lines []))))
