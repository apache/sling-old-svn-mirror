package org.apache.sling.replication.agent.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.apache.sling.replication.packaging.impl.exporter.RemoteReplicationPackageExporter;
import org.apache.sling.replication.packaging.impl.exporter.RemoteReplicationPackageExporterFactory;
import org.apache.sling.replication.packaging.impl.importer.LocalReplicationPackageImporterFactory;
import org.apache.sling.replication.packaging.impl.importer.RemoteReplicationPackageImporter;
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
})
public class DefaultReplicationComponentProvider implements ReplicationComponentProvider {

    public static final String COMPONENT_TYPE = "type";
    public static final String NAME = "name";

    private final Logger log = LoggerFactory.getLogger(getClass());


    Map<String, ReplicationQueueProvider> replicationQueueProviderMap = new ConcurrentHashMap<String, ReplicationQueueProvider>();
    Map<String, ReplicationQueueDistributionStrategy> replicationQueueDistributionStrategyMap = new ConcurrentHashMap<String, ReplicationQueueDistributionStrategy>();
    Map<String, TransportAuthenticationProvider> transportAuthenticationProviderMap = new ConcurrentHashMap<String, TransportAuthenticationProvider>();
    Map<String, ReplicationPackageImporter> replicationPackageImporterMap = new ConcurrentHashMap<String, ReplicationPackageImporter>();
    Map<String, ReplicationPackageExporter> replicationPackageExporterMap = new ConcurrentHashMap<String, ReplicationPackageExporter>();
    private BundleContext bundleContext;



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

    private void bindReplicationQueueProvider(ReplicationQueueProvider replicationQueueProvider, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationQueueProviderMap.put(name, replicationQueueProvider);
        }
    }

    private void unbindReplicationQueueProvider(ReplicationQueueProvider replicationQueueProvider, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationQueueProviderMap.remove(name);
        }
    }

    private void bindReplicationQueueDistributionStrategy(ReplicationQueueDistributionStrategy replicationQueueDistributionStrategy, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationQueueDistributionStrategyMap.put(name, replicationQueueDistributionStrategy);
        }
    }

    private void unbindReplicationQueueDistributionStrategy(ReplicationQueueDistributionStrategy replicationQueueDistributionStrategy, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationQueueDistributionStrategyMap.remove(name);
        }
    }

    private void bindTransportAuthenticationProvider(TransportAuthenticationProvider transportAuthenticationProvider, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            transportAuthenticationProviderMap.put(name, transportAuthenticationProvider);

        }

    }

    private void unbindTransportAuthenticationProvider(TransportAuthenticationProvider transportAuthenticationProvider, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            transportAuthenticationProviderMap.remove(name);

        }
    }

    private void bindReplicationPackageImporter(ReplicationPackageImporter replicationPackageImporter, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationPackageImporterMap.put(name, replicationPackageImporter);

        }
    }

    private void unbindReplicationPackageImporter(ReplicationPackageImporter replicationPackageImporter, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationPackageImporterMap.remove(name);
        }
    }

    private void bindReplicationPackageExporter(ReplicationPackageExporter replicationPackageExporter, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationPackageExporterMap.put(name, replicationPackageExporter);
        }
    }

    private void unbindReplicationPackageExporter(ReplicationPackageExporter replicationPackageExporter, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            replicationPackageExporterMap.remove(name);
        }

    }

}
