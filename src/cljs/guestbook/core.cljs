(ns guestbook.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))

(dom/render
  [:h1 "Hello, Reagent"]
  (.getElementById js/document "content"))