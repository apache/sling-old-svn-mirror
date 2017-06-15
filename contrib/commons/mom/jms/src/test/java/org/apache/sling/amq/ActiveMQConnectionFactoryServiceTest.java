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

package org.apache.sling.amq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.annotation.Nonnull;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ActiveMQConnectionFactoryServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQConnectionFactoryServiceTest.class);

    @Test
    public void testGetConnectionFactory() throws Exception {
        LOGGER.info("Starting test");
        ActiveMQConnectionFactoryService cfs = ActiveMQConnectionFactoryServiceTest.activate();
        ConnectionFactory cf = cfs.getConnectionFactory();
        Connection connection = cf.createConnection();
        Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
        Topic t = session.createTopic("testTopic");
        MessageConsumer consumer = session.createConsumer(t);
        LOGGER.info("Starting connection");
        connection.start();
        LOGGER.info("Connection started.. sending message");
        session.createProducer(t).send(session.createTextMessage("testing with a message"));
        session.commit();
        LOGGER.info("Message sent ... receiving message");
        Message m = consumer.receive();
        LOGGER.info("Message received");
        assertTrue(m instanceof TextMessage);
        assertEquals("testing with a message", ((TextMessage)m).getText());
        session.close();
        connection.stop();

        deactivate(cfs);
    }

    public static void deactivate(@Nonnull ActiveMQConnectionFactoryService cfs) {
        cfs.deactivate();
    }

    @Nonnull
    public static ActiveMQConnectionFactoryService activate() {
        ActiveMQConnectionFactoryService amqConnectionFactoryService = new ActiveMQConnectionFactoryService();
        final ActiveMQConnectionFactoryService.Config config = Mockito.mock(ActiveMQConnectionFactoryService.Config.class);
        Mockito.when(config.jms_brokerUri()).thenReturn(ActiveMQConnectionFactoryService.DEFAULT_BROKER_URI);
        amqConnectionFactoryService.activate(config);
        return amqConnectionFactoryService;
    }

}