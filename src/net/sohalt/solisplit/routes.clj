(ns net.sohalt.solisplit.routes
  (:require
   [hiccup.def :refer [defelem]]
   [hiccup.page :as page]
   [reitit.core :as r]
   [reitit.ring :as rr]
   [ring.middleware.params :as params]
   [ring.middleware.content-type :as ct]
   [clojure.string :as str]
   [net.sohalt.solisplit.about :as about]))

(defonce !shares (atom {}))
(defonce !person->share (atom {}))

(def server-address "http://localhost:8090")

(defn person->share [person-id]
  (let [share-id (@!person->share person-id)]
    (@!shares share-id)))

(comment
  (reset! !shares {})
  (reset! !person->share {}))

(defn add-share [shares {:as share :keys [id]}]
  (assoc shares id share))

(defn currency? [x]
  ;FIXME use precise type
  (double? x))

(defn parse-currency [s]
  (parse-double s))

(def currency-symbol "€")

(defn format-currency [x]
  (format "%.2f%s" (double x) currency-symbol))

(comment
  (format-currency 0)
  (let [x (double (/ (rand-int 1000) 100))]
    (= x (parse-currency (format-currency x)))))

(defelem currency-input
  ([name]
   (currency-input name nil))
  ([name placeholder]
   [:input {:type "number"
            :name name
            :placeholder placeholder
            :step "any"
            :min 0}]))

(defelem link [to text]
  [:a {:href to
       :class ["underline"]} text])

(defelem label [name text]
  [:label {:for name} text])

(defelem text-field
  ([name]
   (text-field name nil))
  ([name value]
   [:input {:type "text", :name name, :id name, :placeholder name, :value value}]))

(defelem text-area
  ([name]
   (text-area name nil))
  ([name value]
   [:textarea {:name name, :id name, :placeholder name, :value value}]))

(defelem button [text]
  [:input.rounded-lg.border.p-2 {:type "submit" :value text}])

(defn create-share-form []
  (let [left {:class ["p-2" "flex-1"]}
        right {:class ["mb-1" "p-1" "flex-1" "rounded" "border-2" "border-dotted"]}]
    [:form.flex.flex-col.max-w-md.font-sans {:method "post"}
     [:div.flex.flex-row.mb-2
      (label left "title" "title")
      (text-field right "title")]
     [:div.flex.flex-row.mb-2
      (label left "description" "description")
      (text-area right "description")]
     [:div.flex.flex-row.mb-2
      (label left "total" "total")
      (currency-input (merge right {:required true}) "total" "total")]
     [:div#names
      [:div.flex.flex-row.mb-2
       (label left "name" "name")
       (text-field (merge right {:required true}) "name")]]
     (button {:class ["bg-teal-800" "text-white"]} "create")]))

(defn redirect [target]
  {:status 302
   :headers {"Location" target}})

(defn redirect-to-share [{:keys [id]}]
  (redirect (str "/share/" id "/")))

(defn create-person [name]
  (let [id (random-uuid)]
    {:id id
     :name name}))

(defn create-share [{:as share :keys [total names]}]
  (assert (currency? total) "A project needs a total")
  (assert (seq names) "A project needs at least one person")
  (let [id (random-uuid)]
    (-> share
        (dissoc :names)
        (assoc :id id)
        (assoc :people (into {} (map (fn [name]
                                       (let [{:as person :keys [id]} (create-person name)]
                                         [id person]))
                                     names))))))

(defn handle-create-share [{:as req :keys [form-params]}]
  (let [{:strs [title description total name]} form-params
        total (parse-currency total)
        names (filter (comp not str/blank?) name)
        share (create-share (cond-> {:total total
                                     :names names}
                              title (assoc :title title )
                              description (assoc :description description)))
        people-ids (keys (:people share))]
    (swap! !shares add-share share)
    (swap! !person->share merge (into {} (map (fn [person-id] [person-id (:id share)]) people-ids)))
    (redirect-to-share share)))

(defn header []
  [:h1.bg-teal-800.text-white.text-center.text-4xl.p-2 [:a {:href "/"} "Solisplit"]])

(defmacro page [& body]
  `(page/html5
    (page/include-js "/js/main.js")
    (page/include-js "https://cdn.tailwindcss.com")
    (header)
    [:div.w-full.flex.flex-row.justify-center.bg-grey-100
     [:div.max-w-lg.align-self-center.drop-shadow.bg-white.rounded-b.p-5
      ~@body]]))

(defn html-response [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

(defn handle-healthcheck [req]
  {:status 200})

(defn create-project-form [req]
  (html-response
   (page
    [:div.text-sm]
    (create-share-form))))

(defn everyone-submitted-bid? [{:keys [people]}]
  (every? :bid (vals people)))

(defn contribution-form [{:as share :keys [id total people]}]
  [:div
   [:p "total: " (format-currency total) (str " (" (format-currency (/ total (count people))) " per person, when splitting equally)")]
   [:p "find your name and enter the maximum you'd be willing to contribute (leave other fields blank)"]
   [:form.flex.flex-col.max-w-md.font-medium.font-sans {:method "post"}
    (for [{:keys [id name bid]} (vals people)]
      [:div.flex.flex-row.mb-2
       [:label {:class ["p-2" "flex-1"] :for id} name (when bid [:span.text-xs.text-gray.ml-1 "(already submitted)"])]
       (currency-input {:class ["p-2" "flex-1" "border-2" "border-dotted"]} id)])
    (button {:class ["mb-2" "bg-teal-800" "text-white"]} "submit my contribution")]
   [:form.flex.flex-col.max-w-md.font-medium.font-sans {:method "get"
                                                        :action "check"}
    (button (let [disabled? (not (everyone-submitted-bid? share))]
              {:id "check"
               :disabled disabled?
               :class (if disabled?
                        ["bg-gray-200"]
                        ["bg-teal-800" "text-white"])})
            "check if goal is reached")]])

(defn not-found-response []
  {:status 404
   :body "not found"})

(@!shares #uuid "8f2ab79f-2c80-41dd-8051-a2b8767cfe37" )

(defn share-view [router {:as share :keys [title description total people]}]
  [:div.flex.flex-col.max-w-md.font-medium.font-sans
   (when title [:h2 title])
   (when description [:p description])
   [:span "total: " total]
   [:div [:p "Share this link with the person"]
    (for [{:keys [id bid name]} (vals people)]
      [:div.flex-row.mb-2
       [:span.p-2.flex-1 name (when bid [:span.text-sm "(submitted)"])]
       [:span.p-2.flex-1 (let [person-link (str server-address (r/match->path (r/match-by-name router :person {:person-id (str id)})))]
                           (link person-link person-link))]])]])

(defn handle-view-share [{::r/keys [router] :keys [path-params]}]
  (let [id (parse-uuid (:share-id path-params))]
    (if-let [share (@!shares id)]
      (html-response (page (share-view router share)))
      (not-found-response))))

(defn deep-merge [a b]
  (reduce (fn [acc k] (update acc k merge (b k))) a (keys a)))

(comment
  (deep-merge {"foo" {:id "foo" :name "foo"}
               "bar" {:id "bar" :name "bar"}}
              {"foo" {:bid 2}})
  (deep-merge {"foo" {:id "foo" :name "foo"}
               "bar" {:id "bar" :name "bar"}}
              {"foo" {:bid 2}
               "bar" {:bid 3}})
  (deep-merge {"foo" {:id "foo" :name "foo"}
               "bar" {:id "bar" :name "bar"}}
              {"foo" {:bid 2}
               "baz" {:bid 3}}))

(defn handle-update-share [{:keys [path-params form-params]}]
  (let [id (parse-uuid (:share-id path-params))
        bids (into {} (filter identity (map (fn [[id bid]] (when-let [bid (parse-currency bid)]
                                                             [(parse-uuid id) {:bid bid}]))
                                            form-params)))]
    (if-let [share (@!shares id)]
      (do
        (swap! !shares (fn [shares] (update-in shares [id :people] deep-merge bids)))
        (redirect-to-share share))
      (not-found-response))))

(defn total-committed [{:keys [people]}]
  (reduce + (map :bid (vals people))))

(defn goal-reached? [{:as share :keys [total]}]
  (>= (total-committed share) total))

(defn compute-distribution [{:as share :keys [total people]}]
  (assert (goal-reached? share))
  (let [tc (total-committed share)
        ratio (/ total tc)]
    (into {} (map (fn [[id {:keys [bid]}]] [id (* bid ratio)]) people))))

(comment
  (def share {:id #uuid "3552075c-6258-4c76-98c4-5333b707dc89"
              :total 7
              :people {#uuid "ec0f24ac-0bda-4bb7-b286-dd364c2c8af1"
                       {:id #uuid "ec0f24ac-0bda-4bb7-b286-dd364c2c8af1"
                        :name "foo"
                        :bid 2}
                       #uuid "d7a3f660-ffc9-4cf4-a51c-4fa0f5877d04"
                       {:id #uuid "d7a3f660-ffc9-4cf4-a51c-4fa0f5877d04"
                        :name "bar"
                        :bid 3}}})
  (def share2 (update share :people assoc #uuid "c455da09-ee73-4ca0-b652-c741231c1627"
                      {:id #uuid "c455da09-ee73-4ca0-b652-c741231c1627"
                       :name "baz"
                       :bid 2}))
  (= 5 (total-committed share))
  (= false (goal-reached? share))
  (= true (goal-reached? share2))
  (= {#uuid "ec0f24ac-0bda-4bb7-b286-dd364c2c8af1" 2,
      #uuid "d7a3f660-ffc9-4cf4-a51c-4fa0f5877d04" 3,
      #uuid "c455da09-ee73-4ca0-b652-c741231c1627" 2}
     (compute-distribution share2))
  (render-distribution share2))

(defn render-distribution [{:as share :keys [people]}]
  (into [:div.flex.flex-col.max-w-md.font-medium.font-sans
         [:p "Here is what everyone should pay:"]]
        (for [[id contribution] (compute-distribution share)]
          (let [name (get-in share [:people id :name])]
            [:div.flex.flex-row.mb-2
             [:span.flex-1 name] [:span.flex-1 (format-currency contribution)]]))))

(defn render-not-reached [{:as share :keys [total people]}]
  (let [tc (total-committed share)
        missing (- total tc)
        missing-per-person (/ missing (count people))]
    [:div [:p (str "We are " (format-currency missing) " short (" (format-currency missing-per-person) ") per person, when splitting equally.")]]))

(defn handle-check [{:keys [path-params]}]
  (let [id (parse-uuid (:share-id path-params))]
    (if-let [{:as share :keys [people]} (@!shares id)]
      (html-response
       (page
        (if (everyone-submitted-bid? share)
          (if (goal-reached? share)
            (render-distribution share)
            (render-not-reached share))
          [:p "not everyone has submitted a bid yet"])))
      (not-found-response))))

(defn render-contribution-form []
  [:form {:method "post"}
   [:p "How much would you be willing to contribute?"]
   [:label {:for "bid"} "contribution"]
   (currency-input "bid")
   (button "submit")])

(defn person-view [{:keys [path-params]}]
  (let [person-id (parse-uuid (:person-id path-params))]
    (if-let [share (person->share person-id)]
      (html-response
       (page
        (if (everyone-submitted-bid? share)
          (if (goal-reached? share)
            [:p "You should pay " (format-currency ((compute-distribution share) person-id))]
            (render-not-reached share))
          (render-contribution-form))))
      (not-found-response))))

(defn handle-submit-contribution [{:as req :keys [path-params form-params]}]
  (tap> req)
  (let [person-id (parse-uuid (:person-id path-params))
        share-id (@!person->share person-id)
        bid (parse-currency (get form-params "bid"))]
    (swap! !shares (fn [shares] (assoc-in shares [share-id :people person-id :bid] bid)))
    (redirect (str "/contribute/" person-id))))

(defn home [req]
  (html-response (page
                  (about/project-description)
                  [:div.flex.justify-center
                   [:a.rounded-lg.border.p-2.bg-teal-800.text-white {:href "/share/"} "Split expense"]])))


(def share-id #uuid "0a6b6083-2f0c-400f-a943-5063f117cc04")
(everyone-submitted-bid? (@!shares share-id))

(defn routes []
  [["/healthcheck" {:get handle-healthcheck}]
   ["/" {:get home}]
   ["/share"
    ["/" {:get create-project-form
          :post handle-create-share}]
    ["/:share-id"
     ["/" {:get handle-view-share
           :post handle-update-share}]
     ["/check" {:get handle-check}]]]
   ["/contribute/:person-id" {:name :person
                              :get person-view
                              :post handle-submit-contribution}]])

(def dev-router #(rr/router (routes)))
(def prod-router (constantly (rr/router (routes))))

(defn dev-app [req] ((rr/ring-handler
                      (dev-router)
                      (rr/create-resource-handler {:path "/"})
                      {:middleware [params/wrap-params
                                    ct/wrap-content-type]}) req))
(def prod-app (rr/ring-handler
               (prod-router)
               (rr/create-resource-handler {:path "/"})
               {:middleware [params/wrap-params
                             ct/wrap-content-type]}))
