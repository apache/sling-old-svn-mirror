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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationComponent;
import org.apache.sling.replication.agent.ReplicationComponentFactory;
import org.apache.sling.replication.agent.ReplicationComponentProvider;
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
 * An OSGi service factory for 'Coordinate' {@link org.apache.sling.replication.agent.ReplicationAgent}s.
 */
@Component(metatype = true,
        label = "Coordinating Replication Agents Factory",
        description = "OSGi configuration factory for coordinate agents",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class CoordinatingReplicationAgentFactory implements ReplicationComponentProvider {
    public static final String QUEUEPROVIDER_TARGET = "queueProvider.target";

    public static final String QUEUE_DISTRIBUTION_TARGET = "queueDistributionStrategy.target";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DEFAULT_QUEUEPROVIDER = "(name=" + JobHandlingReplicationQueueProvider.NAME + ")";

    private static final String DEFAULT_DISTRIBUTION = "(name=" + SingleQueueDistributionStrategy.NAME + ")";

    @Property(boolValue = true, label = "Enabled")
    private static final String ENABLED = "enabled";

    @Property(label = "Name")
    public static final String NAME = "name";

    @Property(label = "Service Name")
    public static final String SERVICE_NAME = "serviceName";

    @Property(label = "Package Exporter", cardinality = 100)
    public static final String PACKAGE_EXPORTER = "packageExporter";

    @Property(label = "Package Importer", cardinality = 100)
    public static final String PACKAGE_IMPORTER = "packageImporter";

    @Property(label = "Target ReplicationQueueProvider", name = QUEUEPROVIDER_TARGET, value = DEFAULT_QUEUEPROVIDER)
    @Reference(name = "queueProvider", target = DEFAULT_QUEUEPROVIDER)
    private volatile ReplicationQueueProvider queueProvider;

    @Property(label = "Target QueueDistributionStrategy", name = QUEUE_DISTRIBUTION_TARGET, value = DEFAULT_DISTRIBUTION)
    @Reference(name = "queueDistributionStrategy", target = DEFAULT_DISTRIBUTION)
    private volatile ReplicationQueueDistributionStrategy queueDistributionStrategy;

    @Property(label = "Target TransportAuthenticationProvider", name = "transportAuthenticationProvider.target")
    @Reference(name = "transportAuthenticationProvider")
    private volatile TransportAuthenticationProvider transportAuthenticationProvider;

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private ReplicationComponentFactory componentFactory;

    private ServiceRegistration componentReg;

    @Activate
    public void activate(BundleContext context, Map<String, Object> config) {

        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ENABLED), true);

        if (enabled) {
            props.put(ENABLED, true);

            String name = PropertiesUtil
                    .toString(config.get(NAME), String.valueOf(new Random().nextInt(1000)));
            props.put(NAME, name);


            String queue = PropertiesUtil.toString(config.get(QUEUEPROVIDER_TARGET), DEFAULT_QUEUEPROVIDER);
            props.put(QUEUEPROVIDER_TARGET, queue);

            String distribution = PropertiesUtil.toString(config.get(QUEUE_DISTRIBUTION_TARGET), DEFAULT_DISTRIBUTION);
            props.put(QUEUE_DISTRIBUTION_TARGET, distribution);

            if (componentReg == null) {
                Map<String, Object> properties = new HashMap<String, Object>();
                properties.putAll(config);

                properties.put("type", "simple");
                properties.put("isPassive", false);

                {
                    String[] packageImporterProperties = PropertiesUtil.toStringArray(properties.get(PACKAGE_IMPORTER));
                    List<String> packageImporterPropertiesList = new ArrayList<String>();
                    packageImporterPropertiesList.addAll(Arrays.asList(packageImporterProperties));
                    packageImporterPropertiesList.add("type=remote");
                    packageImporterProperties = packageImporterPropertiesList.toArray(new String[packageImporterPropertiesList.size()]);
                    properties.put(PACKAGE_IMPORTER, packageImporterProperties);
                }

                {
                    String[] packageExporterProperties = PropertiesUtil.toStringArray(properties.get(PACKAGE_EXPORTER));
                    List<String> packageExporterPropertiesList = new ArrayList<String>();
                    packageExporterPropertiesList.addAll(Arrays.asList(packageExporterProperties));
                    packageExporterPropertiesList.add("type=remote");
                    packageExporterProperties = packageExporterPropertiesList.toArray(new String[packageExporterPropertiesList.size()]);
                    properties.put(PACKAGE_EXPORTER, packageExporterProperties);
                }


                properties.put("trigger0", new String[] { "type=scheduledEvent" });

                ReplicationAgent agent = componentFactory.createComponent(ReplicationAgent.class, properties, this);

                log.debug("activated agent {}", agent != null ? agent.getName() : null);

                if (agent != null) {
                    props.put(NAME, agent.getName());

                    // register agent service
                    componentReg = context.registerService(ReplicationAgent.class.getName(), agent, props);

                    if (agent instanceof ReplicationComponent) {
                        ((ReplicationComponent) agent).enable();
                    }
                }
            }

        }
    }

    @Deactivate
    private void deactivate(BundleContext context) {
        if (componentReg != null) {
            ServiceReference reference = componentReg.getReference();
            Object service = context.getService(reference);
            if (service instanceof ReplicationComponent) {
                ((ReplicationComponent) service).disable();
            }

            componentReg.unregister();
            componentReg = null;
        }

    }

    public <ComponentType> ComponentType getComponent(Class<ComponentType> type, String componentName) {
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
}
