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
package org.apache.sling.discovery.commons.providers.spi.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.commons.providers.DummyTopologyView;
import org.apache.sling.discovery.commons.providers.ViewStateManager;
import org.apache.sling.discovery.commons.providers.base.DummyListener;
import org.apache.sling.discovery.commons.providers.base.TestHelper;
import org.apache.sling.discovery.commons.providers.base.ViewStateManagerFactory;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteConfig;
import org.apache.sling.discovery.commons.providers.spi.base.IdMapService;
import org.apache.sling.discovery.commons.providers.spi.base.OakBacklogConsistencyService;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestOakSyncTokenConsistencyService {

    private static final String SYNCTOKEN_PATH = "/var/discovery/commons/synctokens";

    private static final String IDMAP_PATH = "/var/discovery/commons/idmap";

    public final class SimpleCommonsConfig implements DiscoveryLiteConfig {
        
        private long bgIntervalMillis;
        private long bgTimeoutMillis;

        SimpleCommonsConfig() {
            this(1000, -1); // defaults
        }

        SimpleCommonsConfig(long bgIntervalMillis, long bgTimeoutMillis) {
            this.bgIntervalMillis = bgIntervalMillis;
            this.bgTimeoutMillis = bgTimeoutMillis;
        }
        
        @Override
        public String getSyncTokenPath() {
            return SYNCTOKEN_PATH;
        }

        @Override
        public String getIdMapPath() {
            return IDMAP_PATH;
        }

        @Override
        public long getBgTimeoutMillis() {
            return bgTimeoutMillis;
        }

        @Override
        public long getBgIntervalMillis() {
            return bgIntervalMillis;
        }

    }

    ResourceResolverFactory factory1;
    ResourceResolverFactory factory2;
    private SlingRepository repository1;
    private SlingRepository repository2;
    private MemoryNodeStore memoryNS;
    private IdMapService idMapService1;
    private String slingId1;
    
    @Before
    public void setup() throws Exception {
        RepositoryTestHelper.resetRepo();
        memoryNS = new MemoryNodeStore();
        repository1 = RepositoryTestHelper.newOakRepository(memoryNS);
        RepositoryTestHelper.initSlingNodeTypes(repository1);
        repository2 = RepositoryTestHelper.newOakRepository(memoryNS);
        factory1 = RepositoryTestHelper.mockResourceResolverFactory(repository1);
        factory2 = RepositoryTestHelper.mockResourceResolverFactory(repository2);
        slingId1 = UUID.randomUUID().toString();
        idMapService1 = IdMapService.testConstructor(new SimpleCommonsConfig(), new DummySlingSettingsService(slingId1), factory1);
    }
    
    @After
    public void tearDown() throws Exception {
        if (repository1!=null) {
            RepositoryTestHelper.stopRepository(repository1);
            repository1 = null;
        }
        if (repository2!=null) {
            RepositoryTestHelper.stopRepository(repository2);
            repository2 = null;
        }
    }
    
    @Test
    public void testOneNode() throws Exception {
        DummyTopologyView one = TestHelper.newView(true, slingId1, slingId1, slingId1);
        Lock lock = new ReentrantLock();
        OakBacklogConsistencyService cs = OakBacklogConsistencyService.testConstructorAndActivate(new SimpleCommonsConfig(), idMapService1, new DummySlingSettingsService(slingId1), factory1);
        ViewStateManager vsm = ViewStateManagerFactory.newViewStateManager(lock, cs);
        DummyListener l = new DummyListener();
        assertEquals(0, l.countEvents());
        vsm.bind(l);
        cs.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        vsm.handleActivated();
        cs.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        vsm.handleNewView(one);
        cs.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        cs.triggerBackgroundCheck();
        DescriptorHelper.setDiscoveryLiteDescriptor(factory1, new DiscoveryLiteDescriptorBuilder().me(1).seq(1).activeIds(1).setFinal(true));
        assertTrue(idMapService1.waitForInit(2000));
        cs.triggerBackgroundCheck();
        assertTrue(vsm.waitForAsyncEvents(1000));
        assertEquals(1, l.countEvents());
    }
    
    @Test
    public void testTwoNodesOneLeaving() throws Exception {
        String slingId2 = UUID.randomUUID().toString();
        DummyTopologyView two1 = TestHelper.newView(true, slingId1, slingId1, slingId1, slingId2);
        Lock lock1 = new ReentrantLock();
        OakBacklogConsistencyService cs1 = OakBacklogConsistencyService.testConstructorAndActivate(new SimpleCommonsConfig(), idMapService1, new DummySlingSettingsService(slingId1), factory1);
        ViewStateManager vsm1 = ViewStateManagerFactory.newViewStateManager(lock1, cs1);
        DummyListener l = new DummyListener();
        vsm1.bind(l);
        vsm1.handleActivated();
        vsm1.handleNewView(two1);
        cs1.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        DescriptorHelper.setDiscoveryLiteDescriptor(factory1, new DiscoveryLiteDescriptorBuilder().setFinal(true).me(1).seq(1).activeIds(1).deactivatingIds(2));
        cs1.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        DescriptorHelper.setDiscoveryLiteDescriptor(factory1, new DiscoveryLiteDescriptorBuilder().setFinal(true).me(1).seq(2).activeIds(1));
        cs1.triggerBackgroundCheck();
        Lock lock2 = new ReentrantLock();
        IdMapService idMapService2 = IdMapService.testConstructor(
                new SimpleCommonsConfig(), new DummySlingSettingsService(slingId2), factory2);
        OakBacklogConsistencyService cs2 = OakBacklogConsistencyService.testConstructorAndActivate(new SimpleCommonsConfig(), idMapService2, new DummySlingSettingsService(slingId2), factory2);
        ViewStateManager vsm2 = ViewStateManagerFactory.newViewStateManager(lock2, cs2);
        cs1.triggerBackgroundCheck();
        cs2.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        DescriptorHelper.setDiscoveryLiteDescriptor(factory2, new DiscoveryLiteDescriptorBuilder().setFinal(true).me(2).seq(3).activeIds(1, 2));
        cs1.triggerBackgroundCheck();
        cs2.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        DescriptorHelper.setDiscoveryLiteDescriptor(factory1, new DiscoveryLiteDescriptorBuilder().setFinal(true).me(1).seq(3).activeIds(1, 2));
        cs1.triggerBackgroundCheck();
        cs2.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        vsm2.handleActivated();
        assertTrue(idMapService1.waitForInit(2000));
        assertTrue(idMapService2.waitForInit(2000));
        DummyTopologyView two2 = TestHelper.newView(two1.getLocalClusterSyncTokenId(), two1.getLocalInstance().getClusterView().getId(), true, slingId1, slingId1, slingId1, slingId2);
        vsm2.handleNewView(two2);
        cs1.triggerBackgroundCheck();
        cs2.triggerBackgroundCheck();
        assertEquals(1, l.countEvents());
        DummyTopologyView oneLeaving = two1.clone();
        oneLeaving.removeInstance(slingId2);
        DescriptorHelper.setDiscoveryLiteDescriptor(factory1, new DiscoveryLiteDescriptorBuilder().setFinal(true).me(1).seq(1).activeIds(1).deactivatingIds(2));
        vsm1.handleNewView(oneLeaving);
        cs1.triggerBackgroundCheck();
        cs2.triggerBackgroundCheck();
        assertEquals(2, l.countEvents());
        DescriptorHelper.setDiscoveryLiteDescriptor(factory1, new DiscoveryLiteDescriptorBuilder().setFinal(true).me(1).seq(2).activeIds(1).inactiveIds(2));
        cs1.triggerBackgroundCheck();
        cs2.triggerBackgroundCheck();
        RepositoryTestHelper.dumpRepo(factory1);
        assertEquals(3, l.countEvents());
    }
    
}
