(ns guestbook.routes.home
  (:require
    [guestbook.layout :as layout]
    [guestbook.messages :as msg]
    [guestbook.middleware :as middleware]
    [ring.util.response]))


(defn home-page [request]
  (layout/render
    request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]])
