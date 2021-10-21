(ns tpx.data)

(defonce ^:private jam-id* (atom nil))
(defonce ^:private tp-id* (atom nil))

;; Jam ID (UUID)
(defn set-jam-id! [jam-id]
  (reset! jam-id* jam-id))

(defn clear-jam-id! []
  (reset! jam-id* nil))

(defn get-jam-teleporters []
  (str @jam-id* "/teleporters"))

(defn get-jam []
  (str @jam-id* "/jam"))


;; TP ID (UUID)
(defn set-tp-id! [tp-id]
  (reset! tp-id* tp-id))

(defn clear-tp-id! []
  (reset! tp-id* nil))

(defn get-tp-id []
  (str @tp-id*))


(defn same-tp? [tp-id]
  (and (some? @tp-id*)
       (= (str @tp-id*) (str tp-id))))
