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
package org.apache.sling.discovery.commons.providers.spi.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.SimpleValueFactory;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.util.GenericDescriptors;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.discovery.commons.providers.ViewStateManager;
import org.apache.sling.discovery.commons.providers.impl.Listener;
import org.apache.sling.discovery.commons.providers.impl.SimpleTopologyView;
import org.apache.sling.discovery.commons.providers.impl.TestHelper;
import org.apache.sling.discovery.commons.providers.impl.ViewStateManagerFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestOakSyncTokenConsistencyService {

    ResourceResolverFactory factory1;
    ResourceResolverFactory factory2;
    private SlingRepository repository1;
    private SlingRepository repository2;
    private MemoryNodeStore memoryNS;
    
    @Before
    public void setup() throws Exception {
        MockFactory.resetRepo();
        memoryNS = new MemoryNodeStore();
        repository1 = RepositoryHelper.newOakRepository(memoryNS);
//        repository1 = MultipleRepositoriesSupport.newRepository("target/repo1");
        RepositoryHelper.initSlingNodeTypes(repository1);
        repository2 = RepositoryHelper.newOakRepository(memoryNS);
//        repository2 = MultipleRepositoriesSupport.newRepository("target/repo2");
//        MultipleRepositoriesSupport.initSlingNodeTypes(repository2);
        factory1 = MockFactory.mockResourceResolverFactory(repository1);
        factory2 = MockFactory.mockResourceResolverFactory(repository2);
    }
    
    @After
    public void tearDown() throws Exception {
        if (repository1!=null) {
            RepositoryHelper.stopRepository(repository1);
            repository1 = null;
        }
        if (repository2!=null) {
            RepositoryHelper.stopRepository(repository2);
            repository2 = null;
        }
    }
    
    @Test
    public void testOneNode() throws Exception {
        String slingId1 = UUID.randomUUID().toString();
        SimpleTopologyView one = TestHelper.newView(true, slingId1, slingId1, slingId1);
        Lock lock = new ReentrantLock();
        OakSyncTokenConsistencyService cs = new OakSyncTokenConsistencyService(factory1, slingId1, -1, -1);
        ViewStateManager vsm = ViewStateManagerFactory.newViewStateManager(lock, cs);
        Listener l = new Listener();
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
        setDiscoveryLiteDescriptor(factory1, new DiscoLite().me(1).seq(1).activeIds(1));
        cs.triggerBackgroundCheck();
        assertEquals(1, l.countEvents());
    }
    
    @Test
    public void testTwoNodesOneLeaving() throws Exception {
        String slingId1 = UUID.randomUUID().toString();
        String slingId2 = UUID.randomUUID().toString();
        SimpleTopologyView two1 = TestHelper.newView(true, slingId1, slingId1, slingId1, slingId2);
        Lock lock1 = new ReentrantLock();
        OakSyncTokenConsistencyService cs1 = new OakSyncTokenConsistencyService(factory1, slingId1, -1, -1);
        ViewStateManager vsm1 = ViewStateManagerFactory.newViewStateManager(lock1, cs1);
        Listener l = new Listener();
        vsm1.bind(l);
        vsm1.handleActivated();
        vsm1.handleNewView(two1);
        cs1.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        setDiscoveryLiteDescriptor(factory1, new DiscoLite().me(1).seq(1).activeIds(1).deactivatingIds(2));
        cs1.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        setDiscoveryLiteDescriptor(factory1, new DiscoLite().me(1).seq(2).activeIds(1));
        cs1.triggerBackgroundCheck();
        Lock lock2 = new ReentrantLock();
        OakSyncTokenConsistencyService cs2 = new OakSyncTokenConsistencyService(factory2, slingId2, -1, -1);
        ViewStateManager vsm2 = ViewStateManagerFactory.newViewStateManager(lock2, cs2);
        cs1.triggerBackgroundCheck();
        cs2.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        setDiscoveryLiteDescriptor(factory2, new DiscoLite().me(2).seq(3).activeIds(1, 2));
        cs1.triggerBackgroundCheck();
        cs2.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        setDiscoveryLiteDescriptor(factory1, new DiscoLite().me(1).seq(3).activeIds(1, 2));
        cs1.triggerBackgroundCheck();
        cs2.triggerBackgroundCheck();
        assertEquals(0, l.countEvents());
        vsm2.handleActivated();
        SimpleTopologyView two2 = TestHelper.newView(two1.getLocalClusterSyncTokenId(), two1.getLocalInstance().getClusterView().getId(), true, slingId1, slingId1, slingId1, slingId2);
        vsm2.handleNewView(two2);
        cs1.triggerBackgroundCheck();
        cs2.triggerBackgroundCheck();
        assertEquals(1, l.countEvents());
        SimpleTopologyView oneLeaving = two1.clone();
        oneLeaving.removeInstance(slingId2);
        setDiscoveryLiteDescriptor(factory1, new DiscoLite().me(1).seq(1).activeIds(1).deactivatingIds(2));
        vsm1.handleNewView(oneLeaving);
        cs1.triggerBackgroundCheck();
        cs2.triggerBackgroundCheck();
        assertEquals(2, l.countEvents());
        setDiscoveryLiteDescriptor(factory1, new DiscoLite().me(1).seq(2).activeIds(1).inactiveIds(2));
        cs1.triggerBackgroundCheck();
        cs2.triggerBackgroundCheck();
        RepositoryHelper.dumpRepo(factory1);
        assertEquals(3, l.countEvents());
    }
    
    private void setDiscoveryLiteDescriptor(ResourceResolverFactory factory, DiscoLite builder) throws JSONException, Exception {
        setDescriptor(factory, OakSyncTokenConsistencyService.OAK_DISCOVERYLITE_CLUSTERVIEW, builder.asJson());
    }
    
    private void setDescriptor(ResourceResolverFactory factory, String key,
            String value) throws Exception {
        ResourceResolver resourceResolver = factory.getAdministrativeResourceResolver(null);
        try{
            Session session = resourceResolver.adaptTo(Session.class);
            if (session == null) {
                return;
            }
            Repository repo = session.getRepository();
            
            //<hack>
//            Method setDescriptorMethod = repo.getClass().
//                    getDeclaredMethod("setDescriptor", String.class, String.class);
//            if (setDescriptorMethod!=null) {
//                setDescriptorMethod.setAccessible(true);
//                setDescriptorMethod.invoke(repo, key, value);
//            } else {
//                fail("could not get 'setDescriptor' method");
//            }
            Method getDescriptorsMethod = repo.getClass().getDeclaredMethod("getDescriptors");
            if (getDescriptorsMethod==null) {
                fail("could not get 'getDescriptors' method");
            } else {
                getDescriptorsMethod.setAccessible(true);
                GenericDescriptors descriptors = (GenericDescriptors) getDescriptorsMethod.invoke(repo);
                SimpleValueFactory valueFactory = new SimpleValueFactory();
                descriptors.put(key, valueFactory.createValue(value), true, true);
            }
            //</hack>
            
            //<verify-hack>
            assertEquals(value, repo.getDescriptor(key));
            //</verify-hack>
        } finally {
            if (resourceResolver!=null) {
                resourceResolver.close();
            }
        }
    }

}
