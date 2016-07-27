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
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ieb on 30/03/2016.
 * This class provides support for sending messages to topics over JMS and subscribing to topics. It uses the ConnectionFactoryService
 * to interact with JMS. There is nothing in
 */
@Component(immediate = true)
@Service(value = TopicManager.class)
public class JMSTopicManager implements TopicManager {


    private static final Logger LOGGER = LoggerFactory.getLogger(JMSTopicManager.class);


    /**
     * Holds all QueueReader registrations.
     */
    @Reference(referenceInterface = Subscriber.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            bind="addSubscriber",
            unbind="removeSubscriber")
    private final Map<ServiceReference<Subscriber>, SubscriberHolder> registrations =
            new ConcurrentHashMap<ServiceReference<Subscriber>, SubscriberHolder>();

    @Reference
    private ConnectionFactoryService connectionFactoryService;
    // A single connection is maintained open per instance of this component.
    private Connection connection;
    // A single session is used for listening to messages. Separate sessions are opened for sending to avoid synchronisation on sending operations.
    private Session session;
    private final Object lock = new Object();

    @Activate
    public synchronized  void activate(Map<String, Object> properties) throws JMSException {
            connection = connectionFactoryService.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            connection.start();
    }

    @Deactivate
    public synchronized  void deactivate(Map<String, Object> properties) throws JMSException {
        for ( Map.Entry<ServiceReference<Subscriber>, SubscriberHolder> e : registrations.entrySet()) {
            e.getValue().close();
        }
        registrations.clear();
        // don't close the session, there is a bug in JMS which means an already closed session wont go quietly
        // and the hook that shutsdown an embedded connection still gets fired when OSGi shutsdown even with
        // a flag to prevent it. connection.stop and close are clean.
        connection.stop();
        connection.close();
    }


    @Override
    public void publish(Types.TopicName name, Types.CommandName commandName, Map<String, Object> message) {
        Session session = null;
        try {
            // use a fresh session per message.
            session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            TextMessage textMessage = session.createTextMessage(Json.toJson(message));
            textMessage.setJMSType(JMSMessageTypes.JSON.toString());
            session.createProducer(session.createTopic(name.toString())).send(textMessage);
            session.commit();
            session.close();
        } catch (JMSException e) {
            LOGGER.error("Unable to send message to queue "+name, e);
            if(session != null) {
                try {
                    session.close();
                } catch (JMSException e1) {
                    LOGGER.warn("Unable to close session ",e1);
                }
            }

        }
    }


    // Register Subscribers using OSGi Whiteboard pattern
    public synchronized  void addSubscriber(ServiceReference<Subscriber> serviceRef) {
        if (registrations.containsKey(serviceRef)) {
            LOGGER.error("Registration for service reference is already present {}",serviceRef);
            return;
        }
        SubscriberHolder subscriberHolder = new SubscriberHolder(session, serviceRef.getBundle().getBundleContext().getService(serviceRef), getServiceProperties(serviceRef));
        registrations.put(serviceRef, subscriberHolder);
    }

    private Map<String, Object> getServiceProperties(ServiceReference<Subscriber> serviceRef) {
        Map<String, Object> m = new HashMap<String, Object>();
        for ( String k : serviceRef.getPropertyKeys()) {
            m.put(k, serviceRef.getProperty(k));
        }
        return Collections.unmodifiableMap(m);
    }

    public synchronized void removeSubscriber(ServiceReference<Subscriber> serviceRef) {
        SubscriberHolder subscriberHolder = registrations.remove(serviceRef);
        if (subscriberHolder != null) {
            subscriberHolder.close();
        }
    }

    private static class SubscriberHolder implements Closeable {


        private final FilteredTopicSubscriber filteredTopicSubscriber;

        public SubscriberHolder(Session session, Subscriber subscriber, Map<String, Object> properties) {
            try {
                LOGGER.info("Creating Subscriber holder for {} ", subscriber.getClass());
                String[] topicNames = (String[]) properties.get(Subscriber.TOPIC_NAMES_PROP);

                if ( topicNames == null || topicNames.length == 0) {
                    throw new IllegalArgumentException("At least one valid topic name in property " + Subscriber.TOPIC_NAMES_PROP + " is required for Subscriber registration");
                }
                if ( subscriber instanceof MessageFilter) {
                    filteredTopicSubscriber = new FilteredTopicSubscriber(session, subscriber, topicNames, (MessageFilter)subscriber);
                } else {
                    filteredTopicSubscriber = new FilteredTopicSubscriber(session, subscriber, topicNames, new MessageFilter() {


                        @Override
                        public boolean accept(Types.Name name, Map<String, Object> mapMessage) {
                            return true;
                        }
                    });

                }
            } catch (JMSException e) {
                throw new IllegalArgumentException("Unable to register QueueReader with JMS ",e);
            }

        }

        public void close() {
            try {
                filteredTopicSubscriber.close();
            } catch (IOException e) {
                LOGGER.warn("Unable to close topic subscriber {} ", e);
            }
        }
    }

    /**
     * This listens to topic messages, and applies the message filter prior to sending to the subscriber.
     * Although JMS has its own filtering language, this is JMS specific and since we don't want to expose implementation
     * details in the JOBs API either explicitly or out of band, the JMS specific filters cant be used. As a replacement the
     * API provides the MessageFilter API.
     */
    private static final class FilteredTopicSubscriber implements Closeable, MessageListener {
        private final Subscriber subscriber;
        private final MessageFilter filter;
        private final List<MessageConsumer> consumers = new ArrayList<MessageConsumer>();

        public FilteredTopicSubscriber(@Nonnull Session session,
                                       @Nonnull Subscriber subscriber,
                                       @Nonnull String[] topicNames,
                                       @Nonnull MessageFilter filter) throws JMSException {
            this.subscriber = subscriber;
            this.filter = filter;
            for (String t : topicNames) {
                MessageConsumer c = session.createConsumer(session.createTopic(t));
                c.setMessageListener(this);
                consumers.add(c);
            }
        }


        @Override
        public void onMessage(Message message) {
            try {
                LOGGER.info("Got message {} ", message);
                Destination destination = message.getJMSDestination();
                if (destination instanceof Topic) {
                    Topic topic = (Topic) destination;
                    String type = message.getJMSType();
                    if (JMSMessageTypes.JSON.equals(JMSMessageTypes.valueOf(type))) {
                        TextMessage textMessage = (TextMessage) message;
                        Map<String, Object> mapMessage = Json.toMap(textMessage.getText());
                        Types.TopicName topicName = Types.topicName(topic.getTopicName());
                        if ( filter.accept(topicName, mapMessage) ) {
                            subscriber.onMessage(topicName, mapMessage);
                        }
                    }
                }
            } catch (JMSException e) {
                LOGGER.warn("Failed to deliver message ",e);
            }
        }

        @Override
        public void close() throws IOException {
            for (MessageConsumer c : consumers) {
                try {
                    LOGGER.info("Closing consumer on dispose {} ",c);
                    c.close();
                } catch (JMSException e) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
        }
    }



}
