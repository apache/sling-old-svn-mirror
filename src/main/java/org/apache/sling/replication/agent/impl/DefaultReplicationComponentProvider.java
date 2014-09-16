package org.apache.sling.replication.agent.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.packaging.impl.exporter.LocalReplicationPackageExporterFactory;
import org.apache.sling.replication.packaging.impl.exporter.RemoteReplicationPackageExporterFactory;
import org.apache.sling.replication.packaging.impl.importer.LocalReplicationPackageImporterFactory;
import org.apache.sling.replication.packaging.impl.importer.RemoteReplicationPackageImporterFactory;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.rule.ReplicationRuleEngine;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilderFactory;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProviderFactory;
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
        @Reference(name = "transportAuthenticationProviderFactory", referenceInterface = TransportAuthenticationProviderFactory.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "replicationComponentListener", referenceInterface = ReplicationComponentListener.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
})
public class DefaultReplicationComponentProvider implements ReplicationComponentProvider {

    public static final String COMPONENT_TYPE = "type";
    public static final String NAME = "name";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ReplicationRuleEngine replicationRuleEngine;

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    @Reference
    private SlingRepository repository;

    @Reference
    private Packaging packaging;

    Map<String, ReplicationComponentListener> replicationComponentListenerMap = new ConcurrentHashMap<String, ReplicationComponentListener>();
    Map<String, ReplicationQueueProvider> replicationQueueProviderMap = new ConcurrentHashMap<String, ReplicationQueueProvider>();
    Map<String, ReplicationQueueDistributionStrategy> replicationQueueDistributionStrategyMap = new ConcurrentHashMap<String, ReplicationQueueDistributionStrategy>();
    Map<String, TransportAuthenticationProviderFactory> transportAuthenticationProviderFactoryMap = new ConcurrentHashMap<String, TransportAuthenticationProviderFactory>();
    Map<String, ReplicationPackageImporter> replicationPackageImporterMap = new ConcurrentHashMap<String, ReplicationPackageImporter>();
    Map<String, ReplicationPackageExporter> replicationPackageExporterMap = new ConcurrentHashMap<String, ReplicationPackageExporter>();

    public <ComponentType> ComponentType getComponent(Class<ComponentType> type, String componentName) {
        if (type.isAssignableFrom(ReplicationPackageExporter.class)) {
            return (ComponentType) replicationPackageExporterMap.get(componentName);
        } else if (type.isAssignableFrom(ReplicationPackageImporter.class)) {
            return (ComponentType) replicationPackageImporterMap.get(componentName);
        } else if (type.isAssignableFrom(ReplicationQueueProvider.class)) {
            return (ComponentType) replicationQueueProviderMap.get(componentName);
        } else if (type.isAssignableFrom(ReplicationQueueDistributionStrategy.class)) {
            return (ComponentType) replicationQueueDistributionStrategyMap.get(componentName);
        } else if (type.isAssignableFrom(TransportAuthenticationProviderFactory.class)) {
            return (ComponentType) transportAuthenticationProviderFactoryMap.get(componentName);
        }

        return null;
    }

    public <ComponentType> ComponentType createComponent(Class<ComponentType> type, Map<String, Object> properties) {
        try {
            if (type.isAssignableFrom(ReplicationAgent.class)) {
                return (ComponentType) createAgent(properties, this);
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

            String name = PropertiesUtil.toString(properties.get(CompactSimpleReplicationAgentFactory.NAME), String.valueOf(new Random().nextInt(1000)));

            String[] rules = PropertiesUtil.toStringArray(properties.get(CompactSimpleReplicationAgentFactory.RULES), new String[0]);

            boolean useAggregatePaths = PropertiesUtil.toBoolean(properties.get(CompactSimpleReplicationAgentFactory.USE_AGGREGATE_PATHS), true);

            boolean isPassive = PropertiesUtil.toBoolean(properties.get(CompactSimpleReplicationAgentFactory.IS_PASSIVE), false);

            // check configuration is valid
            if (name == null || packageExporter == null || packageImporter == null || queueProvider == null || queueDistributionStrategy == null) {
                log.error("could not create the agent with following bindings {}", Arrays.toString(new Object[]{name, packageExporter, packageImporter, queueProvider, queueDistributionStrategy}));
                return null;
            }

            return new SimpleReplicationAgent(name, rules, useAggregatePaths, isPassive,
                    packageImporter, packageExporter, queueProvider, queueDistributionStrategy, replicationEventFactory, replicationRuleEngine);

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
            Map<String, Object> authenticationFactoryProperties = extractMap("authenticationFactory", properties);
            TransportAuthenticationProviderFactory authenticationProviderFactory = createAuthenticationProviderFactory(authenticationFactoryProperties, componentProvider);

            Map<String, Object> builderProperties = extractMap("packageBuilder", properties);
            ReplicationPackageBuilder packageBuilder = createBuilder(builderProperties);

            return RemoteReplicationPackageExporterFactory.getInstance(properties, packageBuilder, authenticationProviderFactory);
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
            Map<String, Object> authenticationFactoryProperties = extractMap("authenticationFactory", properties);
            TransportAuthenticationProviderFactory authenticationProviderFactory = createAuthenticationProviderFactory(authenticationFactoryProperties, componentProvider);
            return RemoteReplicationPackageImporterFactory.getInstance(properties, authenticationProviderFactory);
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

    public TransportAuthenticationProviderFactory createAuthenticationProviderFactory(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(TransportAuthenticationProviderFactory.class, name);

        }

        return null;
    }

    public ReplicationPackageBuilder createBuilder(Map<String, Object> properties) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("vlt".equals(factory)) {
            return FileVaultReplicationPackageBuilderFactory.getInstance(properties, repository, packaging);
        }

        return null;
    }

    Map<String, Object> extractMap(String key, Map<String, Object> objectMap) {
        Object value = objectMap.get(key);

        if (value instanceof String[]) {
            return SettingsUtils.compactMap(SettingsUtils.toMap((String[]) value));

        }
        return null;
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

    private void bindTransportAuthenticationProviderFactory(TransportAuthenticationProviderFactory transportAuthenticationProviderFactory, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            transportAuthenticationProviderFactoryMap.put(name, transportAuthenticationProviderFactory);
            notifyListeners(transportAuthenticationProviderFactory, name, true);

        }

    }

    private void unbindTransportAuthenticationProviderFactory(TransportAuthenticationProviderFactory transportAuthenticationProviderFactory, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            transportAuthenticationProviderFactoryMap.remove(name);
            notifyListeners(transportAuthenticationProviderFactory, name, false);

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
