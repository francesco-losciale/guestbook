(ns test.reframe)

(require '[re-frame.core :as rf] :reload)

; register handlers for events with `reg-event-fx`

(rf/reg-event-fx
  :init
  (fn [_ _] {:db {:value 1}}))

; register event db only

(rf/reg-event-db :value
                 (fn [db [_ value]]
                   (do
                     (-> db
                         (assoc :value value))
                     )))

; subscribe event
(rf/reg-sub :value
            (fn [db events]
              (do
                (println "listening... " db events)
                (:value db))))

(rf/dispatch [:init])
(rf/dispatch [:value 12])
(rf/subscribe [:value])