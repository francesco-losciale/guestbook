(ns guestbook.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [ajax.core :refer [GET POST]]
            [clojure.string :as string]
            [guestbook.validation :refer [validate-message]]
            [re-frame.core :as rf]))


(defn errors-component [errors id]
  (when-let [error (id @errors)]
    [:div.notification.is-danger (string/join error)]))


; events registration

(rf/reg-event-fx
  :app/initialize
  (fn [_ _]
    {:db {:messages/loading? true}
     :dispatch [:messages/load]}))

(rf/reg-event-fx
  :messages/load
  (fn [{:keys [db]} _]
    (GET "/api/messages"
         {:headers {"Accept" "application/transit+json"}
          :handler #(rf/dispatch [:messages/set (:messages %)])})
    {:db (assoc db :messages/loading? true)}))

(rf/reg-event-db
  :messages/set
  (fn [db [_ messages]]
    (-> db
        (assoc :messages/loading? false
               :messages/list messages))))

(rf/reg-event-db
  :message/add
  (fn [db [_ message]]
    (update db :messages/list conj message)))

(rf/reg-event-db
  :form/set-field
  [(rf/path :form/fields)]
  (fn [fields [_ id value]]
    (assoc fields id value)))

(rf/reg-event-db
  :form/clear-fields
  [(rf/path :form/fields)]
  (fn [_ _]
    {}))

(rf/reg-event-db
  :form/set-server-errors
  [(rf/path :form/server-errors)]
  (fn [_ [_ errors]]
    errors))

(rf/reg-sub
  :form/server-errors
  (fn [db _]
    (:form/server-errors db)))

;;Validation errors are reactively computed
(rf/reg-sub
  :form/validation-errors
  :<- [:form/fields]
  (fn [fields _]
    (validate-message fields)))

(rf/reg-sub
  :form/validation-errors?
  :<- [:form/validation-errors]
  (fn [errors _]
    (not (empty? errors))))

(rf/reg-sub
  :form/errors
  :<- [:form/validation-errors]
  :<- [:form/server-errors]
  (fn [[validation server] _]
    (merge validation server)))

(rf/reg-sub
  :form/error
  :<- [:form/errors]
  (fn [errors [_ id]]
    (get errors id)))

(rf/reg-event-fx
  :message/send!
  (fn [{:keys [db]} [_ fields]]
    (POST "/api/message"
          {:format        :json
           :headers
                          {"Accept"       "application/transit+json"
                           "x-csrf-token" (.-value (.getElementById js/document "token"))}
           :params        fields
           :handler       #(rf/dispatch
                             [:message/add
                              (-> fields
                                  (assoc :timestamp (js/Date.)))])
           :error-handler #(rf/dispatch
                             [:form/set-server-errors
                              (get-in % [:response :errors])])})
    {:db (dissoc db :form/server-errors)}))
;

; events subscription

(rf/reg-sub
  :messages/loading?
  (fn [db _]
    (:messages/loading? db)))

(rf/reg-sub
  :form/field
  :<- [:form/fields]
  (fn [fields [_ id]]
    (get fields id)))

(rf/reg-sub
  :form/fields
  (fn [db _]
    (:form/fields db)))

(rf/reg-sub
  :messages/list
  (fn [db _]
    (:messages/list db [])))

;;;


;(defn send-message! [fields errors]
;  (if-let [validation-errors (validate-message @fields)]
;    (reset! errors validation-errors)
;    (POST "/api/message"
;          {:params        @fields
;           :headers
;                          {"Accept"       "application/transit+json"
;                           "x-csrf-token" (.-value (.getElementById js/document "token"))
;                           }
;           ; cljs-ajax uses status code in response to choose the handler to use...
;           :handler       (fn [r]
;                            (.log js/console (str "response:" r))
;                            (rf/dispatch
;                              [:message/add (assoc @fields :timestamp (js/Date.))])
;                            (reset! fields nil)
;                            (reset! errors nil)
;                            )
;           :error-handler (fn [e]
;                            (.log js/console (str e))
;                            (reset! errors (-> e :response :errors)))})))

(defn message-list [messages]
  (println messages)
  [:ul.messages
   (for [{:keys [timestamp message name]} @messages]
     ^{:key timestamp}                                      ; li identifier
     [:li
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p " - " name]])])

(defn errors-component [id]
  (when-let [error @(rf/subscribe [:form/error id])]
    [:div.notification.is-danger (string/join error)]))

(defn reload-messages-button []
  (let [loading? (rf/subscribe [:messages/loading?])]
    [:button.button.is-info.is-fullwidth {:on-click #(rf/dispatch [:messages/load])
                                          :disabled @loading?} (if @loading?
                                                                 "Loading Messages" "Refresh Messages")]))

(defn message-form []
  [:div
   [errors-component :server-error]
   [:div.field
    [:label.label {:for :name} "Name"]
    [errors-component :name]
    [:input.input
     {:type :text
      :name :name
      :on-change #(rf/dispatch
                    [:form/set-field
                     :name
                     (.. % -target -value)])
      :value @(rf/subscribe [:form/field :name])}]]
   [:div.field
    [:label.label {:for :message} "Message"]
    [errors-component :message]
    [:textarea.textarea
     {:name :message
      :value @(rf/subscribe [:form/field :message])
      :on-change #(rf/dispatch
                    [:form/set-field
                     :message
                     (.. % -target -value)])}]]
   [:input.button.is-primary
    {:type :submit
     :disabled @(rf/subscribe [:form/validation-errors?])
     :on-click #(rf/dispatch [:message/send!
                              @(rf/subscribe [:form/fields])])
     :value "comment"}]])


(defn get-messages []
  (GET "/api/messages"                                      ; no need of csrf for GET
       {:headers {"Accept" "application/transit+json"}
        :handler #(rf/dispatch [:messages/set (:messages %)])}))

; The get-messages and the message-list functions have no direct coupling between them and are not aware of each other.
; The Reagent atoms provide a way for any component to observe the current value in the model without having the
; knowledge of how and when it’s populated.
(defn home []
  "The component calls the get-messages function on the messages when it is first mounted, then
  returns it’s render function. Reagent will render home, including message-list, while the value
  of messages is still nil. When the get- messages function finishes, the messages atom is reset with
  the messages from the server and the message-list component is then repainted automatically."
  (let [messages (rf/subscribe [:messages/list])]
    (fn []
      (if @(rf/subscribe [:messages/loading?])
        [:div>div.row>div.span12>h3 "Loading Messages..."]
        [:div.content>div.columns.is-centered>div.column.is-two-thirds
         [:div.columns>div.column
          [:h3 "Messages"]
          [message-list messages]]
         [:div.columns>div.column
          [reload-messages-button]]
         [:div.columns>div.column
          [message-form messages]]]))))

(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (.log js/console "Mounting Components...")
  (dom/render [#'home] (.getElementById js/document "content"))
  (.log js/console "Components Mounted!"))

(defn init! []
  (.log js/console "Initializing App...")
  (rf/dispatch [:app/initialize])
  (get-messages)
  (mount-components))

(dom/render
  [home]
  (.getElementById js/document "content"))

(.log js/console "guestbook.core evaluated!")