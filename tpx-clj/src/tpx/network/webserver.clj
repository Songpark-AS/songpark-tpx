(ns tpx.network.webserver
  (:require [com.stuartsierra.component :as component]
            [hiccup.page :refer [html5]]
            [org.httpkit.server :as httpkit.server]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [taoensso.timbre :as log]))

(defn post? [request]
  (= (:request-method request) :post))

(defn head [request]
  [:head
   [:title "Songpark Teleporter Network Settings"]
   [:meta {:charset "UTF-8"}]
   [:style ".main {
  display: flex;
  flex-direction: column;
  width: 50%;
  margin: 0 auto;
}
table {
  width: 100%;
}
"]])

(defn handle-post [{:keys [form-params] :as _request}]
  (do
    ;; reset IP
    (log/debug :params form-params)
    
    ))

(defn form [request]
  (let [ip-opts {:minlength 7
                 :maxlength 15
                 :pattern "^((\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.){3}(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])$"}]
    [:form
     {:method :POST}
     [:table
      (for [[type what opts] [[:checkbox "DHCP"]
                              [:text "IP" ip-opts]
                              [:text "Gateway" ip-opts]
                              [:text "Netmask" ip-opts]]]
        [:tr
         [:td [:label {:for what} what]]
         [:td [:input (merge {:type type
                              :id what
                              :name what
                              :placholder what}
                             opts)]]])
      [:tr
       [:td]
       [:td
        [:input {:type :submit
                 :value "Update network settings"}]]]]]))

(defn body [request]
  [:body
   [:div.main
    [:h1 "Songpark Teleporter Network configuration"]
    (if (post? request)
      (handle-post request)
      (form request))]])

(defn page [request]
  (html5
   [:html
    (head request)
    (body request)]))

(defn app [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (page request)})

(defrecord Webserver [started? server set-network! config]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting Webserver" (select-keys config [:ip :port]))
          (let [started-server (httpkit.server/run-server (-> #'app
                                                              (wrap-keyword-params)
                                                              (wrap-params)) config)]
            (reset! server started-server)
            (assoc this
                   :started? true)))))
  (stop [this]
    (if-not started?
      this
      (do
        (log/info "Stopping Webserver")
        (@server :timeout 100)
        (reset! server nil)
        (assoc this
               :started? false)))))


(defn webserver [settings]
  (map->Webserver settings))
