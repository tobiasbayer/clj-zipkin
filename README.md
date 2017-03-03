# clj-zipkin

Zipkin tracing instrumentation for Clojure applications.

## Usage

Add the following dependency to your `project.clj` file:

       [org.clojars.tobiasbayer/clj-zipkin "0.1.5"]

### Tracing

Tracing a code chunk 

```clojure

(:require [clj-zipkin.tracer :as t)

(t/trace {:host "10.2.1.2" :span "GET" :config {:type :scribe
                                                :scribe {:host "zipkin.host" :port 9410}}}
         (..code..))

```

Nested tracing, scribe config can be avoided for inner tracing. This will make inner traces
 appear in zipkin as childs of the immediate parent trace.

```clojure

(:require [clj-zipkin.tracer :as t)

(t/trace {:host "10.2.1.2" :span "GET" :config {:type :scribe
                                                :scribe {:host "zipkin.host" :port 9410}}}
         (..code..)
         (t/trace {:host "10.2.1.2" :span "OTHER"}
                  (..code..)))

```

Tracing with a specific `trace-id`, since all related spans in zipkin should share the
same `trace-id`, if your code is executing in different hosts you can tie your traces
together specifying the `trace-id` to use.

```clojure

(:require [clj-zipkin.tracer :as t)

(t/trace {:host "10.2.1.2" :span "GET" :trace-id 12345 :config {:type :scribe
                                                                :scribe {:host "zipkin.host" :port 9410}}}
         (..code..)
         (t/trace {:host "10.2.1.2" :span "OTHER"}
                  (..code..)))

```

#### Parameters

Tracing parameters

```
  :host => current host, defaults to InetAddress/getLocalHost if unspecified
  :span => span name
  :trace-id => optional, new one will be created if unspecified
  :span-id => optional, current span-id, useful to append annotations or to keep track of created id
  :parent-span-id => optional, parent span-id, this span will be nested
  :scribe => scribe/zipkin endpoint configuration {:host h :port p}
```

The `host` parameter can also be a structured hash-map

```clojure
   {:host "10.2.1.1" :port 3030 :service "Service Name"}
```

If not specified will default to port `0` and service `Unknown Service`.

#### Connection

The `make-logger` function creates a logger object to be reused among different tracing calls.

Use it in order to avoid creation of a new connection for each logged span.

```clojure

(def conn (t/make-logger {:type :scribe 
                          :host "zipkin.host" :port 9410}))

(t/trace {:config {:type :scribe 
                   :scribe conn} 
          :span "GET"}
         (..code..))

```

### Ring Handler

A ring handler is available for automated tracing of incoming requests

```clojure
   (require '[clj-zipkin.middleware :as m])
  
   (defroutes routes
     (GET "/" [] "<h1>Hello World</h1>")
     (route/not-found "<h1>Page not found</h1>"))

   (def app
       (-> routes
       (m/request-tracer {:type :scribe
                          :scribe {:host "localhost" :port 9410}
                          :service "WebServer"})))

```

We also have another middleware that takes advantage of the start/stop span functions.  We can also write a wrap-annotations function that will add annotations to the span.

```clojure

   (require '[clj-zipkin.middleware :as m])
  
   (defroutes routes
     (GET "/" [] "<h1>Hello World</h1>")
     (route/not-found "<h1>Page not found</h1>"))

   (defn wrap-annotations
     "We add some more annotations here."
     [handler]
     (fn [{:keys [uri query-string] :as request}]
       (tls/add-annotation {:path uri})
       (when query-string (tls/add-annotation {:query query-string)})
       (handler request)))

   (def app
       (-> routes
       wrap-annotations
       (m/wrap-trace (t/make-logger {:type :scribe 
                                     :host "localhost" :port 9410}))))

   
```

Configuration received by the tracer handler is almost the same as the `trace` function defined above, so it also supports a `host` parameter.

```clojure
(m/request-tracer {:type :scribe
                   :scribe {:host "localhost" :port 9410}
                   :host {:port 2020 :service "MyService"}}))
```

If no `ip` is specified will default to `java.net.InetAddress/getLocalHost`

## License

Copyright © 2013 Guillermo Winkler

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[1]: http://docs.oracle.com/javase/7/docs/api/java/lang/ThreadLocal.html
