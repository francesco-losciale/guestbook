(ns guestbook.messages
  (:require [guestbook.db.core :as db]
            [guestbook.validation :refer [validate-message]]
            [ring.util.http-response :as response]))


(defn save-message! [message]
  (if-let
    [errors (validate-message message)]
    (throw
      (ex-info "Message is invalid"
               {:guestbook/error-id :validation
                :errors             errors}))
    (db/save-message! message)))

(defn message-list []
  (response/ok {:messages (vec (db/get-messages))}))