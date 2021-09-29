(ns tpx.init
  (:require ;! Fundamentals
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [clojure.java.shell :as shell]
            [songpark.common.communication :refer [PUT GET]]
            ;! TPX
            [tpx.logger :as logger]
            #_[tpx.config :as tpx.config]
            [tpx.config :refer [config]]
            [tpx.ipc :as tpx.ipc]
            #_[tpx.http :as tpx.http]
            [tpx.audio :as tpx.audio]))


;! --- Sindre's and my stuff ---
(def handler-map
  {:tpx-unit {
              ;! Gains
              :adjust-gain-input       tpx.audio/adjust-gain-input
              :adjust-gain-musician    tpx.audio/adjust-gain-musician
              ;! Mute states
              :toggle-mute-musician    tpx.audio/toggle-mute-musician
              :toggle-mute-unit        tpx.audio/toggle-mute-unit
              ;! Volumes
              :adjust-volume-musician  tpx.audio/adjust-volume-musician
              :adjust-volume-unit      tpx.audio/adjust-volume-unit
              ;! Other effects
              :adjust-dsp-effects      tpx.audio/adjust-dsp-effects
              :toggle-phantom-power    tpx.audio/toggle-phantom-power
              ;! Other
              :audio-file-management   tpx.ipc/audio-file-management
              :toggle-audio-recording  tpx.ipc/toggle-audio-recording}})
              
  ;;  :phone-app  { :dummy dumb }; Not used in tpx

(defn get-device-mac []
  (-> (clojure.string/split (:out (shell/sh "ip" "n")) #"\n")
      first
      (clojure.string/split #"\s")
      (nth 4))
  )

(comment 
  
  (PUT "http://localhost:3000/api/teleporter" 
   {:teleporter/mac "00:0a:35:00:00:00"}
   (fn [response]
     (println response))
   nil)
)
   


(defn init []
  ;; TODO get MAC address and send it to the platform, platform gives you UUID store that for MQTT topic
  ;; TODO start MQTT topic 
  ;; TODO initiate BP communication via serial 
  ;; TODO pubish to MQTT and start listening for mesages 
  (println "I AM TPX"))

{:exit 0, :out "eth0: flags=4099<UP,BROADCAST,MULTICAST>  mtu 1500\n        ether 00:0a:35:00:00:00  txqueuelen 1000  (Ethernet)\n        RX packets 0  bytes 0 (0.0 B)\n        RX errors 0  dropped 0  overruns 0  frame 0\n        TX packets 0  bytes 0 (0.0 B)\n        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0\n        device interrupt 27  base 0xb000  \n\neth1: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500\n        inet 192.168.11.153  netmask 255.255.255.0  broadcast 192.168.11.255\n        inet6 fe80::9fb0:de61:5010:63bc  prefixlen 64  scopeid 0x20<link>\n        ether 02:01:02:03:04:08  txqueuelen 1000  (Ethernet)\n        RX packets 1316619  bytes 66759655 (63.6 MiB)\n        RX errors 0  dropped 0  overruns 0  frame 0\n        TX packets 51782  bytes 4580058 (4.3 MiB)\n        TX errors 1031  dropped 0 overruns 0  carrier 1031  collisions 0\n        device interrupt 28  base 0xc000  \n\nlo: flags=73<UP,LOOPBACK,RUNNING>  mtu 65536\n        inet 127.0.0.1  netmask 255.0.0.0\n        inet6 ::1  prefixlen 128  scopeid 0x10<host>\n        loop  txqueuelen 1000  (Local Loopback)\n        RX packets 298  bytes 19474 (19.0 KiB)\n        RX errors 0  dropped 0  overruns 0  frame 0\n        TX packets 298  bytes 19474 (19.0 KiB)\n        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0\n\nwg0: flags=209<UP,POINTOPOINT,RUNNING,NOARP>  mtu 1420\n        inet 10.100.200.11  netmask 255.255.255.255  destination 10.100.200.11\n        unspec 00-00-00-00-00-00-00-00-00-00-00-00-00-00-00-00  txqueuelen 1000  (UNSPEC)\n        RX packets 1415  bytes 130180 (127.1 KiB)\n        RX errors 0  dropped 0  overruns 0  frame 0\n        TX packets 9904  bytes 481532 (470.2 KiB)\n        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0\n\n", :err ""}