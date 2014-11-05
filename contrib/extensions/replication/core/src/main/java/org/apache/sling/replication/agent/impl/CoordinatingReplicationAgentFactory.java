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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.component.ManagedReplicationComponent;
import org.apache.sling.replication.component.ReplicationComponent;
import org.apache.sling.replication.component.ReplicationComponentFactory;
import org.apache.sling.replication.component.ReplicationComponentProvider;
import org.apache.sling.replication.event.impl.ReplicationEventFactory;
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
 * An OSGi service factory for 'Coordinate' {@link org.apache.sling.replication.agent.ReplicationAgent}s.
 */
@Component(metatype = true,
        label = "Sling Replication - Coordinating Agents Factory",
        description = "OSGi configuration factory for coordinate agents",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class CoordinatingReplicationAgentFactory implements ReplicationComponentProvider {

    private static final String TRANSPORT_AUTHENTICATION_PROVIDER_TARGET = ReplicationComponentFactory.COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER + ".target";


    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(boolValue = true, label = "Enabled")
    private static final String ENABLED = ReplicationComponentFactory.COMPONENT_ENABLED;

    @Property(value = ReplicationComponentFactory.AGENT_SIMPLE, propertyPrivate = true)
    private static final String TYPE = ReplicationComponentFactory.COMPONENT_TYPE;

    @Property(label = "Name")
    public static final String NAME = ReplicationComponentFactory.COMPONENT_NAME;

    @Property(boolValue = false, propertyPrivate = true)
    public static final String IS_PASSIVE = ReplicationComponentFactory.AGENT_SIMPLE_PROPERTY_IS_PASSIVE;

    @Property(label = "Service Name")
    public static final String SERVICE_NAME = ReplicationComponentFactory.AGENT_SIMPLE_PROPERTY_SERVICE_NAME;

    @Property(label = "Package Exporter", cardinality = 100)
    public static final String PACKAGE_EXPORTER = ReplicationComponentFactory.COMPONENT_PACKAGE_EXPORTER;

    @Property(label = "Package Importer", cardinality = 100)
    public static final String PACKAGE_IMPORTER = ReplicationComponentFactory.COMPONENT_PACKAGE_IMPORTER;

    @Property(label = "Target TransportAuthenticationProvider", name = TRANSPORT_AUTHENTICATION_PROVIDER_TARGET)
    @Reference(name = "transportAuthenticationProvider")
    private volatile TransportAuthenticationProvider transportAuthenticationProvider;

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private JobManager jobManager;

    @Reference
    private ReplicationComponentFactory componentFactory;

    private BundleContext savedContext;

    private ServiceRegistration componentReg;

    private String agentName;

    @Activate
    protected void activate(BundleContext context, Map<String, Object> config) {

        savedContext = context;
        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ENABLED), true);

        if (enabled) {
            props.put(ENABLED, true);

            agentName = PropertiesUtil.toString(config.get(NAME), null);
            props.put(NAME, agentName);

            if (componentReg == null) {
                Map<String, Object> properties = new HashMap<String, Object>();
                properties.putAll(config);

                String[] packageImporterProperties = PropertiesUtil.toStringArray(properties.get(PACKAGE_IMPORTER));
                String[] packageExporterProperties = PropertiesUtil.toStringArray(properties.get(PACKAGE_EXPORTER));

                if (packageImporterProperties == null || packageExporterProperties == null ||
                        packageImporterProperties.length == 0 || packageExporterProperties.length == 0) {
                    throw new IllegalArgumentException("package exporters and importers cannot be null/empty");
                }

                List<String> packageImporterPropertiesList = new ArrayList<String>();
                packageImporterPropertiesList.addAll(Arrays.asList(packageImporterProperties));
                packageImporterPropertiesList.add("type=remote");
                packageImporterProperties = packageImporterPropertiesList.toArray(new String[packageImporterPropertiesList.size()]);
                properties.put(PACKAGE_IMPORTER, packageImporterProperties);

                List<String> packageExporterPropertiesList = new ArrayList<String>();
                packageExporterPropertiesList.addAll(Arrays.asList(packageExporterProperties));
                packageExporterPropertiesList.add("type=remote");
                packageExporterProperties = packageExporterPropertiesList.toArray(new String[packageExporterPropertiesList.size()]);
                properties.put(PACKAGE_EXPORTER, packageExporterProperties);

                ReplicationAgent agent = componentFactory.createComponent(ReplicationAgent.class, properties, this);

                log.debug("activated agent {}", agentName);

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
            return (ComponentType) new JobHandlingReplicationQueueProvider(agentName, jobManager, savedContext);
        } else if (type.isAssignableFrom(ReplicationQueueDistributionStrategy.class)) {
            return (ComponentType) new SingleQueueDistributionStrategy();
        } else if (type.isAssignableFrom(TransportAuthenticationProvider.class)) {
            return (ComponentType) transportAuthenticationProvider;
        }
        return null;
    }
}
