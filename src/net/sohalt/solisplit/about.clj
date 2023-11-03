(ns net.sohalt.solisplit.about
  (:require [clojure.java.io :as io]
   [nextjournal.markdown :as md]
            [nextjournal.markdown.transform :as transform]))

(defn project-description []
  (-> (io/resource "about.md")
      slurp
      md/parse
      transform/->hiccup))
