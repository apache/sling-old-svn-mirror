package org.apache.sling.replication.agent.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationComponent;
import org.apache.sling.replication.agent.ReplicationComponentFactory;
import org.apache.sling.replication.agent.ReplicationComponentProvider;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@Component(metatype = true,
        label = "Generic Replication Components Factory",
        description = "OSGi configuration factory for generic Replication Components",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class GenericReplicationComponentFactory implements ReplicationComponentProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(boolValue = true, label = "Enabled")
    private static final String ENABLED = "enabled";

    @Property(label = "Name")
    public static final String NAME = "name";

    @Property(label = "Properties")
    public static final String PROPERTIES = "properties";

    @Property(label = "Component Type")
    public static final String COMPONENT_TYPE = "componentType";

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private ReplicationComponentFactory componentFactory;

    @Property(label = "Target TransportAuthenticationProvider", name = "transportAuthenticationProvider.target")
    @Reference(name = "transportAuthenticationProvider", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private volatile TransportAuthenticationProvider transportAuthenticationProvider;

    private ServiceRegistration componentReg;
    private String componentType;

    private BundleContext savedContext;
    private Map<String, Object> savedConfig;

    @Activate
    public void activate(BundleContext context, Map<String, Object> config) {
        log.debug("activating component with config {}", config);

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
                Map<String, Object> configProperties = SettingsUtils.extractMap(PROPERTIES, config);

                Map<String, Object> properties = new HashMap<String, Object>();
                properties.putAll(config);
                properties.putAll(configProperties);

                String componentClass = null;
                Object componentObject = null;

                if ("agent".equals(componentType)) {
                    ReplicationAgent agent = componentFactory.createComponent(ReplicationAgent.class, properties, this);
                    componentClass = ReplicationAgent.class.getName();
                    componentObject = agent;

                } else if ("trigger".equals(componentType)) {

                    ReplicationTrigger trigger = componentFactory.createComponent(ReplicationTrigger.class, properties, this);

                    componentClass = ReplicationTrigger.class.getName();
                    componentObject = trigger;
                }

                if (componentObject != null && componentClass != null) {
                    if (componentObject instanceof ReplicationComponent) {
                        ((ReplicationComponent) componentObject).enable();
                    }

                    componentReg = context.registerService(componentClass, componentObject, props);


                    log.debug("activated component {} with name {}", componentType, name);
                }
            }
        }
    }

    @Deactivate
    private void deactivate(BundleContext context) {
        log.debug("deactivating component");
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

    private void refresh() {
        if (savedContext != null && savedConfig != null) {
            if (componentReg == null) {
                activate(savedContext, savedConfig);
            }
            else if (componentReg != null) {
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

    public <ComponentType> ComponentType getComponent(Class<ComponentType> type, String componentName) {
        if (type.isAssignableFrom(TransportAuthenticationProvider.class)) {
            return (ComponentType) transportAuthenticationProvider;
        }
        return null;
    }
}
