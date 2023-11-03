(ns net.sohalt.solisplit.routes
  (:require
   [hiccup.def :refer [defelem]]
   [hiccup.page :as page]
   [reitit.ring :as rr]
   [ring.middleware.params :as params]
   [ring.middleware.content-type :as ct]
   [clojure.string :as str]))

(defonce !shares (atom {}))

(comment
  (reset! !shares {}))

(defn add-share [shares {:as share :keys [id]}]
  (assoc shares id share))

(defn currency? [x]
  ;FIXME use precise type
  (double? x))

(defn parse-currency [s]
  (parse-double s))

(defn format-currency [x]
  (format "%.2f" (double x)))

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
   [:input {:type "textarea", :name name, :id name, :placeholder name, :value value}]))

(defelem button [text]
  [:input.rounded-lg.outline.p-2.m-5 {:type "submit" :value text}])

(defn create-share-form []
  (let [left {:class ["p-2" "flex-1"]}
        right {:class ["p-2" "flex-1" "outline-dotted"]}]
    [:form.flex.flex-col.max-w-md.font-medium.font-sans.bg-blue-200.p-5 {:method "post"}
     [:div.flex.flex-row
      (label left "title" "title")
      (text-field right "title")]
     [:div.flex.flex-row
      (label left "description" "description")
      (text-area right "description")]
     [:div.flex.flex-row
      (label left "total" "total")
      (currency-input right "total" "total")]
     [:div#names
      [:div.flex.flex-row
       (label left "name" "name")
       (text-field right "name")]]
     (button "create")]))

(defn redirect [target]
  {:status 302
   :headers {"Location" target}})

(defn redirect-to-share [{:keys [id]}]
  (redirect (str "/share/" id "/")))

(defn create-person [name]
  (let [id (random-uuid)]
    {:id id
     :name name}))

(defn create-share [{:as share :keys [names]}]
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
                              description (assoc :description description )))]
    (swap! !shares add-share share)
    (redirect-to-share share)))

(defmacro page [& body]
  `(page/html5
    (page/include-js "js/main.js")
    (page/include-js "https://cdn.tailwindcss.com")
    ~@body))

(defn html-response [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

(defn handle-healthcheck [req]
  {:status 200})

(defn create-project-form [req]
  (html-response
   (page
    (create-share-form))))

(defn everyone-submitted-bid? [{:keys [people]}]
  (every? :bid (vals people)))

(defn contribution-form [{:as share :keys [id total people]}]
  (let [left {:class ["p-2" "flex-1"]}
        right {:class ["p-2" "flex-1" "outline-dotted"]}]
    [:div
     [:p "total: " (format-currency total) (str " (" (format-currency (/ total (count people))) " per person, when splitting equally)")]
     [:p "find your name and enter the maximum you'd be willing to contribute (leave other fields blank)"]
     [:form.flex.flex-col.max-w-md.font-medium.font-sans.bg-blue-200.p-5 {:method "post"}
      (for [{:keys [id name bid]} (vals people)]
        [:div.flex.flex-row {:class (if bid ["bg-green-200" "submitted"] [])}
         (label left id name)
         (currency-input right id) #_(when bid [:span.submitted "(already submitted)"])])
      (button "submit my contribution")]
     [:form {:method "get"
             :action "check"}
      (button (let [disabled? (not (everyone-submitted-bid? share))]
                {:id "check"
                 :disabled disabled?
                 :class (when disabled? ["bg-gray-200"])})
              "check if goal is reached")]]))

(defn not-found-response []
  {:status 404
   :body "not found"})

(defn handle-view-share [{:keys [path-params]}]
  (let [id (parse-uuid (:share-id path-params))]
    (if-let [share (@!shares id)]
      (html-response (page (contribution-form share)))
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
  (into [:div]
        (for [[id contribution] (compute-distribution share)]
          (let [name (get-in share [:people id :name])]
            [:div [:span name] [:span (format-currency contribution)]]))))

(defn render-not-reached [{:as share :keys [total people]}]
  (let [tc (total-committed share)
        missing (- total tc)
        missing-per-person (/ missing (count people))]
    [:div [:p (str "We are " (format-currency missing) " short (" (format-currency missing-per-person) ") per person, when splitting equally.")]]))

(defn handle-check [{:keys [path-params]}]
  (let [id (parse-uuid (:share-id path-params))]
    (if-let [{:as share :keys [people]} (@!shares id)]
      (if (everyone-submitted-bid? share)
        (if (goal-reached? share)
          (html-response (page (render-distribution share)))
          (html-response (page (render-not-reached share))))
        (html-response "not everyone has submitted a bid yet"))
      (not-found-response))))

(def app (rr/ring-handler
          (rr/router
           [["/healthcheck" {:get handle-healthcheck}]
            ["/" {:get create-project-form
                  :post handle-create-share}]
            ["/share/:share-id"
             ["/" {:get handle-view-share
                   :post handle-update-share}]
             ["/check" {:get handle-check}]]])
          (rr/create-resource-handler {:path "/"})
          {:middleware [params/wrap-params
                        ct/wrap-content-type]}))
