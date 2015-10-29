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
package org.apache.sling.discovery.base.its.setup;

import java.util.UUID;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.commons.scheduler.impl.QuartzScheduler;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.commons.threads.impl.DefaultThreadPoolManager;
import org.apache.sling.discovery.base.commons.BaseDiscoveryService;
import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.commons.ViewChecker;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistryImpl;
import org.apache.sling.discovery.base.connectors.ping.ConnectorRegistry;
import org.apache.sling.discovery.base.connectors.ping.ConnectorRegistryImpl;
import org.apache.sling.discovery.base.its.setup.mock.ArtificialDelay;
import org.apache.sling.discovery.base.its.setup.mock.FailingScheduler;
import org.apache.sling.discovery.commons.providers.spi.base.DummySlingSettingsService;
import org.apache.sling.settings.SlingSettingsService;

import junitx.util.PrivateAccessor;

public abstract class VirtualInstanceBuilder {

    private static Scheduler singletonScheduler = null;
    
    public static Scheduler getSingletonScheduler() throws Exception {
        if (singletonScheduler!=null) {
            return singletonScheduler;
        }
        final Scheduler newscheduler = new QuartzScheduler();
        final ThreadPoolManager tpm = new DefaultThreadPoolManager(null, null);
        try {
            PrivateAccessor.invoke(newscheduler, "bindThreadPoolManager",
                    new Class[] { ThreadPoolManager.class },
                    new Object[] { tpm });
        } catch (Throwable e1) {
            org.junit.Assert.fail(e1.toString());
        }
        OSGiMock.activate(newscheduler);
        singletonScheduler = newscheduler;
        return singletonScheduler;
    }

    private String debugName;
    protected ResourceResolverFactory factory;
    private boolean resetRepo;
    private String slingId = UUID.randomUUID().toString();
    private ClusterViewService clusterViewService;
    protected ViewChecker viewChecker;
    private AnnouncementRegistry announcementRegistry;
    private ConnectorRegistry connectorRegistry;
    private Scheduler scheduler;
    private BaseDiscoveryService discoveryService;
    private SlingSettingsService slingSettingsService;
    protected boolean ownRepository;
    private int minEventDelay = 1;
    protected VirtualInstanceBuilder hookedToBuilder;
    protected final ArtificialDelay delay = new ArtificialDelay();

    public VirtualInstanceBuilder() {
    }
    
    public ArtificialDelay getDelay() {
        return delay;
    }
    
    public VirtualInstanceBuilder newRepository(String path, boolean resetRepo) throws Exception {
        createNewRepository();
        ownRepository = true;
        this.resetRepo = resetRepo;
        setPath(path);
        return this;
    }
    
    public abstract VirtualInstanceBuilder createNewRepository() throws Exception;
    
    public VirtualInstanceBuilder useRepositoryOf(VirtualInstance other) throws Exception {
        return useRepositoryOf(other.getBuilder());
    }
    
    public VirtualInstanceBuilder useRepositoryOf(VirtualInstanceBuilder other) throws Exception {
        factory = other.factory;
        hookedToBuilder = other;
        ownRepository = false;
        return this;
    }

    public VirtualInstanceBuilder setConnectorPingTimeout(int connectorPingTimeout) {
        getConnectorConfig().setViewCheckTimeout(connectorPingTimeout);
        return this;
    }
    
    public VirtualInstanceBuilder setConnectorPingInterval(int connectorPingInterval) {
        getConnectorConfig().setViewCheckInterval(connectorPingInterval);
        return this;
    }

    public boolean isResetRepo() {
        return resetRepo;
    }

    public String getSlingId() {
        return slingId;
    }

    public String getDebugName() {
        return debugName;
    }

    public ResourceResolverFactory getResourceResolverFactory() {
        return factory;
    }

    public ClusterViewService getClusterViewService() {
        if (clusterViewService==null) {
            clusterViewService = createClusterViewService();
        }
        return clusterViewService;
    }
    
    protected abstract ClusterViewService createClusterViewService();

    public ViewChecker getViewChecker() throws Exception {
        if (viewChecker==null) {
            viewChecker = createViewChecker();
        }
        return viewChecker;
    }
    
    public AnnouncementRegistry getAnnouncementRegistry() {
        if (announcementRegistry==null) {
            announcementRegistry = createAnnouncementRegistry();
        }
        return announcementRegistry;
    }
    
    protected AnnouncementRegistry createAnnouncementRegistry() {
        return AnnouncementRegistryImpl.testConstructor( 
                getResourceResolverFactory(), getSlingSettingsService(), getConnectorConfig());
    }

    public ConnectorRegistry getConnectorRegistry() {
        if (connectorRegistry==null) {
            connectorRegistry = createConnectorRegistry();
        }
        return connectorRegistry;
    }
    
    protected ConnectorRegistry createConnectorRegistry() {
        return ConnectorRegistryImpl.testConstructor(announcementRegistry, getConnectorConfig());
    }

    protected abstract ViewChecker createViewChecker() throws Exception;

    protected abstract VirtualInstanceBuilder setPath(String string);

    public VirtualInstanceBuilder setDebugName(String debugName) {
        this.debugName = debugName;
        delay.setDebugName(debugName);
        return this;
    }

    public abstract ModifiableTestBaseConfig getConnectorConfig();

    public void setScheduler(Scheduler singletonScheduler) {
        this.scheduler = singletonScheduler;
    }
    
    public Scheduler getScheduler() throws Exception {
        if (scheduler == null) {
            scheduler = getSingletonScheduler();
        }
        return scheduler;
    }

    public BaseDiscoveryService getDiscoverService() throws Exception {
        if (discoveryService==null) {
            discoveryService = createDiscoveryService();
        }
        return discoveryService;
    }

    protected abstract BaseDiscoveryService createDiscoveryService() throws Exception;
    
    protected SlingSettingsService getSlingSettingsService() {
        if (slingSettingsService==null) {
            slingSettingsService = createSlingSettingsService();
        }
        return slingSettingsService;
    }

    protected SlingSettingsService createSlingSettingsService() {
        return new DummySlingSettingsService(getSlingId());
    }

    public abstract Object[] getAdditionalServices(VirtualInstance instance) throws Exception;

    public VirtualInstanceBuilder setMinEventDelay(int minEventDelay) {
        this.minEventDelay = minEventDelay;
        return this;
    }

    public int getMinEventDelay() {
        return minEventDelay;
    }

    public VirtualInstance build() throws Exception {
        return new VirtualInstance(this);
    }

    public VirtualInstanceBuilder setSlingId(String slingId) {
        this.slingId = slingId;
        return this;
    }
    
    public VirtualInstanceBuilder withFailingScheduler(boolean useFailingScheduler) {
        if (useFailingScheduler) {
            this.scheduler = new FailingScheduler();
        }
        return this;
    }

    protected abstract void resetRepo() throws Exception;

}
