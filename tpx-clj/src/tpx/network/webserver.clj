(ns tpx.network.webserver
  (:require [com.stuartsierra.component :as component]
            [hiccup.page :refer [html5]]
            [org.httpkit.server :as httpkit.server]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :as ring.response]
            [tpx.network.middleware :refer [inject-data]]
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
  align-items: center;
  width: 50%;
  margin: 0 auto;
  font-family:sans-serif;
}
.main h1:first-of-type {
    padding: 2rem 0 4rem 0;
}
table {
  width: 100%;
}
td:first-of-type {
  padding-right: 1rem;
}
tr:not(:first-of-type) td {
  padding-top: 20px;
}
.main form {
  background-color: #0000000f;
  padding: 2rem;
  border: 2px solid #0000000f;
}
"]])


(defn confirm []
  (html5 [:div {:style "text-align:center;"}
          [:h2 "Network settings changed!"]
          [:p "(You can close this window now)"]]))

(defn handle-post [{:keys [form-params] :as _request}]
  (let [set-network! (get-in _request [:data :webserver :set-network!])
        webserver (get-in _request [:data :webserver])]
    (do
      ;; reset IP
      (set-network! (clojure.set/rename-keys form-params {"IP" :ip "Gateway" :gateway "Netmask" :netmask "DHCP" :dhcp?}))
      
      ;; shutdown webserver      
      (future (Thread/sleep 200)
              (.stop webserver))
      (-> (ring.response/response (confirm)) :body))))

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
                              :placeholder (case what
                                             "IP" "192.168.0.168"
                                             "Gateway" "192.168.0.1"
                                             "Netmask" "255.255.255.0"
                                             "DHCP" nil)}
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
      (form request))]
   [:script {:type "text/javascript"}
    "document.addEventListener('DOMContentLoaded', (e) => {
         function add_validation(field) {
           field.addEventListener('input', () => { 
             field.setCustomValidity(''); 
             field.checkValidity(); 
           });
           field.addEventListener('invalid', () => {
             if (field.value == '') {
               field.setCustomValidity('Please enter a valid IP address') 
             } else { 
               field.setCustomValidity('Invalid IPv4 format')
             }
           });
         }
         console.log(\"DOMContentLoaded\");
         var fields = Array.from(document.querySelectorAll('input'));

         fields[0].addEventListener('click', () => {
           if (fields[0].checked == true) {
             fields.slice(1, -1).forEach((field) => {
               field.setAttribute('disabled', true);
             })
           } else {
             fields.slice(1, -1).forEach((field) => {
               field.removeAttribute('disabled');
             })
           }
         })

         fields.slice(1, -1).forEach((field) => {
           field.setAttribute('required', true);
           field.addEventListener('input', () => { 
             field.setCustomValidity(''); 
             field.checkValidity(); 
           });
           field.addEventListener('invalid', () => {
             if (field.value == '') {
               field.setCustomValidity(`Please enter a valid ${field.id}`) 
             } else { 
               field.setCustomValidity('Invalid IPv4 format')
             }
           });
         });
});"]])

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
          (let [new-this (assoc this
                                :started? true)
                started-server (httpkit.server/run-server (-> #'app                                                              
                                                              (wrap-keyword-params)
                                                              (wrap-params)
                                                              (inject-data {:webserver new-this})) config)]
            (reset! server started-server)
            new-this))))
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

