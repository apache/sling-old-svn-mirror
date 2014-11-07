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
package org.apache.sling.distribution.agent.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.component.DistributionComponent;
import org.apache.sling.distribution.component.ManagedDistributionComponent;
import org.apache.sling.distribution.component.DistributionComponentFactory;
import org.apache.sling.distribution.component.DistributionComponentProvider;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.queue.DistributionQueueDistributionStrategy;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.SingleQueueDistributionStrategy;
import org.apache.sling.distribution.queue.impl.jobhandling.JobHandlingDistributionQueueProvider;
import org.apache.sling.distribution.resources.impl.OsgiUtils;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An OSGi service factory for 'Coordinate' {@link org.apache.sling.distribution.agent.DistributionAgent}s.
 */
@Component(metatype = true,
        label = "Sling Distribution - Coordinating Agents Factory",
        description = "OSGi configuration factory for coordinate agents",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class CoordinatingDistributionAgentFactory implements DistributionComponentProvider {

    private static final String TRANSPORT_AUTHENTICATION_PROVIDER_TARGET = DistributionComponentFactory.COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER + ".target";


    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(boolValue = true, label = "Enabled")
    private static final String ENABLED = DistributionComponentFactory.COMPONENT_ENABLED;

    @Property(value = DistributionComponentFactory.AGENT_SIMPLE, propertyPrivate = true)
    private static final String TYPE = DistributionComponentFactory.COMPONENT_TYPE;

    @Property(label = "Name")
    public static final String NAME = DistributionComponentFactory.COMPONENT_NAME;

    @Property(boolValue = false, propertyPrivate = true)
    public static final String IS_PASSIVE = DistributionComponentFactory.AGENT_SIMPLE_PROPERTY_IS_PASSIVE;

    @Property(label = "Service Name")
    public static final String SERVICE_NAME = DistributionComponentFactory.AGENT_SIMPLE_PROPERTY_SERVICE_NAME;

    @Property(label = "Request Authorization Strategy Properties", cardinality = 100)
    public static final String REQUEST_AUTHORIZATION_STRATEGY = DistributionComponentFactory.COMPONENT_REQUEST_AUTHORIZATION_STRATEGY;

    @Property(label = "Package Exporter Properties", cardinality = 100)
    public static final String PACKAGE_EXPORTER = DistributionComponentFactory.COMPONENT_PACKAGE_EXPORTER;

    @Property(label = "Package Importer Properties", cardinality = 100)
    public static final String PACKAGE_IMPORTER = DistributionComponentFactory.COMPONENT_PACKAGE_IMPORTER;

    @Property(label = "Trigger Properties", cardinality = 100)
    public static final String TRIGGER = DistributionComponentFactory.COMPONENT_TRIGGER;

    @Property(label = "Target TransportAuthenticationProvider", name = TRANSPORT_AUTHENTICATION_PROVIDER_TARGET)
    @Reference(name = "transportAuthenticationProvider")
    private volatile TransportAuthenticationProvider transportAuthenticationProvider;

    @Reference
    private DistributionEventFactory distributionEventFactory;

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private JobManager jobManager;

    @Reference
    private DistributionComponentFactory componentFactory;

    private BundleContext savedContext;

    private ServiceRegistration componentReg;

    private String agentName;

    @Activate
    protected void activate(BundleContext context, Map<String, Object> config) {
        log.info("activating with config {}", OsgiUtils.osgiPropertyMapToString(config));


        savedContext = context;
        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ENABLED), true);

        if (enabled) {
            props.put(ENABLED, true);

            agentName = PropertiesUtil.toString(config.get(NAME), null);
            props.put(NAME, agentName);

            if (componentReg == null) {
                String[] requestAuthProperties = PropertiesUtil.toStringArray(config.get(REQUEST_AUTHORIZATION_STRATEGY), new String[0]);
                String[] packageImporterProperties = PropertiesUtil.toStringArray(config.get(PACKAGE_IMPORTER), new String[0]);
                String[] packageExporterProperties = PropertiesUtil.toStringArray(config.get(PACKAGE_EXPORTER), new String[0]);
                String[] triggerProperties = PropertiesUtil.toStringArray(config.get(TRIGGER), new String[0]);

                if (packageImporterProperties == null || packageExporterProperties == null ||
                        packageImporterProperties.length == 0 || packageExporterProperties.length == 0) {
                    throw new IllegalArgumentException("package exporters and importers cannot be null/empty");
                }


                Map<String, Object> properties = new HashMap<String, Object>();
                properties.putAll(config);

                properties.put(REQUEST_AUTHORIZATION_STRATEGY, SettingsUtils.parseLines(requestAuthProperties));
                properties.put(PACKAGE_IMPORTER, SettingsUtils.parseLines(packageImporterProperties));
                properties.put(PACKAGE_EXPORTER, SettingsUtils.parseLines(packageExporterProperties));
                properties.put(TRIGGER, SettingsUtils.parseLines(triggerProperties));

                // ensure exporter and importer are remote
                ((Map) properties.get(PACKAGE_EXPORTER)).put("type", "remote");
                ((Map) properties.get(PACKAGE_IMPORTER)).put("type", "remote");


                DistributionAgent agent = componentFactory.createComponent(DistributionAgent.class, properties, this);

                log.debug("activated agent {}", agentName);

                if (agent != null) {
                    // register agent service
                    componentReg = context.registerService(DistributionAgent.class.getName(), agent, props);
                    if (agent instanceof ManagedDistributionComponent) {
                        ((ManagedDistributionComponent) agent).enable();
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
            if (service instanceof ManagedDistributionComponent) {
                ((ManagedDistributionComponent) service).disable();
            }

            componentReg.unregister();
            componentReg = null;
        }

    }

    public <ComponentType extends DistributionComponent> ComponentType getComponent(@Nonnull Class<ComponentType> type,
                                                                                   @Nullable String componentName) {
        if (type.isAssignableFrom(DistributionQueueProvider.class)) {
            return (ComponentType) new JobHandlingDistributionQueueProvider(agentName, jobManager, savedContext);
        } else if (type.isAssignableFrom(DistributionQueueDistributionStrategy.class)) {
            return (ComponentType) new SingleQueueDistributionStrategy();
        } else if (type.isAssignableFrom(TransportAuthenticationProvider.class)) {
            return (ComponentType) transportAuthenticationProvider;
        }
        return null;
    }
}
