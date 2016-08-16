/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jms;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.mom.*;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jms.*;
import java.io.Closeable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ieb on 30/03/2016.
 * A JMS implementation of a QueueManager. It will allow callers to add messages to named queues, and consumers to read
 * messages from named queues in order. The component uses a single connection to the JMS broker, but dedicated sessions
 * for each send and for each Queue reader.
 */
@Component(immediate = true)
@Service(value = QueueManager.class)
public class JMSQueueManager implements QueueManager {



    private static final Logger LOGGER = LoggerFactory.getLogger(JMSQueueManager.class);
    private static final String NRETRIES = "_nr";

    @Reference
    private ConnectionFactoryService connectionFactoryService;


    /**
     * Holds all QueueReader registrations.
     */
    @Reference(referenceInterface = QueueReader.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            bind="addReader",
            unbind="removeReader")
    private final Map<ServiceReference<QueueReader>, QueueReaderHolder> registrations =
            new ConcurrentHashMap<ServiceReference<QueueReader>, QueueReaderHolder>();

    private Connection connection;

    @Activate
    public synchronized void activate(Map<String, Object> properties) throws JMSException {
        connection = connectionFactoryService.getConnectionFactory().createConnection();
        connection.start();
    }

    @Deactivate
    public synchronized void deactivate(Map<String, Object> properties) throws JMSException {
        for ( Map.Entry<ServiceReference<QueueReader>, QueueReaderHolder> e : registrations.entrySet()) {
            e.getValue().close();
        }
        registrations.clear();
        connection.stop();
        connection.close();
    }



    /**
     * Add a message to the queue. The message is added to the queue transactionally and auto acknowledged.
     * @param name the name of the queue.
     * @param message the message to post to the queue.
     */
    @Override
    public void add(@Nonnull Types.QueueName name, @Nonnull Map<String, Object> message) {
        Session session = null;
        try {
            session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            message.put(NRETRIES, 0L); // set the number of retries to 0.
            TextMessage textMessage = session.createTextMessage(Json.toJson(message));
            textMessage.setJMSType(JMSMessageTypes.JSON.toString());
            LOGGER.info("Sending to {} message {} ", name, textMessage);
            session.createProducer(session.createQueue(name.toString())).send(textMessage);
            session.commit();
            session.close();
        } catch (JMSException e) {
            LOGGER.error("Unable to send message to queue "+name, e);
            close(session);

        }

    }


    /**
     * quietly close the session.
     * @param session
     */
    private void close(Session session) {
        if(session != null) {
            try {
                session.close();
            } catch (JMSException e) {
                LOGGER.warn("Unable to close session ",e);
            }
        }
    }


    // Register Readers using OSGi Whiteboard pattern
    public synchronized  void addReader(ServiceReference<QueueReader> serviceRef) {
        if (registrations.containsKey(serviceRef)) {
            LOGGER.error("Registration for service reference is already present {}",serviceRef);
            return;
        }
        QueueReaderHolder queueReaderHolder = new QueueReaderHolder(connection, serviceRef.getBundle().getBundleContext().getService(serviceRef), getServiceProperties(serviceRef));
        registrations.put(serviceRef, queueReaderHolder);
    }

    private Map<String, Object> getServiceProperties(ServiceReference<QueueReader> serviceRef) {
        Map<String, Object> m = new HashMap<String, Object>();
        for ( String k : serviceRef.getPropertyKeys()) {
            m.put(k, serviceRef.getProperty(k));
        }
        return Collections.unmodifiableMap(m);
    }

    public synchronized void removeReader(ServiceReference<QueueReader> serviceRef) {
        QueueReaderHolder queueReaderHolder = registrations.remove(serviceRef);
        if ( queueReaderHolder != null) {
            queueReaderHolder.close();
        }
    }

    private static class QueueReaderHolder implements Closeable {
        private final JMSQueueSession session;

        public QueueReaderHolder(Connection connection, QueueReader queueReader, Map<String, Object> properties) {
            try {
                LOGGER.info("Creating Queue holder for {} ", queueReader.getClass());
                String name = (String) properties.get(QueueReader.QUEUE_NAME_PROP);
                checkNotNull(name, "A valid queue name as property " + QueueReader.QUEUE_NAME_PROP + " is required for QueueReader registration");
                if (queueReader instanceof MessageFilter) {
                    session = new JMSQueueSession(connection, queueReader, name, (MessageFilter) queueReader, true, 5);
                } else {
                    session = new JMSQueueSession(connection, queueReader, name, new MessageFilter() {
                        @Override
                        public boolean accept(Types.Name name, Map<String, Object> mapMessage) {
                            return true;
                        }

                    }, true, 5);

                }
            } catch (JMSException e) {
                throw new IllegalArgumentException("Unable to register QueueReader with JMS ",e);
            }

        }

        public void close() {
            session.close();
        }
    }

    private static void checkNotNull(Object v, String message) {
        if ( v == null) {
            throw new IllegalArgumentException(message);
        }
    }


    public static class JMSQueueSession implements Closeable, MessageListener {
        private static final Logger LOGGER = LoggerFactory.getLogger(JMSQueueSession.class);
        private final QueueReader queueReader;
        private final String name;
        private final MessageFilter messageFilter;
        private final Session session;
        private final MessageConsumer queueConsumer;
        private final MessageProducer queueProducer;
        private boolean retryByRequeue;
        private int maxRetries;

        public JMSQueueSession(Connection connection, QueueReader queueReader, String name,  MessageFilter messageFilter, boolean retryByRequeue, int maxRetries) throws JMSException {
            this.queueReader = queueReader;
            this.name = name;
            this.messageFilter = messageFilter;
            this.retryByRequeue = retryByRequeue;
            this.maxRetries = maxRetries;
            session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(name);
            queueConsumer = session.createConsumer(queue);
            queueProducer = session.createProducer(queue);
            queueConsumer.setMessageListener(this);
        }

        @Override
        public void close() {
            if ( queueConsumer != null) {
                try {
                    queueConsumer.close();
                } catch (JMSException e) {
                    LOGGER.warn("Failed to close queue consumer on "+name,e);
                }
            }
            if ( session != null) {
                try {
                    session.close();
                } catch (JMSException e) {
                    LOGGER.warn("Failed to close queue session on " + name, e);
                }
            }

        }

        @Override
        public void onMessage(Message message) {
            boolean committed = false;
            TextMessage textMessage = null;
            try {
                try {
                    LOGGER.info("Got from {} message {} ", name, message);
                    Destination destination = message.getJMSDestination();
                    if (destination instanceof Queue) {
                        Queue queue = (Queue) destination;
                        if ( JMSMessageTypes.JSON.equals(JMSMessageTypes.valueOf(message.getJMSType()))) {
                            textMessage = (TextMessage) message;
                            final Map<String, Object> mapMessage = Json.toMap(textMessage.getText());
                            Types.QueueName queueName = Types.queueName(queue.getQueueName());
                            if (queueName.equals(name) && messageFilter.accept(queueName, mapMessage)) {
                                queueReader.onMessage(queueName, mapMessage);
                                session.commit();
                                // all ok.
                                committed = true;
                                return;
                            }
                        }
                    }
                } catch (RequeueMessageException e) {
                    LOGGER.info("QueueReader requested requeue of message ", e);
                    if (retryByRequeue && textMessage != null) {
                        Map<String, Object> mapMessage = Json.toMap(textMessage.getText());
                        if ((int)mapMessage.get(NRETRIES) < maxRetries) {
                            mapMessage.put(NRETRIES, ((long) mapMessage.get(NRETRIES)) + 1);
                            TextMessage retryMessage = session.createTextMessage(Json.toJson(mapMessage));
                            retryMessage.setJMSType(JMSMessageTypes.JSON.toString());
                            LOGGER.info("Retrying message Sending to {} message {} ", name, textMessage);
                            queueProducer.send(retryMessage);
                            session.commit();
                            committed = true;
                            return;
                        }
                    }
                }
            } catch (JMSException e) {
                LOGGER.info("Receive failed leaving to provider to require if required. ", e);
            } finally {
                try {
                    if (!committed) {
                        session.rollback();
                    }
                } catch (JMSException e) {
                    LOGGER.info("QueueReader rollback failed. ",e);
                }
            }
            // If onMessage throws an exception JMS will put the message back onto the queue.
            // the delay before it gets retried is a JMS server configuration.
            throw new IllegalArgumentException("Unable to process message, requeue");
        }

    }

}
