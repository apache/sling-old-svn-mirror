# Message Oriented Middleware API implementation using Active MQ.

This bundle implements the MoM API using Active MQ. It supports both Pub/Sub and Queue patterns and will run out of the
box, embedded or connected to a dedicated cluster.

## Out of the Box.

As the name suggests, no configuration is required, the components will start, create an ActiveMQ instance embedded inside
the OSGi container and run. On restart, normal or after a crash, provided the working directory is not changed or modified, 
the ActiveMQ embedded server will restart and resume operations. In the event the JVM crashes, ActiveMQ will perform recovery
 by replaying its journal. The embedded server uses KahaDB to store data on local disk.
 
## Embedded with custom configuration.

AMQ can be run embedded with custom configuration to allow a cluster of Sling instances for form a multi master AMQ cluster with each 
Sling instance embedding its own AMQ broker. This is achieved via OSGi configuration. (ToDo: Config + Doc)

## External AMQ Broker cluster.

The bundle can be run to use an external AMQ Broker cluster, maintained and setup separately from the Sling cluster. To do this, modify the
Broker URL via OSGi configuration.

# Implementation details.

## AMQ ConnectionFactory.

Running AMQ inside OSGi is very simple. All that is required is the AMQ dependencies, and instancing an AQM PooledConnectionFactory with a
default localhost url. vm://localhost:61616. The PooledConnectionFactory will trigger the creation of an AMQ Broker if one is not present
and AMQ will run normally. This service implements an internal service API org.apache.sling.jms.ConnectionFactoryService, which enables 
consumers to get a JMS ConnectionFactory.

## MoM API implementation

The MoM API implementation uses the ConnectionFactory service to get a JMS connection. It opens JMS sessions using that connection factory
and implements the methods in the API. JMS Support both Pub/Sub and Queue patterns in the MoM API without much additional work. 
The JMS sessions are single threaded, so care is taken not to share between threads or cause throughput issues with synchronization.

The Map of Map messages in the MoM API are serialised to Json using the Gson library and transmitted as Text messages. JMS Headers are currently
not used other than to identify the JSON encoding of the text payload.

The delivery of messages on Topics and Queues is entirely managed by AMQ with no additional code. The retry semantics of the QueueReader API
is achieved by dispatching JMS messages from within a JMS MessageListener onMessage method, and throwing an IllegalArgumentException to JMS
when a message needs to be re-queued. How retries work and the backoff algorithm used for messages that need to be retried is managed 
by ActiveMQ configuration which supports many scenarios for retrying messages.

## Delivery Retry for Queued messages

The semantics of the MoM API is that a consumer may throw an exception when its QueueReader.onMessage method is called. That indicates that the 
message could not be consumed at this time and should be retried. There are several ways that this can be achieved in general, and some 
AMQ specific ways. By default JMS ensures delivery order. Hence a message on the queue that is not dequeued, will block other messages on the queue 
until it is dequeued. AMQ deals with this by allowing a deployyer to configure queues to retry at the broker rather than attempting to redeliver in 
order to the same JMS consumer. The configuration is not default and has to be provided by configuring the AMQ broker.

    <broker xmlns="http://activemq.apache.org/schema/core"    schedulerSupport="true" >
        .... 
        <plugins>
            <redeliveryPlugin fallbackToDeadLetter="true" sendToDlqIfMaxRetriesExceeded="true">
                <redeliveryPolicyMap>
                    <redeliveryPolicyMap>
                        <redeliveryPolicyEntries>
                            <!-- a destination specific policy -->
                            <redeliveryPolicy queue="SpecialQueue" maximumRedeliveries="4" redeliveryDelay="10000" />
                        </redeliveryPolicyEntries>
                        <!-- the fallback policy for all other destinations -->
                        <defaultEntry>
                            <redeliveryPolicy maximumRedeliveries="4" initialRedeliveryDelay="5000" redeliveryDelay="10000" />
                        </defaultEntry>
                    </redeliveryPolicyMap>
                </redeliveryPolicyMap>
            </redeliveryPlugin>
        </plugins>
        
This can also be achieved in code, by dequeing all messages regardless of failiure or not. Those generate an exception on dequeued get requeued. If the size of the 
queue is so large as to significantly impact processing due to delays in processing the queue, then an alternative approach is to requeue to a special retry queue, ensuring
that retries get a higher level of priority. This may not be necessary, as retries happen due to unavailability, and if the queue is long, then resources will be
available, so no retry. If the queue is short, then the re-queue time is minimal. The approach is quite simular to the approach used in AMQ 5.7 and later, although
it will work with any JMS provider.

The code base is currently configured to use and explicit dequeue and requeue approach that does not depend on features of the JMS provider.