(ns net.sohalt.solisplit.about
  (:require [clojure.java.io :as io]
            [clojure.walk :as walk]
            [nextjournal.markdown :as md]
            [nextjournal.markdown.transform :as transform]))

(defn remove-plain-nodes [ast]
  (walk/postwalk (fn [x] (if (map? x)
                                   (let [{:keys [type content]} x]
                                     (if (= type :plain)
                                       (first content)
                                       x))
                                   x)) ast))
(defn project-description []
  (-> (io/resource "about.md")
      slurp
      md/parse
      remove-plain-nodes
      transform/->hiccup))
