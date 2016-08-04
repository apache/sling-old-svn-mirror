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
package org.apache.sling.discovery.impl.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.discovery.base.commons.BaseDiscoveryService;
import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.commons.UndefinedClusterViewException;
import org.apache.sling.discovery.base.commons.ViewChecker;
import org.apache.sling.discovery.base.its.setup.ModifiableTestBaseConfig;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.discovery.base.its.setup.mock.DummyResourceResolverFactory;
import org.apache.sling.discovery.commons.providers.spi.base.SyncTokenService;
import org.apache.sling.discovery.impl.DiscoveryServiceImpl;
import org.apache.sling.discovery.impl.cluster.ClusterViewServiceImpl;
import org.apache.sling.discovery.impl.cluster.voting.VotingHandler;
import org.apache.sling.discovery.impl.common.heartbeat.HeartbeatHandler;
import org.apache.sling.discovery.impl.common.resource.EstablishedInstanceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullJR2VirtualInstanceBuilder extends VirtualInstanceBuilder {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String path;

    private TestConfig config;

    private Object[] additionalServices;

    private VotingEventListener observationListener;

    private ObservationManager observationManager;

    private VotingHandler votingHandler;

    private SyncTokenService syncTokenService;

    @Override
    public VirtualInstanceBuilder createNewRepository() throws Exception {
        DummyResourceResolverFactory dummyFactory = new DummyResourceResolverFactory();
        dummyFactory.setArtificialDelay(delay);
        this.factory = dummyFactory;
        return this;
    }
    
    @Override
    public VirtualInstanceBuilder useRepositoryOf(VirtualInstanceBuilder other) throws Exception {
        super.useRepositoryOf(other);
        DummyResourceResolverFactory dummyFactory = new DummyResourceResolverFactory();
        DummyResourceResolverFactory originalFactory = (DummyResourceResolverFactory) this.factory;
        // force repository to be created now..
        originalFactory.getAdministrativeResourceResolver(null);
        dummyFactory.setSlingRepository(originalFactory.getSlingRepository());
        dummyFactory.setArtificialDelay(getDelay());
        this.factory = dummyFactory; 
        return this;
    }

    @Override
    public VirtualInstanceBuilder setPath(String path) {
        if (ownRepository) {
            // then that's fine
            this.path = path;
        } else {
            // then that's not fine
            throw new IllegalStateException("cannot set path on inherited repo");
        }
        return this;
    }
    
    TestConfig getConfig() {
        if (config==null) {
            config = createConfig();
        }
        return config;
    }
    
    private TestConfig createConfig() {
        TestConfig c = new TestConfig(path);
        return c;
    }
    
    @Override
    public ModifiableTestBaseConfig getConnectorConfig() {
        return getConfig();
    }
    
    @Override
    protected ViewChecker createViewChecker() throws Exception {
        return HeartbeatHandler.testConstructor(getSlingSettingsService(), getResourceResolverFactory(), getAnnouncementRegistry(), getConnectorRegistry(), getConfig(), getScheduler(), getVotingHandler());
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
        return DiscoveryServiceImpl.testConstructor(getResourceResolverFactory(), getAnnouncementRegistry(), getConnectorRegistry(), (ClusterViewServiceImpl) getClusterViewService(), getHeartbeatHandler(), getSlingSettingsService(), getScheduler(), getConfig(), getSyncTokenService());
    }
    
    @Override
    protected ClusterViewService createClusterViewService() {
        return ClusterViewServiceImpl.testConstructor(getSlingSettingsService(), getResourceResolverFactory(), getConfig());
    }

    private HeartbeatHandler getHeartbeatHandler() throws Exception {
        if (viewChecker==null) {
            throw new IllegalStateException("heartbeatHandler not yet initialized");
        }
        return (HeartbeatHandler) getViewChecker();
    }
    
    @Override
    public Object[] getAdditionalServices(VirtualInstance instance) throws Exception {
        if (additionalServices==null) {
            additionalServices = createAdditionalServices(instance);
        }
        return additionalServices;
    }
    
    VotingHandler getVotingHandler() throws Exception {
        if (votingHandler == null) {
            votingHandler = createVotingHandler();
        }
        return votingHandler;
    }

    private VotingHandler createVotingHandler() throws Exception {
        return VotingHandler.testConstructor(getSlingSettingsService(), getResourceResolverFactory(), getConfig());
    }

    private Object[] createAdditionalServices(VirtualInstance instance) throws Exception {
        Object[] additionals = new Object[1];
        
        additionals[0] = getVotingHandler();
        
        observationListener = new VotingEventListener(instance, votingHandler, getSlingId());
        ResourceResolver resourceResolver = getResourceResolverFactory()
                .getAdministrativeResourceResolver(null);
        Session session = resourceResolver.adaptTo(Session.class);
        observationManager = session.getWorkspace()
                .getObservationManager();
        observationManager.addEventListener(
                observationListener
                , Event.NODE_ADDED | Event.NODE_REMOVED | Event.NODE_MOVED
                        | Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED
                        | Event.PROPERTY_REMOVED | Event.PERSIST, "/", true,
                null,
                null, false);

        return additionals;
    }
    
    void stopVoting() {
        if (observationListener!=null) {
            logger.info("stopVoting: stopping voting of slingId="+getSlingId());
            if (observationManager != null) {
                logger.info("stop: removing listener for slingId="+getSlingId()+": "+observationListener);
                try {
                    observationManager.removeEventListener(observationListener);
                } catch (RepositoryException e) {
                    logger.error("stopVoting: could not remove listener for slingId="+getSlingId()+": "+observationListener+", "+e, e);
                }
            }
            logger.info("stopVoting: stopping observation listener of slingId="+getSlingId());
            observationListener.stop();
            observationListener = null;
            logger.info("stopVoting: stopped observation listener of slingId="+getSlingId());
        } else {
            logger.info("stopVoting: observation listener was null for slingId="+getSlingId());
        }
    }

    public FullJR2VirtualInstance fullBuild() throws Exception {
        return (FullJR2VirtualInstance) build();
    }
    
    @Override
    public VirtualInstance build() throws Exception {
        if (path==null) {
            if (ownRepository) {
                setPath("/var/discovery/impl/");
                getConfig().setPath("/var/discovery/impl/");
            } else {
                FullJR2VirtualInstanceBuilder other = (FullJR2VirtualInstanceBuilder) hookedToBuilder;
                this.path = other.path;
                getConfig().setPath(other.path);
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
        return new FullJR2VirtualInstance(this) {
            @Override
            public void stop() throws Exception {
                if ((observationListener != null) && (observationManager != null)) {
                    logger.info("stop: removing listener for slingId="+slingId+": "+observationListener);
                    observationManager.removeEventListener(observationListener);
                } else {
                    logger.warn("stop: could not remove listener for slingId="+slingId+", debugName="+debugName+", observationManager="+observationManager+", observationListener="+observationListener);
                }
                if (observationListener!=null) {
                    observationListener.stop();
                }
                super.stop();
            }
            
            @Override
            public void assertEstablishedView() {
                super.assertEstablishedView();
                try {
                    assertEquals(EstablishedInstanceDescription.class, this
                            .getClusterViewService().getLocalClusterView().getInstances().get(0)
                            .getClass());
                } catch (UndefinedClusterViewException e) {
                    fail("Undefined clusterView: "+e);
                }
            }
        };
    }
    
    @Override
    protected void resetRepo() throws Exception {
        logger.info("resetRepo: start, logging in");
        Session l = RepositoryProvider.instance().getRepository()
                .loginAdministrative(null);
        try {
            logger.info("resetRepo: removing '/var' ...");
            l.removeItem("/var");
            logger.info("resetRepo: saving...");
            l.save();
            logger.info("resetRepo: logging out...");
            l.logout();
            logger.info("resetRepo: done.");
        } catch (Exception e) {
            logger.error("resetRepo: Exception while trying to remove /var: "+e, e);
            l.refresh(false);
            logger.info("resetRepo: logging out after exception");
            l.logout();
            logger.info("resetRepo: done after exception");
        }
    }

    
}
