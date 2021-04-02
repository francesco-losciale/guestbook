(ns guestbook.routes.home
  (:require
    [guestbook.layout :as layout]
    [guestbook.messages :as msg]
    [guestbook.middleware :as middleware]
    [ring.util.response]))

(defn about-page [request]
  (layout/render request "about.html"))

(defn home-page [request]
  (layout/render
    request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/about" {:get about-page}]])
