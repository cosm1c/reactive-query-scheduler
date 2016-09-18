Reactive Query Scheduler
========================

A simple query scheduler that handles processing queries on a blocking thread.

- periodically checks the health of all queries by calling http endpoints to test like a client
- provides status of all queries over JMX or HTTP request
- by default will block access to a query while it is broken (override with query parameter bypassHealthCheck=true.
- incoming queries that are equal to a query that is currently running will receive that (faster response)
- queries can be cached for a time (currently just 1 second)
- includes Gatling scenario to load test
- records instant in time at start and end of operations (could be recorded/reported)
