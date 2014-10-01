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
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.impl.ReplicationTransportConstants;
import org.apache.sling.replication.transport.impl.TransportEndpointStrategyType;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remote implementation of {@link org.apache.sling.replication.packaging.ReplicationPackageImporter}
 */
@Component(label = "Remote Replication Package Importer",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = ReplicationPackageImporter.class)
public class RemoteReplicationPackageImporterFactory implements ReplicationPackageImporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property
    private static final String NAME = "name";

    @Property(name = ReplicationTransportConstants.TRANSPORT_AUTHENTICATION_PROVIDER_TARGET)
    @Reference(name = "TransportAuthenticationProviderFactory", policy = ReferencePolicy.DYNAMIC)
    private volatile TransportAuthenticationProvider transportAuthenticationProvider;

    @Property(options = {
            @PropertyOption(name = "All",
                    value = "all endpoints"
            ),
            @PropertyOption(name = "One",
                    value = "one endpoint"
            )},
            value = "One"
    )
    private static final String ENDPOINT_STRATEGY = ReplicationTransportConstants.ENDPOINT_STRATEGY;

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    private RemoteReplicationPackageImporter importer;

    @Activate
    protected void activate(BundleContext context, Map<String, Object> config) throws Exception {

        importer = getInstance(config, transportAuthenticationProvider);

    }

    public static RemoteReplicationPackageImporter getInstance(Map<String, Object> config, TransportAuthenticationProvider transportAuthenticationProvider) {

        if (transportAuthenticationProvider == null) {
            throw new IllegalArgumentException("transportAuthenticationProviderFactory is required");
        }

        String[] endpoints = PropertiesUtil.toStringArray(config.get(ReplicationTransportConstants.ENDPOINTS), new String[0]);

        String endpointStrategyName = PropertiesUtil.toString(config.get(ENDPOINT_STRATEGY),
                TransportEndpointStrategyType.One.name());
        TransportEndpointStrategyType transportEndpointStrategyType = TransportEndpointStrategyType.valueOf(endpointStrategyName);


        return new RemoteReplicationPackageImporter(transportAuthenticationProvider, endpoints, transportEndpointStrategyType);

    }

    public boolean importPackage(ReplicationPackage replicationPackage) {
        return importer.importPackage(replicationPackage);
    }

    public ReplicationPackage readPackage(InputStream stream) throws ReplicationPackageReadingException {
        return importer.readPackage(stream);
    }

}
