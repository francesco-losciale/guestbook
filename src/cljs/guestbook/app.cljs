(ns ^:dev/once guestbook.app (:require
                               [devtools.core :as devtools]
                               [guestbook.core :as core]))
(enable-console-print!)
(println "loading env/dev/cljs/guestbook/app.cljs...")
(devtools/install!)

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))
(core/init!)