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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by ieb on 31/03/2016.
 */
public class ActiveMQConnectionFactoryServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQConnectionFactoryServiceTest.class);

    @Test
    public void testGetConnectionFactory() throws Exception {
        LOGGER.info("Starting test");
        ActiveMQConnectionFactoryService cfs = ActiveMQConnectionFactoryServiceTest.activate(null);
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
        cfs.deactivate(new HashMap<String, Object>());
    }

    @Nonnull
    public static ActiveMQConnectionFactoryService activate(@Nullable Map<String, Object> props) {
        ActiveMQConnectionFactoryService amqConnectionFactoryService = new ActiveMQConnectionFactoryService();
        if ( props == null ) {
            props = new HashMap<String, Object>();
            props.put(ActiveMQConnectionFactoryService.BROKER_URI, ActiveMQConnectionFactoryService.DEFAULT_BROKER_URI);
        }
        amqConnectionFactoryService.activate(props);
        return amqConnectionFactoryService;
    }

}