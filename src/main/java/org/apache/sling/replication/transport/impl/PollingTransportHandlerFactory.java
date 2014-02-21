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
package org.apache.sling.replication.transport.impl;

import java.util.Dictionary;
import java.util.Map;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.http.client.fluent.Executor;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProviderFactory;
import org.apache.sling.replication.transport.authentication.impl.UserCredentialsTransportAuthenticationProviderFactory;
import org.osgi.framework.BundleContext;

@Component(metatype = true,
        label = "Replication Transport Handler Factory - Http Poll",
        description = "OSGi configuration based PollingTransportHandler service factory",
        name = PollingTransportHandlerFactory.SERVICE_PID,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
public class PollingTransportHandlerFactory extends AbstractTransportHandlerFactory {
    static final String SERVICE_PID = "org.apache.sling.replication.transport.impl.PollingTransportHandlerFactory";

    private static final String DEFAULT_AUTHENTICATION_FACTORY = "(name=" + UserCredentialsTransportAuthenticationProviderFactory.TYPE + ")";

    @Property(boolValue = true)
    private static final String ENABLED = "enabled";

    @Property
    private static final String NAME = "name";

    @Property(cardinality = 1000)
    private static final String ENDPOINT = ReplicationAgentConfiguration.ENDPOINT;

    @Property(name = ReplicationAgentConfiguration.TRANSPORT_AUTHENTICATION_FACTORY, value = DEFAULT_AUTHENTICATION_FACTORY)
    @Reference(name = "TransportAuthenticationProviderFactory", target = DEFAULT_AUTHENTICATION_FACTORY, policy = ReferencePolicy.DYNAMIC)
    private TransportAuthenticationProviderFactory transportAuthenticationProviderFactory;

    @Property
    private static final String AUTHENTICATION_PROPERTIES = ReplicationAgentConfiguration.AUTHENTICATION_PROPERTIES;

    @Property(name = "poll items", description = "number of subsequent poll requests to make", intValue = -1)
    private static final String POLL_ITEMS = "poll.items";


    protected TransportHandler createTransportHandler(Map<String, ?> config,
                                                      Dictionary<String, Object> props,
                                                      TransportAuthenticationProvider transportAuthenticationProvider,
                                                      ReplicationEndpoint[] endpoints, TransportEndpointStrategyType endpointStrategyType) {
        int pollItems = PropertiesUtil.toInteger(config.get(POLL_ITEMS), -1);
        props.put(POLL_ITEMS, pollItems);



        return new PollingTransportHandler(pollItems,
                (TransportAuthenticationProvider<Executor, Executor>) transportAuthenticationProvider,
                endpoints);
    }

    @Override
    protected TransportAuthenticationProviderFactory getAuthenticationFactory() {
        return transportAuthenticationProviderFactory;
    }

    @Activate
    protected void activate(BundleContext context, Map<String, ?> config) throws Exception {
        super.activate(context, config);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }
}
