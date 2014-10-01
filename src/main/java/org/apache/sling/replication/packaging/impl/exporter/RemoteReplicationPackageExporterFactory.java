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
package org.apache.sling.replication.packaging.impl.exporter;

import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.impl.ReplicationTransportConstants;
import org.apache.sling.replication.transport.impl.TransportEndpointStrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link org.apache.sling.replication.packaging.ReplicationPackageExporter}
 */
@Component(label = "Remote Replication Package Exporter",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = ReplicationPackageExporter.class)
public class RemoteReplicationPackageExporterFactory implements ReplicationPackageExporter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property
    private static final String NAME = "name";

    @Property(value = "exporters/remote", propertyPrivate = true)
    private static final String FACTORY_NAME = "factoryName";

    @Property(name = ReplicationTransportConstants.TRANSPORT_AUTHENTICATION_PROVIDER_TARGET)
    @Reference(name = "TransportAuthenticationProviderFactory", policy = ReferencePolicy.DYNAMIC)
    private volatile TransportAuthenticationProvider transportAuthenticationProvider;

    @Property(label = "Target ReplicationPackageBuilder", name = "ReplicationPackageBuilder.target")
    @Reference(name = "ReplicationPackageBuilder", policy = ReferencePolicy.DYNAMIC)
    private volatile ReplicationPackageBuilder packageBuilder;

    @Property(name = "poll items", description = "number of subsequent poll requests to make", intValue = 1)
    public static final String POLL_ITEMS = "poll.items";

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

    ReplicationPackageExporter exporter;

    @Activate
    protected void activate(Map<String, Object> config) throws Exception {
        exporter = getInstance(config, packageBuilder, transportAuthenticationProvider);
    }

    public static ReplicationPackageExporter getInstance(Map<String, Object> config,
                                                         ReplicationPackageBuilder packageBuilder,
                                                         TransportAuthenticationProvider transportAuthenticationProvider) {

        if (packageBuilder == null) {
            throw new IllegalArgumentException("packageBuilder is required");
        }

        String[] endpoints = PropertiesUtil.toStringArray(config.get(ReplicationTransportConstants.ENDPOINTS), new String[0]);

        int pollItems = PropertiesUtil.toInteger(config.get(POLL_ITEMS), 1);

        String endpointStrategyName = PropertiesUtil.toString(config.get(ReplicationTransportConstants.ENDPOINT_STRATEGY), "One");
        TransportEndpointStrategyType transportEndpointStrategyType = TransportEndpointStrategyType.valueOf(endpointStrategyName);

        return new RemoteReplicationPackageExporter(packageBuilder, transportAuthenticationProvider,
                endpoints,
                transportEndpointStrategyType,
                pollItems);
    }

    @Deactivate
    protected void deactivate() {
        exporter = null;
    }

    public List<ReplicationPackage> exportPackage(ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException {
        return exporter.exportPackage(replicationRequest);
    }

    public ReplicationPackage exportPackageById(String replicationPackageId) {
        return exporter.exportPackageById(replicationPackageId);
    }
}
