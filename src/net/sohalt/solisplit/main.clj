(ns net.sohalt.solisplit.main
  (:require
   [babashka.cli :as cli]
   [org.httpkit.server :as server]
   [net.sohalt.solisplit.routes :as routes])
  (:gen-class))

(defonce !server (atom nil))

(defn start! [options]
  (if (and (some? @!server) (= :running (server/server-status @!server)))
    {:error "Server already running"}
    (let [server (reset! !server (server/run-server #'routes/dev-app (merge options {:legacy-return-value? false})))]
      (println (format "Started server on port %s" (server/server-port server)))
      server)))

(defn stop! []
  (if (not= :running (server/server-status @!server))
    {:error "Server not running"}
    (server/server-stop! @!server)))

(defn -main [& args]
  (println (format "Starting solisplit (version %s)" (or (System/getProperty "version") "dev")))
  (start! (cli/parse-opts (or *command-line-args* args))))
