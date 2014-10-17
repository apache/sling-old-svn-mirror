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
package org.apache.sling.replication.packaging.impl.importer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationComponentFactory;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
import org.apache.sling.replication.transport.ReplicationTransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.impl.AdvancedHttpReplicationTransportHandler;
import org.apache.sling.replication.transport.impl.MultipleEndpointReplicationTransportHandler;
import org.apache.sling.replication.transport.impl.TransportEndpointStrategyType;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link org.apache.sling.replication.packaging.ReplicationPackageImporter} supporting multiple
 * endpoints and custom HTTP headers and body.
 */
@Component(label = "Advanced Remote Replication Package Importer",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = ReplicationPackageImporter.class)
public class AdvancedRemoteReplicationPackageImporter implements ReplicationPackageImporter {
    private static final String TRANSPORT_AUTHENTICATION_PROVIDER_TARGET = ReplicationComponentFactory.COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER + ".target";


    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(name = TRANSPORT_AUTHENTICATION_PROVIDER_TARGET)
    @Reference(name = "TransportAuthenticationProvider", policy = ReferencePolicy.DYNAMIC)
    private volatile TransportAuthenticationProvider transportAuthenticationProvider;

    @Property(cardinality = 100)
    public static final String ENDPOINTS = "endpoints";

    @Property(options = {
            @PropertyOption(name = "All",
                    value = "all endpoints"
            ),
            @PropertyOption(name = "One",
                    value = "one endpoint"
            )},
            value = "One"
    )
    private static final String ENDPOINT_STRATEGY = "endpoint.strategy";

    @Property(boolValue = false)
    private static final String USE_CUSTOM_HEADERS = "useCustomHeaders";

    @Property(cardinality = 50)
    private static final String CUSTOM_HEADERS = "customHeaders";

    @Property(boolValue = false)
    private static final String USE_CUSTOM_BODY = "useCustomBody";

    @Property
    private static final String CUSTOM_BODY = "customBody";

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    ReplicationTransportHandler transportHandler;

    @Activate
    protected void activate(BundleContext context, Map<String, ?> config) throws Exception {

        String[] endpoints = PropertiesUtil.toStringArray(config.get(ENDPOINTS), new String[0]);
        String endpointStrategyName = PropertiesUtil.toString(config.get(ENDPOINT_STRATEGY),
                TransportEndpointStrategyType.One.name());
        TransportEndpointStrategyType transportEndpointStrategyType = TransportEndpointStrategyType.valueOf(endpointStrategyName);


        boolean useCustomHeaders = PropertiesUtil.toBoolean(config.get(USE_CUSTOM_HEADERS), false);
        String[] customHeaders = PropertiesUtil.toStringArray(config.get(CUSTOM_HEADERS), new String[0]);
        boolean useCustomBody = PropertiesUtil.toBoolean(config.get(USE_CUSTOM_BODY), false);
        String customBody = PropertiesUtil.toString(config.get(CUSTOM_BODY), "");


        List<ReplicationTransportHandler> transportHandlers = new ArrayList<ReplicationTransportHandler>();

        for (String endpoint : endpoints) {
            if (endpoint != null && endpoint.length() > 0) {
                transportHandlers.add(new AdvancedHttpReplicationTransportHandler(useCustomHeaders, customHeaders,
                        useCustomBody, customBody,
                        transportAuthenticationProvider,
                        new ReplicationEndpoint(endpoint), null, -1));
            }
        }
        transportHandler = new MultipleEndpointReplicationTransportHandler(transportHandlers,
                transportEndpointStrategyType);

    }


    public boolean importPackage(ResourceResolver resourceResolver, ReplicationPackage replicationPackage) {
        boolean result = false;
        try {
            transportHandler.deliverPackage(resourceResolver, replicationPackage);
            result = true;
        } catch (Exception e) {
            log.error("failed delivery", e);
        }
        return result;
    }

    public ReplicationPackage readPackage(ResourceResolver resourceResolver, InputStream stream) throws ReplicationPackageReadingException {
        return null;
    }

}
