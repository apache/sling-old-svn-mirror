# Apache Sling Event API

This module is part of the [Apache Sling](https://sling.apache.org) project.

For user documentation see https://sling.apache.org/documentation/bundles/apache-sling-eventing-and-job-handling.html. 
This README contains information on the API details.

Note that the default implementation has been spun-off into a separate bundle: sling.event.resource

## Bundle

Sling Event API defines the API for Jobs. It defines Job, JobManager and Queue, as well as consumer Apis for a 
JobConsumer. There are ancillary APIs to support the work of these core interfaces. The core APIs are exported from 
org.apache.sling.event.jobs with the consumers exported from org.apache.sling.event.jobs.consumer.