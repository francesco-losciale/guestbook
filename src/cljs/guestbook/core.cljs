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
    {:db {:messages/loading? true}}))

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

; events subscription

(rf/reg-sub
  :messages/loading?
  (fn [db _]
    (:messages/loading? db)))

(rf/reg-sub
  :messages/list
  (fn [db _]
    (:messages/list db [])))

(defn message-form []
  (let [fields (r/atom {})
        errors (r/atom nil)]
    (fn [] [:div
            [:div.field
             [:label.label {:for :name} "Name"]
             [errors-component errors :name]
             [:input.input
              {:type :text
               :name :name
               :on-change #(swap! fields
                                  assoc :name (-> % .-target .-value)) :value (:name @fields)}]]
            [:div.field
             [:label.label {:for :message} "Message"]
             [errors-component errors :message]
             [:textarea.textarea
              {:name  :message
               :value (:message @fields) :on-change #(swap! fields assoc :message (-> % .-target .-value))}]]
            [:input.button.is-primary
             {:type     :submit
              :on-click #(send-message! fields errors)
              :value    "comment"}]

            [:p "Name: " (:name @fields)]                   ; what's @ ? it's called deref, it returns the atom's value
            [:p "Message: " (:message @fields)]
            [errors-component errors :server-error]])))

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

(defn get-messages []
  (GET "/api/messages"                                          ; no need of csrf for GET
       {:headers {"Accept" "application/transit+json"}
        :handler #(rf/dispatch [:messages/set (:messages %)])}))

(defn message-list [messages]
  (println messages)
  [:ul.messages
   (for [{:keys [timestamp message name]} @messages]
     ^{:key timestamp}                                      ; li identifier
     [:li
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p " - " name]])])

(defn send-message! [fields errors]
  (if-let [validation-errors (validate-message @fields)]
    (reset! errors validation-errors)
    (POST "/api/message"
          {:params        @fields
           :headers
                          {"Accept"       "application/transit+json"
                           "x-csrf-token" (.-value (.getElementById js/document "token"))
                           }
           ; cljs-ajax uses status code in response to choose the handler to use...
           :handler       (fn [r]
                            (.log js/console (str "response:" r))
                            (rf/dispatch
                              [:message/add (assoc @fields :timestamp (js/Date.))])
                            (reset! fields nil)
                            (reset! errors nil)
                            )
           :error-handler (fn [e]
                            (.log js/console (str e))
                            (reset! errors (-> e :response :errors)))})))

(dom/render
  [home]
  (.getElementById js/document "content"))