(ns guestbook.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))

(dom/render
  [:div#hello.content>h1 "Hello, Auto!"]
  (.getElementById js/document "content"))