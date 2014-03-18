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
import org.apache.http.client.fluent.Executor;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProviderFactory;
import org.apache.sling.replication.transport.authentication.impl.UserCredentialsTransportAuthenticationProviderFactory;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.Map;

@Component(metatype = true,
        label = "Replication Transport Handler Factory - Http Push",
        description = "OSGi configuration based HttpTransportHandler service factory",
        name = HttpTransportHandlerFactory.SERVICE_PID,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
public class HttpTransportHandlerFactory extends AbstractTransportHandlerFactory {
    static final String SERVICE_PID = "org.apache.sling.replication.transport.impl.HttpTransportHandlerFactory";

    private static final String DEFAULT_AUTHENTICATION_FACTORY = "(name=" + UserCredentialsTransportAuthenticationProviderFactory.TYPE + ")";

    @Property(boolValue = true)
    private static final String ENABLED = "enabled";

    @Property
    private static final String NAME = "name";

    @Property(cardinality = 1000)
    private static final String ENDPOINT = ReplicationAgentConfiguration.ENDPOINT;

    @Property(options = {
            @PropertyOption(name = "All",
                    value = "all endpoints"
            ),
            @PropertyOption(name = "OneSuccessful",
                value = "one successful endpoint"
            ),
            @PropertyOption(name = "FirstSuccessful",
                    value = "first successful endpoint"
            )},
            value = "All"
    )
    private static final String ENDPOINT_STRATEGY = ReplicationAgentConfiguration.ENDPOINT_STRATEGY;

    @Property(name = ReplicationAgentConfiguration.TRANSPORT_AUTHENTICATION_FACTORY, value = DEFAULT_AUTHENTICATION_FACTORY)
    @Reference(name = "TransportAuthenticationProviderFactory", target = DEFAULT_AUTHENTICATION_FACTORY, policy = ReferencePolicy.DYNAMIC)
    private TransportAuthenticationProviderFactory transportAuthenticationProviderFactory;

    @Property
    private static final String AUTHENTICATION_PROPERTIES = ReplicationAgentConfiguration.AUTHENTICATION_PROPERTIES;

    @Property(boolValue = false)
    private static final String USE_CUSTOM_HEADERS = "useCustomHeaders";

    @Property(cardinality = 50)
    private static final String CUSTOM_HEADERS = "customHeaders";

    @Property(boolValue = false)
    private static final String USE_CUSTOM_BODY = "useCustomBody";

    @Property
    private static final String CUSTOM_BODY = "customBody";

    protected TransportHandler createTransportHandler(Map<String, ?> config,
                                                      Dictionary<String, Object> props,
                                                      TransportAuthenticationProvider transportAuthenticationProvider,
                                                      ReplicationEndpoint[] endpoints, TransportEndpointStrategyType endpointStrategyType) {
        boolean useCustomHeaders = PropertiesUtil.toBoolean(config.get(USE_CUSTOM_HEADERS), false);
        props.put(USE_CUSTOM_HEADERS, useCustomHeaders);

        String[] customHeaders = PropertiesUtil.toStringArray(config.get(CUSTOM_HEADERS), new String[0]);
        props.put(CUSTOM_HEADERS, customHeaders);

        boolean useCustomBody = PropertiesUtil.toBoolean(config.get(USE_CUSTOM_BODY), false);
        props.put(USE_CUSTOM_BODY, useCustomBody);

        String customBody = PropertiesUtil.toString(config.get(CUSTOM_BODY), "");
        props.put(CUSTOM_BODY, customBody);

        return new HttpTransportHandler(useCustomHeaders,
                customHeaders,
                useCustomBody,
                customBody,
                (TransportAuthenticationProvider<Executor, Executor>) transportAuthenticationProvider,
                endpoints,
                endpointStrategyType);
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
