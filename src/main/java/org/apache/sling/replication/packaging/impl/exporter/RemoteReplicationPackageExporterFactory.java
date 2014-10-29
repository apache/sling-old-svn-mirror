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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.component.ReplicationComponent;
import org.apache.sling.replication.component.ReplicationComponentFactory;
import org.apache.sling.replication.component.ReplicationComponentProvider;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link org.apache.sling.replication.packaging.ReplicationPackageExporter}
 */
@Component(label = "Sling Replication - Remote Package Exporter Factory",
        metatype = true,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = ReplicationPackageExporter.class)
public class RemoteReplicationPackageExporterFactory implements ReplicationPackageExporter, ReplicationComponentProvider {
    private static final String TRANSPORT_AUTHENTICATION_PROVIDER_TARGET = ReplicationComponentFactory.COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER + ".target";


    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value = ReplicationComponentFactory.PACKAGE_EXPORTER_REMOTE, propertyPrivate = true)
    private static final String TYPE = ReplicationComponentFactory.COMPONENT_TYPE;

    @Property
    private static final String NAME = ReplicationComponentFactory.COMPONENT_NAME;

    @Property(name = TRANSPORT_AUTHENTICATION_PROVIDER_TARGET)
    @Reference(name = "TransportAuthenticationProvider", policy = ReferencePolicy.STATIC)
    private volatile TransportAuthenticationProvider transportAuthenticationProvider;

    @Property(cardinality = 100)
    public static final String ENDPOINTS = ReplicationComponentFactory.PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS;

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
    private static final String ENDPOINT_STRATEGY = ReplicationComponentFactory.PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS_STRATEGY;


    @Reference
    ReplicationComponentFactory replicationComponentFactory;

    ReplicationPackageExporter exporter;

    @Activate
    protected void activate(Map<String, Object> config) throws Exception {
        exporter = replicationComponentFactory.createComponent(ReplicationPackageExporter.class, config, this);
    }


    @Deactivate
    protected void deactivate() {
        exporter = null;
    }

    @Nonnull
    public List<ReplicationPackage> exportPackages(@Nonnull ResourceResolver resourceResolver, @Nonnull ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException {
        return exporter.exportPackages(resourceResolver, replicationRequest);
    }

    public ReplicationPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String replicationPackageId) {
        return exporter.getPackage(resourceResolver, replicationPackageId);
    }

    public <ComponentType extends ReplicationComponent> ComponentType getComponent(@Nonnull Class<ComponentType> type,
                                                                                   @Nullable String componentName) {
        if (type.isAssignableFrom(TransportAuthenticationProvider.class)) {
            return (ComponentType) transportAuthenticationProvider;
        }

        return null;
    }
}
