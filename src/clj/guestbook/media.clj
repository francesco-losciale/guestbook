(ns guestbook.media
  (:require [guestbook.db.core :as db]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:import
    [java.io ByteArrayOutputStream]))

(defn insert-image-returning-name
  [{:keys [tempfile filename content-type]} {:keys [owner]}]
  (with-open [in (io/input-stream tempfile)
              out (ByteArrayOutputStream.)]
    (io/copy in out)
    (if (= 0
           (db/save-file! {:name  filename
                           :data  (.toByteArray out)
                           :owner owner
                           :type  content-type}))
      (do
        (log/error "Attempted to overwrite an image that you don't own!")
        (throw (ex-info "Attempted to overwrite an image that you don't own!"
                                                                                          {:name filename})))
      filename)))