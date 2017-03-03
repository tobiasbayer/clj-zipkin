(ns clj-zipkin.middleware
  (:require
   [clj-zipkin.tracer :as t]))

(def ^:private TRACE-ID (.toLowerCase "X-B3-TraceId"))
(def ^:private SPAN-ID (.toLowerCase "X-B3-SpanId"))
(def ^:private PARENT-SPAN-ID (.toLowerCase "X-B3-ParentSpanId"))

(defn- to-long [v]
  (some-> v
          read-string))

(defn request-tracer [handler config]
  (fn [request]
    (let [tid (or (to-long (get (:headers request) TRACE-ID)) (t/create-id))
          pid (to-long (get (:headers request) PARENT-SPAN-ID))
          sid (or (to-long (get (:headers request) SPAN-ID)) (t/create-id))]
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
