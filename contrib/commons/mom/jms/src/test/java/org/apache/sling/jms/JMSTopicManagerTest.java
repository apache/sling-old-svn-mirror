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

import javax.jms.JMSException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by ieb on 31/03/2016.
 */
public class JMSTopicManagerTest {

    private static final long MESSAGE_LATENCY = 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(JMSTopicManagerTest.class);
    private JMSTopicManager jsmTopicManager;
    private ActiveMQConnectionFactoryService amqConnectionFactoryService;
    private Map<String, Object> testMap;
    private boolean passed;
    private long lastSent;
    @Mock
    private ServiceReference<Subscriber> serviceReference;
    @Mock
    private Bundle bundle;
    @Mock
    private BundleContext bundleContext;
    private Map<String, Object> serviceProperties = new HashMap<String, Object>();

    public  JMSTopicManagerTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void before() throws NoSuchFieldException, IllegalAccessException, JMSException {
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
        jsmTopicManager = JMSTopicManagerTest.activate(amqConnectionFactoryService);
        testMap = JsonTest.createTestMap();
        passed = false;
    }

    public static JMSTopicManager activate(ActiveMQConnectionFactoryService amqConnectionFactoryService) throws NoSuchFieldException, IllegalAccessException, JMSException {
        JMSTopicManager jsmTopicManager = new JMSTopicManager();
        setPrivate(jsmTopicManager, "connectionFactoryService", amqConnectionFactoryService);
        jsmTopicManager.activate(new HashMap<String, Object>());
        return jsmTopicManager;

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
        JMSTopicManagerTest.deactivate(jsmTopicManager);
        ActiveMQConnectionFactoryServiceTest.deactivate(amqConnectionFactoryService);
    }

    public static void deactivate(JMSTopicManager jsmTopicManager) throws JMSException {
        jsmTopicManager.deactivate(new HashMap<String, Object>());
    }


    /**
     * Test a working publish operation, read the message and check all ok. Will try and read the message for 1s. Normally messages
     * arrive within 15ms.
     * @throws Exception
     */
    @Test
    public void testPublish() throws Exception {
        // make the test map unique.
        testMap.put("testing", "testPublish" + System.currentTimeMillis());

        addSubscriber(new String[]{"testtopic"}, true);

        jsmTopicManager.publish(Types.topicName("testtopic"), Types.commandName("testcommand"), testMap);
        lastSent = System.currentTimeMillis();
        assertTrue(waitForPassed(MESSAGE_LATENCY));

        removeSubscriber();
    }


    private void addSubscriber(String[] topics, boolean match) {

        Subscriber subscriber = new TestingSubscriber(this, match, topics);

        serviceProperties.clear();
        serviceProperties.put(Subscriber.TOPIC_NAMES_PROP, topics);

        Mockito.when(bundleContext.getService(Mockito.eq(serviceReference))).thenReturn(subscriber);
        jsmTopicManager.addSubscriber(serviceReference);

    }

    private void removeSubscriber() {
        jsmTopicManager.removeSubscriber(serviceReference);
    }


    /**
     * Test that a message sent with the wrong topic doesn't arrive, filtered by the topic inside the jmsTopicManager.
     * @throws Exception
     */
    @Test
    public void testFilterdByTopic() throws Exception {
        // make the test map unique.
        testMap.put("testing", "testFilterdByTopic" + System.currentTimeMillis());
        addSubscriber(new String[]{"testtopic"}, false);

        lastSent = System.currentTimeMillis();
        assertFalse(waitForPassed(MESSAGE_LATENCY)); // not expecting a message at all

        removeSubscriber();
    }

    /**
     * Check that a message sent to the correct topic is filtered by the MessageFilter.
     * The test waits 1s for the message to arrive. If testPublish does not fail, message
     * latency is < 1s.
     * @throws Exception
     */
    @Test
    public void testFilterdByFilter() throws Exception {
        // make the test map unique.
        testMap.put("testing", "testFilterdByFilter" + System.currentTimeMillis());
        addSubscriber(new String[]{"testtopic"}, false);

        jsmTopicManager.publish(Types.topicName("testtopic"), Types.commandName("testcommand"), testMap);
        lastSent = System.currentTimeMillis();
        assertFalse(waitForPassed(MESSAGE_LATENCY)); // not expecting a message at all

        removeSubscriber();
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
        LOGGER.info("Message not recieved after "+t+" ms");
        return false;
    }


    private static class TestingSubscriber implements Subscriber, MessageFilter {
        private JMSTopicManagerTest test;
        private final boolean accept;
        private final Set<Types.Name> topicnames;

        public TestingSubscriber(JMSTopicManagerTest test, boolean accept, String[] topicname) {
            this.test = test;
            this.accept = accept;
            this.topicnames = new HashSet<Types.Name>();
            for(String t : topicname) {
                topicnames.add(Types.topicName(t));
            }
        }

        @Override
        public void onMessage(Types.TopicName topic, Map<String, Object> message) {
            LOGGER.info("Got message in "+(System.currentTimeMillis()-test.lastSent)+" ms");
            JsonTest.checkEquals(test.testMap, message);
            test.passed = true;
        }

        @Override
        public boolean accept(Types.Name name, Map<String, Object> mapMessage) {
            return topicnames.contains(name) == accept;
        }


    }
}