(ns user
  (:require [nextjournal.clerk :as clerk]
            [net.sohalt.solisplit.main :as main]
            [org.httpkit.server :as server]))

(clerk/serve! {:browse? true})
(clerk/show! 'nextjournal.clerk.tap)
(let [server (main/-main)
      port (server/server-port server)]
  (clojure.java.browse/browse-url (format "http://localhost:%s" port)))
