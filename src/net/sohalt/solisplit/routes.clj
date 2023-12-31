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

(def server-address (or (System/getenv "SERVER_ADDRESS") "http://localhost:8090"))

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
            :min 0
            :class ["rounded" "border-2" "border-dotted" "p-2"]}]))

(defelem link [to text]
  [:a {:href to
       :class ["underline"]} text])

(defelem label [name text]
  [:label {:for name} text])

(defelem text-field
  ([name]
   (text-field name nil))
  ([name value]
   [:input {:type "text",
            :name name,
            :id name,
            :placeholder name,
            :value value,
            :class ["rounded" "border-2" "border-dotted" "p-2"]}]))

(defelem text-area
  ([name]
   (text-area name nil))
  ([name value]
   [:textarea {:name name,
               :id name,
               :placeholder name,
               :value value,
               :class ["rounded" "border-2" "border-dotted" "p-2"]}]))

(defelem button [text]
  [:input.rounded-lg.border.p-2 {:type "submit" :value text}])

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

(defn header []
  [:h1.bg-teal-800.text-white.text-center.text-4xl.p-2 [:a {:href "/"} "Solisplit"]])

(defn html-response [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

(defn not-found-response []
  {:status 404
   :body "not found"})

(defn handle-healthcheck [req]
  {:status 200})

(defmacro page [& body]
  `(page/html5
    (page/include-js "/js/main.js")
    (page/include-js "https://cdn.tailwindcss.com")
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1"}]
    (header)
    [:div.w-full.flex.flex-row.justify-center.bg-grey-100
     [:div.max-w-5xl.align-self-center.drop-shadow.bg-white.rounded-b.p-5
      ~@body]]))

(defn create-share-form []
  [:form.flex.flex-col.md:grid.md:grid-cols-2.font-sans.gap-2 {:method "post"}
   (label {:class ["mt-3"]} "title" "What do you want to split the cost for?")
   (text-field "title")
   (label {:class ["mt-3"]} "description" "If you want you can provide a bit more detail:")
   (text-area "description")
   (label {:class ["mt-3"]} "total" "What is the total cost that you want to split?")
   (currency-input {:required true} "total" "total")
   [:p.col-span-2.mt-3 "What are the names of the people you want to split the expense with?"]
   [:div#names.col-start-2
    [:div.mb-2 (text-field {:class (concat ["p-2" "w-full"] ["rounded" "border-2" "border-dotted" "p-2"])
                            :required true} "name")]]
   (button {:class ["col-span-2" "bg-teal-800" "text-white"]} "create")])

(defn handle-create-share [{:as req :keys [form-params]}]
  (let [{:strs [title description total name]} form-params
        total (parse-currency total)
        names (filter (comp not str/blank?) name)
        share (create-share (cond-> {:total total
                                     :names names}
                              (not (str/blank? title)) (assoc :title title )
                              (not (str/blank? description)) (assoc :description description)))
        people-ids (keys (:people share))]
    (swap! !shares add-share share)
    (swap! !person->share merge (into {} (map (fn [person-id] [person-id (:id share)]) people-ids)))
    (redirect-to-share share)))

(defn create-project-form [req]
  (html-response
   (page
    [:div.text-sm]
    (create-share-form))))

(defn everyone-submitted-bid? [{:keys [people]}]
  (every? :bid (vals people)))

(defn share-view [router {:as share :keys [title description total people]}]
  [:div.flex.flex-col.font-medium.font-sans
   [:p.pb-5 (str "We want to split " (format-currency total) (when title (str " for " title ".")))]
   (when description [:p.pb-5 description])
   [:p.pb-5 "Share the following links with the people you want to split the cost with:"]
   [:div.grid.gap-5 {:style {"grid-template-columns" "auto auto"}}
    (mapcat (fn [{:keys [id bid name]}]
              [[:p (str "Link for " name) (when bid [:span.text-sm "(submitted)"]) ":"]
               [:p (let [person-link (str server-address (r/match->path (r/match-by-name router :person {:person-id (str id)})))]
                     (link person-link person-link))]])
            (vals people))]])

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
  (into [:div.flex.flex-col.font-medium.font-sans
         [:p "Here is what everyone should pay:"]]
        (for [[id contribution] (compute-distribution share)]
          (let [name (get-in share [:people id :name])]
            [:div.flex.flex-row.mb-2
             [:span.flex-1 name] [:span.flex-1 (format-currency contribution)]]))))

(defn render-not-reached [{:as share :keys [total people]}]
  (let [tc (total-committed share)
        missing (- total tc)
        missing-per-person (/ missing (count people))]
    [:div [:p (str "We are " (format-currency missing) " short (" (format-currency missing-per-person) " per person, when splitting equally).")]]))

(defn handle-check [{:keys [path-params]}]
  (let [id (parse-uuid (:share-id path-params))]
    (if-let [{:as share :keys [people]} (@!shares id)]
      (html-response
       (page
        (if (everyone-submitted-bid? share)
          (if (goal-reached? share)
            (render-distribution share)
            (render-not-reached share))
          [:p "Not everyone has submitted a bid yet"])))
      (not-found-response))))

(defn render-contribution-form [{:keys [name bid]} {:keys [title description total people]}]
  [:div.max-w-3xl
   [:p.pb-2 "Hello " name "!"]
   (when bid
     (list [:p.pb-2 (str "Thank you " name " for your submission. Check back later to see what share you should pay.")]
           [:p.pb-2 "If you want to update the amount you'd be willing to contribute, you can resubmit the form below."]))
   [:p.pb-2 (str "We want to split " (format-currency total) " among " (count people) " people " (str " (" (format-currency (/ total (count people))) " per person, when splitting equally)") (when title (str " for " title)) ".")]
   (when description [:p.pb-2 description])
   [:form.flex.flex-col.md:grid.md:grid-cols-2.font-sans.gap-2
    {:method "post"}
    (label "bid" [:div.flex.flex-col [:span "How much would you be willing to contribute?"] [:span.text-sm "(you will probably end up paying a bit less)"]])
    (currency-input {:required true} "bid")
    (button {:class ["col-span-2" "bg-teal-800" "text-white"]} "submit")]])

(defn person-view [{:keys [path-params]}]
  (let [person-id (parse-uuid (:person-id path-params))]
    (if-let [share (person->share person-id)]
      (html-response
       (page
        (if (everyone-submitted-bid? share)
          (if (goal-reached? share)
            [:p (str "Your share is " (format-currency ((compute-distribution share) person-id)) ".")]
            (render-not-reached share))
          (render-contribution-form (get-in share [:people person-id]) share))))
      (not-found-response))))

(defn handle-submit-contribution [{:as req :keys [path-params form-params]}]
  (let [person-id (parse-uuid (:person-id path-params))
        share-id (@!person->share person-id)
        bid (parse-currency (get form-params "bid"))]
    (swap! !shares (fn [shares] (assoc-in shares [share-id :people person-id :bid] bid)))
    (redirect (str "/contribute/" person-id))))

(defn home [req]
  (html-response (page
                  [:p.pb-2.mt-5 [:span.font-bold "Solisplit"] " is an application to share costs in a more " [:span.font-bold "solidaric"] " way."]
                  [:h2.text-xl.pb-2.mt-5 "How it works"]
                  [:ol.list-decimal.p-5
                   [:li "You enter the total cost that you want to split."]
                   [:li "You enter the names of all the people who want to share the cost."]
                   [:li "Everyone gets a personalized link to a form, where they can enter the maximum amount they would be willing to contribute to the shared expense."]
                   [:li "After everyone has entered their answer, you can check the result to see the actual share that everyone should pay:"
                    [:ul.list-disc.p-5
                     [:li "If the sum of everyone's maximum contribution exceeds the total cost that you want to split, the total cost is divided among everyone, proportional to their answers. This means that everyone pays at most the amount they were comfortable with and the cost is split fairly, based on how much someone was willing to contribute."]
                     [:li "If the sum of everyone's maximum contribution does not reach the total cost, the program cannot calculate a fair distribution and instead shows how much more money would be needed to reach the total cost. You can then start over from the beginning and hope that people will increase their maximum contribution or try reducing the total cost if your situation allows for it."]]]]
                  [:h2.text-xl.pb-2.mt-5 "Example"]
                  [:p.pb-2 "Alice, Bob, and Charlie live together in a shared appartment and want to buy a new fridge for 600€."]
                  [:p.pb-2 "Alice is an engineer with a good salary and offers to pay up to 250€. Bob is a nurse and can pay at most 150€. Charlie is a student and also offers to pay 150€."]
                  [:p.pb-2 "Since the total of 250+150+150 = 550 is below the required 600, we cannot compute a fair distribution and instead show that they are 50€ short of reaching their total."]
                  [:p.pb-2 "The three decide to do a second round and hope that everyone can increase their share a bit."]
                  [:p.pb-2 "This time Alice offers to pay up to 400€, Bob and Charlie each offer to pay up to 200€."]
                  [:p.pb-2 "Since the total of 400+200+200 = 800 exceeds the required 600, we now split the 600 proportional to the offers. This results in the following shares:"]
                  [:ul.list-disc.p-5
                   [:li "Alice: 600 * 400/800 = 300"]
                   [:li "Bob: 600 * 200/800 = 150"]
                   [:li "Charlie: 600 * 200/800 = 150"]]
                  [:p.pb-2 "As we can see, Bob and Charlie pay the same, since they entered the same maximum. Alice pays double what Bob or Charlie are paying, since her maximum was twice that of Bob or Charlie. Also everyone pays less than their stated maximum and the sum of everyone's share adds up to the required total."]
                  [:div.flex.justify-center
                   [:a.rounded-lg.border.p-2.bg-teal-800.text-white.text-lg {:href "/share/"} "Split expense"]])))


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
