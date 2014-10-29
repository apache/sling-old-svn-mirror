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
package org.apache.sling.replication.agent.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.component.ManagedReplicationComponent;
import org.apache.sling.replication.component.ReplicationComponent;
import org.apache.sling.replication.component.ReplicationComponentFactory;
import org.apache.sling.replication.component.ReplicationComponentProvider;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.queue.impl.SingleQueueDistributionStrategy;
import org.apache.sling.replication.queue.impl.jobhandling.JobHandlingReplicationQueueProvider;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An OSGi service factory for {@link ReplicationAgent}s which references already existing OSGi services.
 */
@Component(metatype = true,
        label = "Sling Replication - Simple Agents Factory",
        description = "OSGi configuration factory for agents",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class SimpleReplicationAgentFactory implements ReplicationComponentProvider {
    private static final String QUEUE_PROVIDER_TARGET = ReplicationComponentFactory.COMPONENT_QUEUE_PROVIDER + ".target";
    private static final String QUEUE_DISTRIBUTION_TARGET = ReplicationComponentFactory.COMPONENT_QUEUE_DISTRIBUTION_STRATEGY + ".target";
    private static final String TRANSPORT_AUTHENTICATION_PROVIDER_TARGET = ReplicationComponentFactory.COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER + ".target";

    private static final String DEFAULT_QUEUEPROVIDER = "(name=" + JobHandlingReplicationQueueProvider.NAME + ")";
    private static final String DEFAULT_DISTRIBUTION = "(name=" + SingleQueueDistributionStrategy.NAME + ")";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(boolValue = true, label = "Enabled")
    private static final String ENABLED = ReplicationComponentFactory.COMPONENT_ENABLED;

    @Property(value = ReplicationComponentFactory.AGENT_SIMPLE, propertyPrivate = true)
    private static final String TYPE = ReplicationComponentFactory.COMPONENT_TYPE;

    @Property(label = "Name")
    public static final String NAME = ReplicationComponentFactory.COMPONENT_NAME;

    @Property(boolValue = false, label = "Use this agent as a passive one (only queueing)")
    public static final String IS_PASSIVE = ReplicationComponentFactory.AGENT_SIMPLE_PROPERTY_IS_PASSIVE;

    @Property(label = "Request Authorization Strategy Properties", cardinality = 100)
    public static final String REQUEST_AUTHORIZATION_STRATEGY = ReplicationComponentFactory.COMPONENT_REQUEST_AUTHORIZATION_STRATEGY;

    @Property(label = "Package Exporter Properties", cardinality = 100)
    public static final String PACKAGE_EXPORTER = ReplicationComponentFactory.COMPONENT_PACKAGE_EXPORTER;

    @Property(label = "Package Importer Properties", cardinality = 100)
    public static final String PACKAGE_IMPORTER = ReplicationComponentFactory.COMPONENT_PACKAGE_IMPORTER;

    @Property(label = "Service Name")
    public static final String SERVICE_NAME = ReplicationComponentFactory.AGENT_SIMPLE_PROPERTY_SERVICE_NAME;

    @Property(label = "Target ReplicationQueueProvider", name = QUEUE_PROVIDER_TARGET, value = DEFAULT_QUEUEPROVIDER)
    @Reference(name = ReplicationComponentFactory.COMPONENT_QUEUE_PROVIDER, target = DEFAULT_QUEUEPROVIDER)
    private volatile ReplicationQueueProvider queueProvider;

    @Property(label = "Target QueueDistributionStrategy", name = QUEUE_DISTRIBUTION_TARGET, value = DEFAULT_DISTRIBUTION)
    @Reference(name = ReplicationComponentFactory.COMPONENT_QUEUE_DISTRIBUTION_STRATEGY, target = DEFAULT_DISTRIBUTION)
    private volatile ReplicationQueueDistributionStrategy queueDistributionStrategy;

    @Property(label = "Target TransportAuthenticationProvider", name = TRANSPORT_AUTHENTICATION_PROVIDER_TARGET)
    @Reference(name = "transportAuthenticationProvider", policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private volatile TransportAuthenticationProvider transportAuthenticationProvider;

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private ReplicationComponentFactory componentFactory;

    private ServiceRegistration componentReg;
    private BundleContext savedContext;
    private Map<String, Object> savedConfig;

    @Activate
    protected void activate(BundleContext context, Map<String, Object> config) {

        savedContext = context;
        savedConfig = config;

        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ENABLED), true);

        if (enabled) {
            props.put(ENABLED, true);

            String name = PropertiesUtil
                    .toString(config.get(NAME), String.valueOf(new Random().nextInt(1000)));
            props.put(NAME, name);

            if (componentReg == null && componentFactory != null) {
                ReplicationAgent agent = componentFactory.createComponent(ReplicationAgent.class, config, this);

                log.debug("activated agent {}", name);

                if (agent != null) {

                    // register agent service
                    componentReg = context.registerService(ReplicationAgent.class.getName(), agent, props);
                    if (agent instanceof ManagedReplicationComponent) {
                        ((ManagedReplicationComponent) agent).enable();
                    }
                }
            }

        }
    }

    @Deactivate
    protected void deactivate(BundleContext context) {
        if (componentReg != null) {
            ServiceReference reference = componentReg.getReference();
            Object service = context.getService(reference);
            if (service instanceof ManagedReplicationComponent) {
                ((ManagedReplicationComponent) service).disable();
            }

            componentReg.unregister();
            componentReg = null;
        }

    }

    public <ComponentType extends ReplicationComponent> ComponentType getComponent(@Nonnull Class<ComponentType> type,
                                                                                   @Nullable String componentName) {
        if (type.isAssignableFrom(ReplicationQueueProvider.class)) {
            return (ComponentType) queueProvider;
        }
        else if (type.isAssignableFrom(ReplicationQueueDistributionStrategy.class)) {
            return (ComponentType) queueDistributionStrategy;
        }
        else if (type.isAssignableFrom(TransportAuthenticationProvider.class)) {
            return (ComponentType) transportAuthenticationProvider;
        }
        return null;
    }

    private void refresh() {
        if (savedContext != null && savedConfig != null) {
            if (componentReg == null) {
                activate(savedContext, savedConfig);
            }
            else {
                deactivate(savedContext);
                activate(savedContext, savedConfig);
            }
        }
    }

    private void bindTransportAuthenticationProvider(TransportAuthenticationProvider transportAuthenticationProvider) {
        this.transportAuthenticationProvider = transportAuthenticationProvider;
        refresh();
    }

    private void unbindTransportAuthenticationProvider(TransportAuthenticationProvider transportAuthenticationProvider) {
        this.transportAuthenticationProvider = null;
        refresh();
    }
}
