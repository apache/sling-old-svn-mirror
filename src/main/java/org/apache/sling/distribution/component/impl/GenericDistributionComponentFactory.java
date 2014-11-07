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
package org.apache.sling.distribution.component.impl;

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
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.component.DistributionComponent;
import org.apache.sling.distribution.component.DistributionComponentFactory;
import org.apache.sling.distribution.component.DistributionComponentProvider;
import org.apache.sling.distribution.component.ManagedDistributionComponent;
import org.apache.sling.distribution.resources.impl.OsgiUtils;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype = true,
        label = "Sling Distribution - Generic Components Factory",
        description = "OSGi configuration factory for generic Distribution Components",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class GenericDistributionComponentFactory implements DistributionComponentProvider {
    private static final String TRANSPORT_AUTHENTICATION_PROVIDER_TARGET = DistributionComponentFactory.COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER + ".target";


    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(boolValue = true, label = "Enabled")
    private static final String ENABLED = DistributionComponentFactory.COMPONENT_ENABLED;

    @Property(label = "Name")
    public static final String NAME = DistributionComponentFactory.COMPONENT_NAME;

    @Property(label = "Properties")
    public static final String PROPERTIES = "properties";

    @Property(label = "Component Type")
    public static final String COMPONENT_TYPE = "componentType";

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private DistributionComponentFactory componentFactory;

    @Property(label = "Target TransportAuthenticationProvider", name = TRANSPORT_AUTHENTICATION_PROVIDER_TARGET)
    @Reference(name = "transportAuthenticationProvider", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private volatile TransportAuthenticationProvider transportAuthenticationProvider;

    private ServiceRegistration componentReg;
    private String componentType;

    private BundleContext savedContext;
    private Map<String, Object> savedConfig;

    @Activate
    protected void activate(BundleContext context, Map<String, Object> config) {
        log.debug("activating with config {}", OsgiUtils.osgiPropertyMapToString(config));

        savedContext = context;
        savedConfig = config;

        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ENABLED), true);
        String name = PropertiesUtil.toString(config.get(NAME), null);

        componentType = PropertiesUtil.toString(config.get(COMPONENT_TYPE), null);

        if (enabled) {
            props.put(ENABLED, true);
            props.put(NAME, name);


            if (componentReg == null) {
                String[] propertyLines = PropertiesUtil.toStringArray(config.get(PROPERTIES));
                Map<String, Object> configProperties = SettingsUtils.parseLines(propertyLines);

                Map<String, Object> properties = new HashMap<String, Object>();
                properties.putAll(config);
                properties.putAll(configProperties);

                String componentClass = null;
                Object componentObject = null;

                try {
                    if (DistributionComponentFactory.COMPONENT_AGENT.equals(componentType)) {
                        DistributionAgent agent = componentFactory.createComponent(DistributionAgent.class, properties, this);
                        componentClass = DistributionAgent.class.getName();
                        componentObject = agent;

                    } else if (DistributionComponentFactory.COMPONENT_TRIGGER.equals(componentType)) {

                        DistributionTrigger trigger = componentFactory.createComponent(DistributionTrigger.class, properties, this);

                        componentClass = DistributionTrigger.class.getName();
                        componentObject = trigger;
                    }
                }
                catch (IllegalArgumentException e) {
                    log.warn("cannot create component", e);
                }

                if (componentObject != null && componentClass != null) {
                    if (componentObject instanceof ManagedDistributionComponent) {
                        ((ManagedDistributionComponent) componentObject).enable();
                    }

                    componentReg = context.registerService(componentClass, componentObject, props);


                    log.debug("activated component {} with name {}", componentType, name);
                }
            }
        }
    }

    @Deactivate
    protected void deactivate(BundleContext context) {
        log.debug("deactivating component");
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

    public <ComponentType extends DistributionComponent> ComponentType getComponent(@Nonnull Class<ComponentType> type,
                                                                                   @Nullable String componentName) {
        if (type.isAssignableFrom(TransportAuthenticationProvider.class)) {
            return (ComponentType) transportAuthenticationProvider;
        }
        return null;
    }
}
