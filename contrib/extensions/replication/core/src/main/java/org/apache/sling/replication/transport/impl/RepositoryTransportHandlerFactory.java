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

import org.apache.felix.scr.annotations.*;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProviderFactory;
import org.apache.sling.replication.transport.authentication.impl.RepositoryTransportAuthenticationProviderFactory;
import org.osgi.framework.BundleContext;

import javax.jcr.Session;
import java.util.Dictionary;
import java.util.Map;

@Component(metatype = true,
        label = "Replication Transport Handler Factory - Repository",
        description = "OSGi configuration based RepositoryTransportHandler service factory",
        name = RepositoryTransportHandlerFactory.SERVICE_PID,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
public class RepositoryTransportHandlerFactory extends AbstractTransportHandlerFactory {
    static final String SERVICE_PID = "org.apache.sling.replication.transport.impl.RepositoryTransportHandlerFactory";

    private static final String DEFAULT_AUTHENTICATION_FACTORY = "(name=" + RepositoryTransportAuthenticationProviderFactory.TYPE + ")";


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


    @Reference
    private SlingRepository repository;

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    @Override
    protected TransportHandler createTransportHandler(Map<String, ?> config,
                                                      Dictionary<String, Object> props,
                                                      TransportAuthenticationProvider transportAuthenticationProvider,
                                                      ReplicationEndpoint[] endpoints, TransportEndpointStrategyType endpointStrategyType) {


        return new RepositoryTransportHandler(repository,
                replicationEventFactory,
                (TransportAuthenticationProvider<SlingRepository,Session>) transportAuthenticationProvider,
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
