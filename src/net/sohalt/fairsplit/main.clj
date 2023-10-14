(ns net.sohalt.fairsplit.main
  (:require [org.httpkit.server :as server]
            [net.sohalt.fairsplit.routes :as routes])
  (:gen-class))

(defonce !server (atom nil))

(defn start! []
  (if (and (some? @!server) (= :running (server/server-status @!server)))
    {:error "Server already running"}
    (let [server (reset! !server (server/run-server #'routes/app {:legacy-return-value? false}))]
      (println (format "Started server on port %s" (server/server-port server)))
      server)))

(defn stop! []
  (if (not= :running (server/server-status @!server))
    {:error "Server not running"}
    (server/server-stop! @!server)))

(defn -main [& args]
  (start!))
