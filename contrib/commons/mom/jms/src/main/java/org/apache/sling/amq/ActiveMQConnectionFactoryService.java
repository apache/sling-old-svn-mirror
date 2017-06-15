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

import javax.jms.ConnectionFactory;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.sling.jms.ConnectionFactoryService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Component(immediate = true,
           configurationPolicy=ConfigurationPolicy.REQUIRE,
           service = ConnectionFactoryService.class)
@Designate(ocd = ActiveMQConnectionFactoryService.Config.class)
public class ActiveMQConnectionFactoryService implements ConnectionFactoryService {

    public static final String DEFAULT_BROKER_URI = "vm://localhost:61616?broker.useShutdownHook=false";

    @ObjectClassDefinition(name="Apache Sling Active MQ Connection Factory",
            description="Connection factory for Active MQ")
    public @interface Config {
        // Where the broker is configured out of the box, the shutdown hook must be disabled.
        // so that the deactivate method can perform the shutdown.
        // This assumes that OSGi does shutdown properly.
        @AttributeDefinition(name = "Broker URI", description="The URI to the broker.")
        String jms_brokerUri() default DEFAULT_BROKER_URI;
    }
    private final Logger LOGGER = LoggerFactory.getLogger(ActiveMQConnectionFactoryService.class);
    private PooledConnectionFactory pooledConnectionFactory;


    @Activate
    public void activate(Config config) {
        pooledConnectionFactory = new PooledConnectionFactory(config.jms_brokerUri());
        pooledConnectionFactory.start();
    }


    @Deactivate
    public void deactivate() {
        LOGGER.info("Stopping ActiveMQ Pooled connection factory");
        pooledConnectionFactory.stop();
        pooledConnectionFactory = null;
    }

    @Override
    public ConnectionFactory getConnectionFactory() {
        return pooledConnectionFactory;
    }
}