(ns net.sohalt.fairsplit.routes
  (:require
   [hiccup.form :as form]
   [hiccup.page :as page]
   [reitit.ring :as rr]
   [ring.middleware.params :as params]
   [ring.middleware.content-type :as ct]
   [clojure.string :as str]))

(defonce !shares (atom {}))

(defn add-share [shares {:as share :keys [id]}]
  (assoc shares id share))

(defn currency-input [name]
  [:input {:type "number"
           :name name
           :step "any"
           :min 0}])

(defn create-share-form []
  [:form {:method "post"}
   (form/label "total" "total")
   (currency-input "total")
   [:div#names]
   [:input {:type "submit" :value "create"}]])

(defn currency? [x]
  ;FIXME use precise type
  (float? x))

(defn redirect [target]
  {:status 302
   :headers {"Location" target}})

(defn create-person [name]
  (let [id (random-uuid)]
    {:id id
     :name name}))

(defn create-share [total names]
  (let [id (random-uuid)]
    {:id id
     :total total
     :people (into {} (map (fn [name]
                             (let [{:as person :keys [id]} (create-person name)]
                               [id person]))
                           names))}))

(defn handle-create-share [{:as req :keys [form-params]}]
  (let [{:strs [total name]} form-params
        total (parse-double total)
        names (filter (comp not str/blank?) name)
        {:as share :keys [id]} (create-share total names)]
    (swap! !shares add-share share)
    (redirect (str "share/" id))))

(defmacro page [& body]
  `(page/html5
    (page/include-js "js/main.js")
    (page/include-js "https://cdn.tailwindcss.com")
    ~@body))

(defn html-response [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(defn handle-healthcheck [req]
  {:status 200})

(defn create-project-form [req]
  (html-response
   (page
    (create-share-form))))

(defn render-share [{:keys [id total people]}]
  (vec (concat
        [:div.grid.grid-cols-2
         [:span "total"] [:span total]]
        (mapcat (fn [person]
                  [[:span (:name person)]
                   (let [link (str "/share/" id "/submit/" (:id person))]
                     [:a {:href link} link])]) (vals people)))))

(defn not-found-response []
  {:status 404
   :body "not found"})

(defn handle-view-share [{:keys [path-params]}]
  (let [id (parse-uuid (:share-id path-params))]
    (if-let [share (@!shares id)]
      (html-response (page (render-share share)))
      (not-found-response))))

(defn submit-bid-form []
  (html-response (page [:form {:action "post"}
                        (currency-input "max-commitment")])))

(defn handle-submit-bid [{:keys [path-params form-params]}])

(def app (rr/ring-handler
          (rr/router
           [["/healthcheck" {:get handle-healthcheck}]
            ["/" {:get create-project-form
                  :post handle-create-share}]
            ["/share/:share-id"
             ["" {:get handle-view-share}]
             ["/submit/:person-id" {:get submit-bid-form
                                    :post handle-submit-bid}]]])
          (rr/create-resource-handler {:path "/"})
          {:middleware [params/wrap-params
                        ct/wrap-content-type]}))
