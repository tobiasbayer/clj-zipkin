(ns clj-zipkin.middleware
  (:require
   [clj-zipkin.tracer :as t]
   [clj-zipkin.tls :as tls]))

(def ^:private TRACE-ID (.toLowerCase "X-B3-TraceId"))
(def ^:private SPAN-ID (.toLowerCase "X-B3-SpanId"))
(def ^:private PARENT-SPAN-ID (.toLowerCase "X-B3-ParentSpanId"))

(defn request-tracer [handler config]
  (fn [request]
    (let [tid (or (get (:headers request) TRACE-ID) (t/create-id))
          pid (get (:headers request) PARENT-SPAN-ID)
          sid (or (get (:headers request) SPAN-ID) (t/create-id))]
      (t/trace {:trace-id tid
                :span-id sid
                :parent-span-id pid
                :span (str (:request-method request) ":" (:uri request))
                :host (or (:host config)
                          (when (:service config)
                            {:service (:service config)}))
                :config config}
               (handler (assoc request :zipkin {:trace-id tid
                                                :span-id sid}))))))

(defn wrap-trace
  [handler logger]
  (fn [{:keys [request-method uri] :as request}]
    (let [tid (or (if-let [id (get-in request [:headers TRACE-ID])]
                    (Integer/parseInt id))
                  (t/create-id))
          pid (if-let [id (get-in request [:headers PARENT-SPAN-ID])]
                (Integer/parseInt id))
          span (tls/start-span {:operation (name request-method)
                                :host "10.2.1.1" ; TODO: get this from some environment variable
                                :trace-id tid
                                :parent-id pid})
          resp (handler (assoc request :zipkin {:trace-id tid
                                                :span-id (:span-id span)}))]
      (tls/close-span logger)
      resp)))
