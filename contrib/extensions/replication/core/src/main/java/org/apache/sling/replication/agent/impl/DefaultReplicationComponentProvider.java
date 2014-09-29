package org.apache.sling.replication.agent.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationComponentProvider;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.packaging.impl.exporter.LocalReplicationPackageExporterFactory;
import org.apache.sling.replication.packaging.impl.exporter.RemoteReplicationPackageExporterFactory;
import org.apache.sling.replication.packaging.impl.importer.LocalReplicationPackageImporterFactory;
import org.apache.sling.replication.packaging.impl.importer.RemoteReplicationPackageImporterFactory;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilderFactory;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.authentication.impl.UserCredentialsTransportAuthenticationProvider;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.apache.sling.replication.trigger.impl.ChainReplicateReplicationTrigger;
import org.apache.sling.replication.trigger.impl.RemoteEventReplicationTrigger;
import org.apache.sling.replication.trigger.impl.ResourceEventReplicationTrigger;
import org.apache.sling.replication.trigger.impl.ScheduledReplicationTrigger;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(ReplicationComponentProvider.class)
@Property(name = "name", value = "default")
@References({
        @Reference(name = "replicationPackageImporter", referenceInterface = ReplicationPackageImporter.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "replicationPackageExporter", referenceInterface = ReplicationPackageExporter.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "replicationQueueProvider", referenceInterface = ReplicationQueueProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "replicationQueueDistributionStrategy", referenceInterface = ReplicationQueueDistributionStrategy.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "transportAuthenticationProvider", referenceInterface = TransportAuthenticationProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "replicationComponentListener", referenceInterface = ReplicationComponentListener.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
})
public class DefaultReplicationComponentProvider implements ReplicationComponentProvider {

    public static final String COMPONENT_TYPE = "type";
    public static final String NAME = "name";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    @Reference
    private SlingRepository repository;

    @Reference
    private Packaging packaging;

    @Reference
    private Scheduler scheduler;

    Map<String, ReplicationComponentListener> replicationComponentListenerMap = new ConcurrentHashMap<String, ReplicationComponentListener>();
    Map<String, ReplicationQueueProvider> replicationQueueProviderMap = new ConcurrentHashMap<String, ReplicationQueueProvider>();
    Map<String, ReplicationQueueDistributionStrategy> replicationQueueDistributionStrategyMap = new ConcurrentHashMap<String, ReplicationQueueDistributionStrategy>();
    Map<String, TransportAuthenticationProvider> transportAuthenticationProviderMap = new ConcurrentHashMap<String, TransportAuthenticationProvider>();
    Map<String, ReplicationPackageImporter> replicationPackageImporterMap = new ConcurrentHashMap<String, ReplicationPackageImporter>();
    Map<String, ReplicationPackageExporter> replicationPackageExporterMap = new ConcurrentHashMap<String, ReplicationPackageExporter>();
    private BundleContext bundleContext;

    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public <ComponentType> ComponentType getComponent(Class<ComponentType> type, String componentName) {
        if (type.isAssignableFrom(ReplicationPackageExporter.class)) {
            return (ComponentType) replicationPackageExporterMap.get(componentName);
        } else if (type.isAssignableFrom(ReplicationPackageImporter.class)) {
            return (ComponentType) replicationPackageImporterMap.get(componentName);
        } else if (type.isAssignableFrom(ReplicationQueueProvider.class)) {
            return (ComponentType) replicationQueueProviderMap.get(componentName);
        } else if (type.isAssignableFrom(ReplicationQueueDistributionStrategy.class)) {
            return (ComponentType) replicationQueueDistributionStrategyMap.get(componentName);
        } else if (type.isAssignableFrom(TransportAuthenticationProvider.class)) {
            return (ComponentType) transportAuthenticationProviderMap.get(componentName);
        }

        return null;
    }

    public <ComponentType> ComponentType createComponent(Class<ComponentType> type, Map<String, Object> properties) {
        try {
            if (type.isAssignableFrom(ReplicationAgent.class)) {
                return (ComponentType) createAgent(properties, this);
            } else if (type.isAssignableFrom(ReplicationTrigger.class)) {
                return (ComponentType) createTrigger(properties, this);
            }
        } catch (Throwable t) {
            log.error("Cannot create agent", t);

        }
        return null;
    }


    public ReplicationAgent createAgent(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "simple");

        if ("simple".equals(factory)) {
            if (log.isDebugEnabled()) {
                log.debug("creating simple agent");
                for (Map.Entry<String, Object> e : properties.entrySet()) {
                    Object value = e.getValue();
                    log.info(e.getKey() + " -> " + (value != null && value.getClass().isArray() ? Arrays.toString((Object[]) value) : value));
                }
            }
            Map<String, Object> importerProperties = extractMap("packageImporter", properties);
            ReplicationPackageImporter packageImporter = createImporter(importerProperties, componentProvider);

            Map<String, Object> exporterProperties = extractMap("packageExporter", properties);
            ReplicationPackageExporter packageExporter = createExporter(exporterProperties, componentProvider);

            Map<String, Object> queueDistributionStrategyProperties = extractMap("queueDistributionStrategy", properties);
            ReplicationQueueDistributionStrategy queueDistributionStrategy = createDistributionStrategy(queueDistributionStrategyProperties, componentProvider);

            Map<String, Object> queueProviderProperties = extractMap("queueProvider", properties);
            ReplicationQueueProvider queueProvider = createQueueProvider(queueProviderProperties, componentProvider);

            List<Map<String, Object>> triggersProperties = extractMapList("trigger", properties);
            List<ReplicationTrigger> triggers = createTriggerList(triggersProperties, componentProvider);

            String name = PropertiesUtil.toString(properties.get(CompactSimpleReplicationAgentFactory.NAME), String.valueOf(new Random().nextInt(1000)));

            boolean useAggregatePaths = PropertiesUtil.toBoolean(properties.get(CompactSimpleReplicationAgentFactory.USE_AGGREGATE_PATHS), true);

            boolean isPassive = PropertiesUtil.toBoolean(properties.get(CompactSimpleReplicationAgentFactory.IS_PASSIVE), false);

            // check configuration is valid
            if (name == null || packageExporter == null || packageImporter == null || queueProvider == null || queueDistributionStrategy == null) {
                log.error("could not create the agent with following bindings {}", Arrays.toString(new Object[]{name, packageExporter, packageImporter, queueProvider, queueDistributionStrategy}));
                return null;
            }

            return new SimpleReplicationAgent(name, useAggregatePaths, isPassive,
                    packageImporter, packageExporter, queueProvider, queueDistributionStrategy, replicationEventFactory, triggers);

        }

        return null;

    }


    public ReplicationPackageExporter createExporter(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(ReplicationPackageExporter.class, name);

        } else if ("local".equals(factory)) {
            Map<String, Object> builderProperties = extractMap("packageBuilder", properties);
            ReplicationPackageBuilder packageBuilder = createBuilder(builderProperties);
            return LocalReplicationPackageExporterFactory.getInstance(packageBuilder);
        } else if ("remote".equals(factory)) {
            Map<String, Object> authenticationProviderProperties = extractMap("authenticationProvider", properties);
            TransportAuthenticationProvider authenticationProvider = createTransportAuthenticationProvider(authenticationProviderProperties, componentProvider);

            Map<String, Object> builderProperties = extractMap("packageBuilder", properties);
            ReplicationPackageBuilder packageBuilder = createBuilder(builderProperties);

            return RemoteReplicationPackageExporterFactory.getInstance(properties, packageBuilder, authenticationProvider);
        }

        return null;
    }

    public ReplicationPackageImporter createImporter(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(ReplicationPackageImporter.class, name);
        } else if ("local".equals(factory)) {
            Map<String, Object> builderProperties = extractMap("packageBuilder", properties);
            ReplicationPackageBuilder packageBuilder = createBuilder(builderProperties);
            return LocalReplicationPackageImporterFactory.getInstance(properties, packageBuilder, replicationEventFactory);
        } else if ("remote".equals(factory)) {
            Map<String, Object> authenticationProviderProperties = extractMap("authenticationProvider", properties);
            TransportAuthenticationProvider authenticationProvider = createTransportAuthenticationProvider(authenticationProviderProperties, componentProvider);

            return RemoteReplicationPackageImporterFactory.getInstance(properties, authenticationProvider);
        }

        return null;
    }

    public ReplicationQueueProvider createQueueProvider(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(ReplicationQueueProvider.class, name);
        }

        return null;
    }

    public ReplicationQueueDistributionStrategy createDistributionStrategy(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(ReplicationQueueDistributionStrategy.class, name);

        }

        return null;
    }

    public TransportAuthenticationProvider createTransportAuthenticationProvider(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(TransportAuthenticationProvider.class, name);

        } else if ("user".equals(factory)) {
            return new UserCredentialsTransportAuthenticationProvider(properties);
        }

        return null;
    }

    public ReplicationPackageBuilder createBuilder(Map<String, Object> properties) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("vlt".equals(factory)) {
            return FileVaultReplicationPackageBuilderFactory.getInstance(properties, repository, packaging, replicationEventFactory);
        }

        return null;
    }


    private ReplicationTrigger createTrigger(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(ReplicationTrigger.class, name);

        } else if (RemoteEventReplicationTrigger.TYPE.equals(factory)) {
            Map<String, Object> authenticationProviderProperties = extractMap("authenticationProvider", properties);

            TransportAuthenticationProvider authenticationProvider = createTransportAuthenticationProvider(authenticationProviderProperties, componentProvider);
            return new RemoteEventReplicationTrigger(properties, authenticationProvider, scheduler);
        } else if (ResourceEventReplicationTrigger.TYPE.equals(factory)) {
            return new ResourceEventReplicationTrigger(properties, bundleContext);
        } else if (ScheduledReplicationTrigger.TYPE.equals(factory)) {
            return new ScheduledReplicationTrigger(properties, scheduler);
        } else if (ChainReplicateReplicationTrigger.TYPE.equals(factory)) {
            return new ChainReplicateReplicationTrigger(properties, bundleContext);
        }

        return null;
    }

    private List<ReplicationTrigger> createTriggerList(List<Map<String, Object>> triggersProperties, ReplicationComponentProvider componentProvider) {
        List<ReplicationTrigger> triggers = new ArrayList<ReplicationTrigger>();
        for (Map<String, Object> properties : triggersProperties) {
            triggers.add(createTrigger(properties, componentProvider));
        }

        return triggers;
    }

    Map<String, Object> extractMap(String key, Map<String, Object> objectMap) {
        return SettingsUtils.extractMap(key, objectMap);
    }

    List<Map<String, Object>> extractMapList(String key, Map<String, Object> objectMap) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (String mapKey : objectMap.keySet()) {
            if (mapKey.startsWith(key)) {
                result.add(SettingsUtils.extractMap(mapKey, objectMap));
            }
        }
        return result;
    }

    private void bindReplicationQueueProvider(ReplicationQueueProvider replicationQueueProvider, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationQueueProviderMap.put(name, replicationQueueProvider);
            notifyListeners(replicationQueueProvider, name, true);
        }
    }

    private void unbindReplicationQueueProvider(ReplicationQueueProvider replicationQueueProvider, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationQueueProviderMap.remove(name);
            notifyListeners(replicationQueueProvider, name, false);
        }
    }

    private void bindReplicationQueueDistributionStrategy(ReplicationQueueDistributionStrategy replicationQueueDistributionStrategy, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationQueueDistributionStrategyMap.put(name, replicationQueueDistributionStrategy);
            notifyListeners(replicationQueueDistributionStrategy, name, true);

        }
    }

    private void unbindReplicationQueueDistributionStrategy(ReplicationQueueDistributionStrategy replicationQueueDistributionStrategy, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationQueueDistributionStrategyMap.remove(name);
            notifyListeners(replicationQueueDistributionStrategy, name, false);

        }
    }

    private void bindTransportAuthenticationProvider(TransportAuthenticationProvider transportAuthenticationProvider, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            transportAuthenticationProviderMap.put(name, transportAuthenticationProvider);
            notifyListeners(transportAuthenticationProvider, name, true);

        }

    }

    private void unbindTransportAuthenticationProvider(TransportAuthenticationProvider transportAuthenticationProvider, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            transportAuthenticationProviderMap.remove(name);
            notifyListeners(transportAuthenticationProvider, name, false);

        }
    }

    private void bindReplicationPackageImporter(ReplicationPackageImporter replicationPackageImporter, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationPackageImporterMap.put(name, replicationPackageImporter);
            notifyListeners(replicationPackageImporter, name, true);

        }
    }

    private void unbindReplicationPackageImporter(ReplicationPackageImporter replicationPackageImporter, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationPackageImporterMap.remove(name);
            notifyListeners(replicationPackageImporter, name, false);
        }
    }

    private void bindReplicationPackageExporter(ReplicationPackageExporter replicationPackageExporter, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationPackageExporterMap.put(name, replicationPackageExporter);
            notifyListeners(replicationPackageExporter, name, true);
        }
    }

    private void unbindReplicationPackageExporter(ReplicationPackageExporter replicationPackageExporter, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationPackageExporterMap.remove(name);
            notifyListeners(replicationPackageExporter, name, false);
        }

    }

    private void bindReplicationComponentListener(ReplicationComponentListener replicationComponentListener, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationComponentListenerMap.put(name, replicationComponentListener);
        }
    }

    private void unbindReplicationComponentListener(ReplicationComponentListener replicationComponentListener, Map<String, Object> config) {
        String name = (String) config.get("name");
        if (name != null) {
            replicationComponentListenerMap.remove(name);
        }
    }

    <ComponentType> void notifyListeners(ComponentType component, String componentName, boolean isBinding) {
        for (ReplicationComponentListener listener : replicationComponentListenerMap.values()) {
            try {
                if (isBinding) {
                    listener.componentBind(component, componentName);
                } else {
                    listener.componentUnbind(component, componentName);
                }
            } catch (Throwable t) {
                log.error("Error while delivering event to ReplicationComponentListener", t);
            }
        }
    }


}
