# Sling Jobs, not to be confused with Sling Event bundle.

This Bundle provides Job processing using a message queue. It aims to do that using off the shelf queues. It does not 
provide an API that allows interaction with the queue beyond that is supported by the ISO AMQP standard or the JMS API. Although the 
classes here may have similar names to the API in org.apache.sling.egent.jobs, they are not the same. Methods present
in org.apache.sling.egent.jobs API that are not compatible with a distributed message queue concept are not included, and the 
API is designed with message passing in mind.