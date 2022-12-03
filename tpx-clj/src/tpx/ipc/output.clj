(ns tpx.ipc.output
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(defn handle-output [context fns line]
  (log/debug :read-line (pr-str line))
  ;; (when-let [[found match] (reduce (fn [_ [k regex]]
  ;;                                    (if-let [match (process-line k line)]
  ;;                                      (reduced [k match])
  ;;                                      nil))
  ;;                                  nil regexes)]
  ;;   (swap! lines conj [found match])
  ;;   ;; (log/debug {:found found
  ;;   ;;             :match match})
  ;;   (let [[happening data] (found-happening lines)]
  ;;     (when happening
  ;;       ;; (log/debug happening)
  ;;       (if-let [f (get fns happening)]
  ;;         (f (pre-process happening data) context)
  ;;         (log/warn found "has no corresponding fn in " (str fns)))
  ;;       (reset! lines []))
  ;;     (do ;; (log/debug :reset-lines? found @lines)
  ;;         (reset-lines? found lines))))
  )
