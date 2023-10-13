(ns user
  (:require [nextjournal.clerk :as clerk]
            [net.sohalt.fairsplit.main :as main]))

(clerk/serve! {:browse? true})
(clerk/show! 'nextjournal.clerk.tap)
(main/-main)
