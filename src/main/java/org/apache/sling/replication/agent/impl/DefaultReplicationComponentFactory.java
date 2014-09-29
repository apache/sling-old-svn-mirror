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
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationComponentProvider;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic factory for replication components using a compact configuration, already existing OSGi services
 * for the components to be wired can be used as well as directly instantiated components (called by type name).
 * <p/>
 * Currently supported components are of kind 'agent' and 'trigger'.
 */
@Component(metatype = true,
        label = "Generic Replication Components Factory",
        description = "OSGi configuration Replication Component factory",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class DefaultReplicationComponentFactory implements ReplicationComponentListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(boolValue = true, label = "Enabled")
    private static final String ENABLED = "enabled";

    @Property(label = "Name")
    public static final String NAME = "name";

    @Property(label = "Properties")
    public static final String PROPERTIES = "properties";

    @Property(label = "Kind")
    public static final String KIND = "kind";

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private ReplicationComponentProvider componentProvider;

    private ServiceRegistration componentReg;
    private ServiceRegistration listenerReg;

    private BundleContext savedContext;
    private Map<String, Object> savedConfig;

    private String kind;

    @Activate
    public void activate(BundleContext context, Map<String, Object> config) throws Exception {
        log.debug("activating agent with config {}", config);

        savedContext = context;
        savedConfig = config;

        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ENABLED), true);
        String name = PropertiesUtil.toString(config.get(NAME), null);

        kind = PropertiesUtil.toString(config.get(KIND), null);

        if (enabled) {
            props.put(ENABLED, true);
            props.put(NAME, name);

            if (listenerReg == null) {
                listenerReg = context.registerService(ReplicationComponentListener.class.getName(), this, props);
            }

            if (componentReg == null) {
                Map<String, Object> configProperties = SettingsUtils.extractMap(PROPERTIES, config);

                Map<String, Object> properties = new HashMap<String, Object>();
                properties.putAll(config);
                properties.putAll(configProperties);

                String componentClass = null;
                Object componentObject = null;

                if ("agent".equals(kind)) {
                    SimpleReplicationAgent agent = (SimpleReplicationAgent) componentProvider.createComponent(ReplicationAgent.class, properties);
                    componentClass = ReplicationAgent.class.getName();
                    componentObject = agent;
                    agent.enable();

                } else if ("trigger".equals(kind)) {

                    ReplicationTrigger trigger = componentProvider.createComponent(ReplicationTrigger.class, properties);

                    componentClass = ReplicationTrigger.class.getName();
                    componentObject = trigger;
                }

                if (componentObject != null && componentClass != null) {
                    componentReg = context.registerService(componentClass, componentObject, props);
                    log.debug("activated component kind {} name", kind, name);
                }
            }
        }
    }

    @Deactivate
    private void deactivate(BundleContext context) {
        log.debug("deactivating component");
        if (componentReg != null) {
            ServiceReference reference = componentReg.getReference();

            if ("agent".equals(kind)) {
                SimpleReplicationAgent replicationComponent = (SimpleReplicationAgent) context.getService(reference);
                replicationComponent.disable();

            } else if ("trigger".equals(kind)) {

            }
            componentReg.unregister();
            componentReg = null;
        }
        if (listenerReg != null) {
            listenerReg.unregister();
            listenerReg = null;
        }
    }

    private void refresh(boolean isBinding) {
        try {
            if (savedContext != null && savedConfig != null) {
                if (isBinding && componentReg == null) {
                    activate(savedContext, savedConfig);
                } else if (!isBinding && componentReg != null) {
                    deactivate(savedContext);
                }
            }

        } catch (Exception e) {
            log.error("Cannot refresh agent", e);
        }
    }

    public <ComponentType> void componentBind(ComponentType component, String componentName) {
        refresh(true);
    }

    public <ComponentType> void componentUnbind(ComponentType component, String componentName) {
        refresh(false);
    }
}
