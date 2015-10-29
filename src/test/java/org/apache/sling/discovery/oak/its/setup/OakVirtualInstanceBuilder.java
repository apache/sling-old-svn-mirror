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
package org.apache.sling.discovery.oak.its.setup;

import static org.junit.Assert.fail;

import javax.jcr.Session;

import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.discovery.base.commons.BaseDiscoveryService;
import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.commons.ViewChecker;
import org.apache.sling.discovery.base.its.setup.ModifiableTestBaseConfig;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.discovery.base.its.setup.mock.MockFactory;
import org.apache.sling.discovery.commons.providers.spi.base.IdMapService;
import org.apache.sling.discovery.commons.providers.spi.base.OakBacklogClusterSyncService;
import org.apache.sling.discovery.commons.providers.spi.base.RepositoryTestHelper;
import org.apache.sling.discovery.commons.providers.spi.base.SyncTokenService;
import org.apache.sling.discovery.oak.OakDiscoveryService;
import org.apache.sling.discovery.oak.cluster.OakClusterViewService;
import org.apache.sling.discovery.oak.pinger.OakViewChecker;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junitx.util.PrivateAccessor;

public class OakVirtualInstanceBuilder extends VirtualInstanceBuilder {

    NodeStore nodeStore;
    private String path;
    private OakTestConfig config;
    private IdMapService idMapService;
    private OakViewChecker oakViewChecker;
    private SimulatedLeaseCollection leaseCollection;
    private OakBacklogClusterSyncService consistencyService;
    private SyncTokenService syncTokenService;
    
    public SimulatedLeaseCollection getSimulatedLeaseCollection() {
        return leaseCollection;
    }
    
    @Override
    public VirtualInstanceBuilder createNewRepository() throws Exception {
        nodeStore = new MemoryNodeStore();
        SlingRepository repository = RepositoryTestHelper.newOakRepository(nodeStore);
        factory = MockFactory.mockResourceResolverFactory(repository);
        leaseCollection = new SimulatedLeaseCollection();
        return this;
    }
    
    @Override
    public VirtualInstanceBuilder useRepositoryOf(VirtualInstanceBuilder other) throws Exception {
        if (!(other instanceof OakVirtualInstanceBuilder)) {
            throw new IllegalArgumentException("other must be of type OakVirtualInstanceBuilder but is: "+other);
        }
        OakVirtualInstanceBuilder otherOakbuilder = (OakVirtualInstanceBuilder)other;
        nodeStore = otherOakbuilder.nodeStore;
        SlingRepository repository = RepositoryTestHelper.newOakRepository(nodeStore);
        factory = MockFactory.mockResourceResolverFactory(repository);
        leaseCollection = otherOakbuilder.leaseCollection;
        hookedToBuilder = other;
        ownRepository = false;
        return this;
    }

    @Override
    public VirtualInstanceBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public Object[] getAdditionalServices(VirtualInstance instance) throws Exception {
        return null;
    }
    
    public IdMapService getIdMapService() {
        if (idMapService==null) {
            idMapService = createIdMapService();
        }
        return idMapService;
    }

    private IdMapService createIdMapService() {
        return IdMapService.testConstructor(getConfig(), getSlingSettingsService(), getResourceResolverFactory());
    }

    @Override
    protected ClusterViewService createClusterViewService() {
        return OakClusterViewService.testConstructor(getSlingSettingsService(), getResourceResolverFactory(), getIdMapService(), getConfig());
    }
    
    OakTestConfig getConfig() {
        if (config==null) {
            config = createConfig();
        }
        return config;
    }
    
    @Override
    public ModifiableTestBaseConfig getConnectorConfig() {
        return getConfig();
    }
    
    private OakTestConfig createConfig() {
        OakTestConfig c = new OakTestConfig();
        c.setDiscoveryResourcePath(path);
        return c;
    }
    
    @Override
    protected ViewChecker createViewChecker() throws Exception {
        getOakViewChecker();
        return new ViewChecker() {
            
            private final Logger logger = LoggerFactory.getLogger(getClass());

            private SimulatedLease lease = new SimulatedLease(getResourceResolverFactory(), leaseCollection, getSlingId());
            
            protected void activate(ComponentContext c) throws Throwable {
                OakViewChecker pinger = getOakViewChecker();
                PrivateAccessor.invoke(pinger, "activate", new Class[] {ComponentContext.class}, new Object[] {c});
            }
            
            @Override
            public void checkView() {
                try {
                    lease.updateDescriptor(getConfig());
                } catch (Exception e) {
                    logger.error("run: could not update lease: "+e);
                }
            }
            
            public void run() {
                heartbeatAndCheckView();
            }
            
            @Override
            public void heartbeatAndCheckView() {
//                next step is try to simulate the logic
//                where no heartbeat means no descriptor yet
//                one heartbeat means i'm visible for others
//                as soon as I see others the descriptor is updated
                try {
                    lease.updateLeaseAndDescriptor(getConfig());
                } catch (Exception e) {
                    logger.error("run: could not update lease: "+e, e);
                }
                try{
                    getOakViewChecker().run();
                } catch(Exception e) {
                    logger.error("run: could not ping: "+e, e);
                }
                if (!getIdMapService().isInitialized()) {
                    if (!getIdMapService().waitForInit(1500)) {
                        fail("init didnt work");
                    }
                }
            }
        };
    }

    private OakViewChecker getOakViewChecker() throws Exception {
        if (oakViewChecker==null) {
            oakViewChecker = createOakViewChecker() ;
        }
        return oakViewChecker;
    }

    private OakViewChecker createOakViewChecker() throws Exception {
        return OakViewChecker.testConstructor(getSlingSettingsService(), getResourceResolverFactory(), getConnectorRegistry(), getAnnouncementRegistry(), getScheduler(), getConfig());
    }

    private OakBacklogClusterSyncService getOakBacklogClusterSyncService() throws Exception {
        if (consistencyService == null) {
            consistencyService = createOakBacklogClusterSyncService();
        }
        return consistencyService;
    }
    
    private OakBacklogClusterSyncService createOakBacklogClusterSyncService() {
        return OakBacklogClusterSyncService.testConstructorAndActivate(getConfig(), getIdMapService(), getSlingSettingsService(), getResourceResolverFactory());
    }

    private SyncTokenService getSyncTokenService() throws Exception {
        if (syncTokenService == null) {
            syncTokenService = createSyncTokenService();
        }
        return syncTokenService;
    }
    
    private SyncTokenService createSyncTokenService() {
        return SyncTokenService.testConstructorAndActivate(getConfig(), getResourceResolverFactory(), getSlingSettingsService());
    }

    @Override
    protected BaseDiscoveryService createDiscoveryService() throws Exception {
        return OakDiscoveryService.testConstructor(
                getSlingSettingsService(), 
                getAnnouncementRegistry(), 
                getConnectorRegistry(), 
                getClusterViewService(), 
                getConfig(), 
                getOakViewChecker(), 
                getScheduler(), 
                getIdMapService(), 
                getOakBacklogClusterSyncService(),
                getSyncTokenService(),
                getResourceResolverFactory());
    }

    @Override
    public VirtualInstance build() throws Exception {
        if (path==null) {
            if (ownRepository) {
                setPath("/var/discovery/impl/");
                getConfig().setDiscoveryResourcePath("/var/discovery/impl/");
            } else {
                OakVirtualInstanceBuilder other = (OakVirtualInstanceBuilder) hookedToBuilder;
                this.path = other.path;
                getConfig().setDiscoveryResourcePath(other.path);
            }
        }
        if (path==null) {
            throw new IllegalStateException("no path set");
        }
        if (!path.startsWith("/")) {
            throw new IllegalStateException("path must start with /: "+path);
        }
        if (!path.endsWith("/")) {
            throw new IllegalStateException("path must end with /: "+path);
        }
        VirtualInstance result = new VirtualInstance(this) {

        };
        return result;
    }
    
    @Override
    protected void resetRepo() throws Exception {
        leaseCollection.reset();
        ResourceResolver rr = null;
        Session l = null;
        try {
            rr = factory.getAdministrativeResourceResolver(null);
            l = rr.adaptTo(Session.class);
            l.removeItem("/var");
            l.save();
            l.logout();
        } catch (Exception e) {
            l.refresh(false);
            l.logout();
        }
    }
}
