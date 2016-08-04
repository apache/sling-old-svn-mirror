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

import org.apache.sling.amq.ActiveMQConnectionFactoryService;
import org.apache.sling.amq.ActiveMQConnectionFactoryServiceTest;
import org.apache.sling.mom.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by ieb on 01/04/2016.
 */
public class JMSQueueManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSQueueManagerTest.class);
    private ActiveMQConnectionFactoryService amqConnectionFactoryService;
    private JMSQueueManager jmsQueueManager;
    private Map<String, Object> testMap;
    private boolean passed;
    private int ndeliveries;

    @Mock
    private ServiceReference<QueueReader> serviceReference;
    @Mock
    private Bundle bundle;
    @Mock
    private BundleContext bundleContext;
    private Map<String, Object> serviceProperties = new HashMap<String, Object>();

    public JMSQueueManagerTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void setUp() throws Exception {
        Mockito.when(serviceReference.getBundle()).thenReturn(bundle);
        Mockito.when(bundle.getBundleContext()).thenReturn(bundleContext);
        Mockito.when(serviceReference.getPropertyKeys()).thenAnswer(new Answer<String[]>() {
            @Override
            public String[] answer(InvocationOnMock invocationOnMock) throws Throwable {
                return (String[]) serviceProperties.keySet().toArray(new String[serviceProperties.size()]);
            }
        });
        Mockito.when(serviceReference.getProperty(Mockito.anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return serviceProperties.get(invocationOnMock.getArguments()[0]);
            }
        });
        amqConnectionFactoryService = ActiveMQConnectionFactoryServiceTest.activate(null);
        jmsQueueManager = JMSQueueManagerTest.activate(amqConnectionFactoryService);
        testMap = JsonTest.createTestMap();
        passed = false;

    }


    private static JMSQueueManager activate(ActiveMQConnectionFactoryService amqConnectionFactoryService) throws NoSuchFieldException, IllegalAccessException, JMSException {
        JMSQueueManager jmsQueueManager = new JMSQueueManager();
        setPrivate(jmsQueueManager, "connectionFactoryService", amqConnectionFactoryService);
        jmsQueueManager.activate(new HashMap<String, Object>());
        return jmsQueueManager;

    }

    private static void setPrivate(Object object, String name, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(name);
        if ( !field.isAccessible()) {
            field.setAccessible(true);
        }
        field.set(object, value);
    }


    @After
    public void after() throws JMSException {
        JMSQueueManagerTest.deactivate(jmsQueueManager);
        ActiveMQConnectionFactoryServiceTest.deactivate(amqConnectionFactoryService);
    }

    public static void deactivate(JMSQueueManager jmsQueueManager) throws JMSException {
        jmsQueueManager.deactivate(new HashMap<String, Object>());
    }

    @Test
    public void testQueue() throws JMSException, InterruptedException {
        // clean the queue out of messages from earlier tests, which may have failed.
        final String queueName = "testQueueReject";

        emptyQueue(queueName);
        // make the test map unique.
        testMap.put("testing", queueName + System.currentTimeMillis());
        jmsQueueManager.add(Types.queueName(queueName), testMap);

        checkMessagesInQueue(queueName, 1);

        ndeliveries = 0;
        QueueReader queueReader = new QueueReader() {
            @Override
            public void onMessage(Types.QueueName queueName, Map<String, Object> message) throws RequeueMessageException {
                ndeliveries++;
                JsonTest.checkEquals(testMap, message);
                passed = true;
            }
        };

        serviceProperties.clear();
        serviceProperties.put(QueueReader.QUEUE_NAME_PROP, queueName);

        Mockito.when(bundleContext.getService(Mockito.eq(serviceReference))).thenReturn(queueReader);
        jmsQueueManager.addReader(serviceReference);




        waitForPassed(1000);
        checkMessagesInQueue(queueName, 0);
        waitForErrors(1000);

        jmsQueueManager.removeReader(serviceReference);
        assertEquals(1, ndeliveries);


    }


    private void waitForErrors(long t) throws InterruptedException {
        Thread.sleep(t);
    }

    private boolean waitForPassed(long t) {
        long end = System.currentTimeMillis() + t;
        while(System.currentTimeMillis() < end) {
            if (passed) {
                return true;
            } else {
                Thread.yield();
            }
        }
        LOGGER.info("Message not received after " + t + " ms");
        return false;
    }

    private void checkMessagesInQueue(String name, int expected) throws JMSException {
        Connection connection = amqConnectionFactoryService.getConnectionFactory().createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(name);
        QueueBrowser browser = session.createBrowser(queue);
        int n = 0;
        for(Enumeration e = browser.getEnumeration(); e.hasMoreElements(); ) {
            Message m = (Message) e.nextElement();
            LOGGER.info("Message at {} is {} ", n,m);
            n++;
        }
        browser.close();
        session.close();
        connection.stop();
        assertEquals(expected, n);
    }

    @Test
    public void testQueueReject() throws JMSException, InterruptedException {
        // clean the queue out of messages from earlier tests, which may have failed.
        final String queueName = "testQueueReject";
        emptyQueue(queueName);
        // make the test map unique, if the dequeue fails, then the message wont be the first.
        testMap.put("testing", queueName + System.currentTimeMillis());
        LOGGER.info("Sending message to queue");
        jmsQueueManager.add(Types.queueName(queueName), testMap);
        LOGGER.info("Sent message to queue ... receiving from queue");

        checkMessagesInQueue(queueName, 1);

        ndeliveries = 0;
        QueueReader queueReader = new QueueReader() {
            @Override
            public void onMessage(Types.QueueName queueName, Map<String, Object> message) throws RequeueMessageException {
                JsonTest.checkEquals(testMap, message);
                ndeliveries++;
                if ( ndeliveries == 1) {
                    LOGGER.info("Requesting requeue of message");
                    throw new RequeueMessageException("Requeing");
                } else if ( ndeliveries == 2) {
                    LOGGER.info("Got message, accepting with no retry.");
                    passed = true;
                } else if ( ndeliveries > 2) {
                    fail("Multiple delivered");
                }
            }
        };

        serviceProperties.clear();
        serviceProperties.put(QueueReader.QUEUE_NAME_PROP, queueName);

        Mockito.when(bundleContext.getService(Mockito.eq(serviceReference))).thenReturn(queueReader);
        jmsQueueManager.addReader(serviceReference);



        waitForPassed(30000);

        jmsQueueManager.removeReader(serviceReference);
        checkMessagesInQueue(queueName, 0);
        assertEquals(2, ndeliveries);


    }

    private void dumpQueue(String name) throws JMSException {
        Connection connection = amqConnectionFactoryService.getConnectionFactory().createConnection();
        connection.start();
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue queue = session.createQueue(name);
        QueueBrowser browser = session.createBrowser(queue);
        LOGGER.info("Starting to dump queue {} ", name);
        int n = 0;
        for ( Enumeration messages = browser.getEnumeration();  messages.hasMoreElements(); ) {
            Message m = (Message) messages.nextElement();
            LOGGER.info("Message at {}  is {} ", n, m);
            n++;
        }
        LOGGER.info("Done dump queue {} ", name);
        browser.close();
        session.close();
        connection.stop();

    }

    private void emptyQueue(String name) throws JMSException {
        dumpQueue(name);
        Connection connection = amqConnectionFactoryService.getConnectionFactory().createConnection();
        connection.start();
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue queue = session.createQueue(name);
        MessageConsumer consumer = session.createConsumer(queue);

        for (;;) {
            Message m = consumer.receive(100);
            if ( m == null) {
                LOGGER.info("No more messages in queue {} ", name);
                break;
            }
            LOGGER.info("Got message  {}",m);
            m.acknowledge();
            session.commit();
        }
        boolean shouldFail = false;
        QueueBrowser browser = session.createBrowser(queue);
        for ( Enumeration messages = browser.getEnumeration(); messages.hasMoreElements(); ) {
            Message m = (Message) messages.nextElement();
            LOGGER.info("Queued message {} ", m);
            shouldFail = true;
        }
        browser.close();
        if ( shouldFail) {
            fail("Queue was not emptied as expected");
        }
        consumer.close();
        session.close();
        connection.stop();
    }

}