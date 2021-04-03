(ns guestbook.routes.services
  (:require
    [guestbook.messages :as msg]
    [guestbook.middleware :as middleware]
    [ring.util.http-response :as response]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.exception :as exception]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [guestbook.middleware.formats :as formats]
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]))

;As your middleware stack gets larger, it can become very hard to debug due to the sheer number of separate functions
;report erratum • discuss
;modifying every request map. Fortunately, reitit allows us to specify our mid- dleware as a vector so we can easily
;inspect between each step.
; use `reitit.ring.middleware.dev/print-request-diffs` ....
; are we assuming that we need to debug? :(

(defn service-routes []
  ["/api"
   {:middleware [;; query-params & form-params parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart params
                 multipart/multipart-middleware]
    :muuntaja   formats/instance
    :coercion   spec-coercion/coercion
    :swagger    {:id ::api}}

   ["" {:no-doc true}
    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]
    ["/swagger-ui*"
     {:get (swagger-ui/create-swagger-ui-handler {:url "/api/swagger.json"})}]]
   ["/messages"
    {:get
     {:responses
      {200
       {:body ;; Data Spec for response body
        {:messages
         [{:id pos-int?
           :name string?
           :message string?
           :timestamp inst?}]}}}

      :handler
      (fn [_]
        (response/ok (msg/message-list)))}}]
   ["/message"
    {:post
     {:parameters
      {:body                                                ;; Data Spec for Request body parameters
       {:name    string?
        :message string?}}
      :responses
      {200
       {:body map?}
       400
       {:body map?}
       500
       {:errors map?}}
      :handler
      (fn [{{params :body} :parameters}]
        (try
          (msg/save-message! params) (response/ok {:status :ok})
          (catch Exception e
            (let [{id :guestbook/error-id errors :errors} (ex-data e)]
              (case id
                :validation
                (response/bad-request {:errors errors})     ;;else
                (response/internal-server-error
                  {:errors
                   {:server-error ["Failed to save message!"]}}))))))}}]])