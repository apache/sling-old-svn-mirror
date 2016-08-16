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

import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jms.ConnectionFactoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.jms.ConnectionFactory;

/**
 * Creates a ConnectionFactoryService that makes a pooled  JMS ConnectionFactory available to consumers. The implementation
 * of JMS is provided by ActiveMQ in this instances. If the component is left un-configured it will connection to vm://localhost:6161.
 * If no server is present at that address, the component will create a standalone ActiveMQ broker on startup. Without additional
 * configuration that AMQ Broker will operate standalone. With configuration it is possible to configure the broker to become
 * a member of a multi master AMQ Broker network. Alternatively if a dedicated AMQ Broker is required the jms.brokerUrl configuration
 * setting should be adjusted to point to that broker.
 *
 * This component works OOTB and in Unit tests with no further action.
 *
 * The jms.brokerUrl allows configuration of the broker in any way that ActiveMQ allows, including xbean and broker.
 *
 *
 * Available URI patterns.
 *
 * xbean:org/apache/sling/amq/activemq.xml will result in the Broker searching for org/apache/sling/amq/activemq.xml in
 * the classpath and using that to configure the Borker, see http://activemq.apache.org/xml-configuration.html for details
 * of the format. See that location for an example of the default configuration.
 *
 *
 *
 * broker:tcp://localhost:61616 will create a broker on localhost port 61616 using the URI configuration format.
 * See http://activemq.apache.org/broker-configuration-uri.html and http://activemq.apache.org/broker-uri.html for
 * details of the format.
 *
 * properties:/foo/bar.properties uses a properties file as per http://activemq.apache.org/broker-properties-uri.html
 *
 */
@Component(immediate = true, metatype = true)
@Service(value=ConnectionFactoryService.class)
public class ActiveMQConnectionFactoryService implements ConnectionFactoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQConnectionFactoryService.class);
    private PooledConnectionFactory pooledConnectionFactory;

    // Where the broker is configured out of the box, the shutdown hook must be disabled.
    // so that the deactivate method can perform the shutdown.
    // This assumes that OSGi does shutdown properly.

    public static final String DEFAULT_BROKER_URI = "vm://localhost:61616?broker.useShutdownHook=false";
    @Property(value = DEFAULT_BROKER_URI)
    public static final String BROKER_URI = "jms.brokerUri";




    @Activate
    public void activate(Map<String, Object> props) {

        String brokerURL = (String) props.get(BROKER_URI);

        pooledConnectionFactory = new PooledConnectionFactory(brokerURL);
        pooledConnectionFactory.start();
    }


    @Deactivate
    public void deactivate(Map<String, Object> props) {

        LOGGER.info("Stopping ActiveMQ Pooled connection factory");
        pooledConnectionFactory.stop();
        pooledConnectionFactory = null;
    }



    @Override
    public ConnectionFactory getConnectionFactory() {
        return pooledConnectionFactory;
    }

}