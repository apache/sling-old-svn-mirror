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
package org.apache.sling.discovery.impl.cluster.voting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.commons.testing.junit.categories.Slow;
import org.apache.sling.commons.threads.ModifiableThreadPoolConfig;
import org.apache.sling.commons.threads.impl.DefaultThreadPool;
import org.apache.sling.discovery.base.its.setup.OSGiMock;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceHelper;
import org.apache.sling.discovery.base.its.setup.mock.DummyResourceResolverFactory;
import org.apache.sling.discovery.commons.providers.spi.base.DummySlingSettingsService;
import org.apache.sling.discovery.impl.cluster.voting.VotingHandler.VotingDetail;
import org.apache.sling.discovery.impl.common.heartbeat.HeartbeatHandler;
import org.apache.sling.discovery.impl.common.heartbeat.HeartbeatHelper;
import org.apache.sling.discovery.impl.setup.TestConfig;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junitx.util.PrivateAccessor;

public class VotingHandlerTest {


    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    VotingHandler votingHandler1;
    VotingHandler votingHandler2;
    VotingHandler votingHandler3;
    VotingHandler votingHandler4;
    VotingHandler votingHandler5;
    
    String slingId1;
    String slingId2;
    String slingId3;
    String slingId4;
    String slingId5;

    ResourceResolverFactory factory;
    ResourceResolver resourceResolver;
    TestConfig config;

    DefaultThreadPool threadPool;
    
    private void resetRepo() throws Exception {
        Session l = RepositoryProvider.instance().getRepository()
                .loginAdministrative(null);
        try{
            l.getNode("/var");
            l.removeItem("/var");
        } catch(PathNotFoundException pnfe) {
            // well then we probably dont have to do any cleanup
        }
        l.save();
        l.logout();
    }
    
    @Before
    public void setUp() throws Exception {
        slingId1 = UUID.randomUUID().toString();
        slingId2 = UUID.randomUUID().toString();
        slingId3 = UUID.randomUUID().toString();
        slingId4 = UUID.randomUUID().toString();
        slingId5 = UUID.randomUUID().toString();
        
        factory = new DummyResourceResolverFactory();
        resetRepo();
        config = new TestConfig("/var/discovery/impltesting/");
        config.setHeartbeatInterval(999);
        config.setHeartbeatTimeout(60);

        votingHandler1 = VotingHandler.testConstructor(new DummySlingSettingsService(slingId1), factory, config);
        votingHandler2 = VotingHandler.testConstructor(new DummySlingSettingsService(slingId2), factory, config);
        votingHandler3 = VotingHandler.testConstructor(new DummySlingSettingsService(slingId3), factory, config);
        votingHandler4 = VotingHandler.testConstructor(new DummySlingSettingsService(slingId4), factory, config);
        votingHandler5 = VotingHandler.testConstructor(new DummySlingSettingsService(slingId5), factory, config);
        
        resourceResolver = factory.getAdministrativeResourceResolver(null);
        
        ModifiableThreadPoolConfig tpConfig = new ModifiableThreadPoolConfig();
        tpConfig.setMinPoolSize(80);
        tpConfig.setMaxPoolSize(80);
        threadPool = new DefaultThreadPool("testing", tpConfig);
    }
    
    @After
    public void tearDown() throws Exception {
        if (resourceResolver != null) {
            resourceResolver.close();
        }
        
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }
    
    @Test
    public void testActivateDeactivate() throws Exception {
        assertFalse((Boolean)PrivateAccessor.getField(votingHandler1, "activated"));
        votingHandler1.activate(null);
        assertTrue((Boolean)PrivateAccessor.getField(votingHandler1, "activated"));
        votingHandler1.deactivate();
        assertFalse((Boolean)PrivateAccessor.getField(votingHandler1, "activated"));
    }
    
    @Test
    public void testNoVotings() throws Exception {
        votingHandler1.analyzeVotings(resourceResolver);
    }
    
    private VotingView newVoting2(String newViewId, String initiatorId, String... liveInstances) throws Exception {
        return VotingView.newVoting(resourceResolver, config, newViewId, initiatorId, new HashSet<String>(Arrays.asList(liveInstances)));
    }
    
    private VotingView newVoting(String initiatorId, String... liveInstances) throws Exception {
        return newVoting2(UUID.randomUUID().toString(), initiatorId, liveInstances);
    }
    
    @Test
    public void testPromotion() throws Exception {
        VotingView voting = newVoting(slingId1, slingId1);
        assertNotNull(voting);
        VirtualInstanceHelper.dumpRepo(factory);
        votingHandler1.activate(null);
        Map<VotingView, VotingDetail> result = votingHandler1.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.PROMOTED, result.values().iterator().next());
        result = votingHandler1.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(0, result.size());
    }
    
    @Test
    public void testVotingYesTwoNodes() throws Exception {
        VotingView voting = newVoting(slingId2, slingId1, slingId2);
        assertNotNull(voting);
        heartbeat(slingId1);
        heartbeat(slingId2);
        VirtualInstanceHelper.dumpRepo(factory);
        votingHandler1.activate(null);
        Map<VotingView, VotingDetail> result = votingHandler1.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.VOTED_YES, result.values().iterator().next());
        result = votingHandler1.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.WINNING, result.values().iterator().next());
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.WINNING, result.values().iterator().next());
    }

    @Test
    public void testVotingYesThreeNodes() throws Exception {
        VotingView voting = newVoting(slingId2, slingId1, slingId2, slingId3);
        assertNotNull(voting);
        heartbeat(slingId1);
        heartbeat(slingId2);
        heartbeat(slingId3);
        VirtualInstanceHelper.dumpRepo(factory);
        votingHandler1.activate(null);
        Map<VotingView, VotingDetail> result = votingHandler1.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.VOTED_YES, result.values().iterator().next());
        result = votingHandler1.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.UNCHANGED, result.values().iterator().next());
    }

    @Test
    public void testVotingYesFiveNodes() throws Exception {
        VotingView voting = newVoting(slingId2, slingId1, slingId2, slingId3, slingId4, slingId5);
        assertNotNull(voting);
        heartbeat(slingId1);
        heartbeat(slingId2);
        heartbeat(slingId3);
        heartbeat(slingId4);
        heartbeat(slingId5);
        VirtualInstanceHelper.dumpRepo(factory);
        votingHandler1.activate(null);
        Map<VotingView, VotingDetail> result = votingHandler1.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.VOTED_YES, result.values().iterator().next());
        result = votingHandler1.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.UNCHANGED, result.values().iterator().next());
        votingHandler3.activate(null);
        result = votingHandler3.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.VOTED_YES, result.values().iterator().next());
        result = votingHandler3.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.UNCHANGED, result.values().iterator().next());
        votingHandler4.activate(null);
        result = votingHandler4.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.VOTED_YES, result.values().iterator().next());
        result = votingHandler4.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.UNCHANGED, result.values().iterator().next());
        votingHandler5.activate(null);
        result = votingHandler5.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.VOTED_YES, result.values().iterator().next());
        result = votingHandler5.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.WINNING, result.values().iterator().next());
        votingHandler2.activate(null);
        result = votingHandler2.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.PROMOTED, result.values().iterator().next());
    }

    private void heartbeat(String slingId) throws Exception {
        HeartbeatHandler hh = HeartbeatHandler.testConstructor(new DummySlingSettingsService(slingId), factory, null, null, config, null, votingHandler1);
        OSGiMock.activate(hh);
        HeartbeatHelper.issueClusterLocalHeartbeat(hh);
    }
    
    @Test
    public void testTimedout() throws Exception {
        config.setHeartbeatTimeout(1);
        VotingView voting = newVoting(slingId1, slingId1);
        assertNotNull(voting);
        Thread.sleep(1200);
        votingHandler1.activate(null);
        Map<VotingView, VotingDetail> result = votingHandler1.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VotingDetail.TIMEDOUT, result.values().iterator().next());
        result = votingHandler1.analyzeVotings(resourceResolver);
        assertNotNull(result);
        assertEquals(0, result.size());
    }
    
    private void asyncVote(final String debugInfo, final VotingHandler votingHandler, final List<VotingDetail> votingDetails, final Semaphore ready, final Semaphore go, final Semaphore done, final Set<Throwable> exceptions) throws Exception {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                boolean released = false;
                try{
                    logger.info("asyncVote["+debugInfo+"] logging in...");
                    Map<VotingView, VotingDetail> result = null;
                    ResourceResolver rr = null;
                    int retries = 0;
                    while(true) {
                        try{
                            rr = factory.getAdministrativeResourceResolver(null);
                            if (retries == 0) {
                                logger.info("asyncVote["+debugInfo+"] marking ready...");
                                ready.release();
                                logger.info("asyncVote["+debugInfo+"] waiting for go...");
                                go.acquire();
                            } else {
                                logger.info("asyncVote["+debugInfo+"] not doing ready/go on retry.");
                            }
                            logger.info("asyncVote["+debugInfo+"] analyzeVotings...");
                            result = votingHandler.analyzeVotings(rr);
                            break;
                        } catch(Exception e) {
                            logger.warn("asyncVote["+debugInfo+"] Exception: "+e, e);
                            if (retries++<5) {
                                Thread.sleep(500);
                                logger.info("asyncVote["+debugInfo+"] retrying after Exception...");
                                continue;
                            }
                            throw e;
                        } finally {
                            if (rr != null) {
                                rr.close();
                            }
                        }
                    }
                    logger.info("asyncVote["+debugInfo+"] done, asserting results...");
                    assertNotNull(result);
                    votingDetails.addAll(result.values());
                    logger.info("asyncVote["+debugInfo+"] marking done.");
                    done.release();
                    released = true;
                } catch(RuntimeException re) {
                    // SLING-5244: make sure we're not silently running into an unchecked exception
                    logger.info("asyncVote["+debugInfo+"] RuntimeException: "+re, re);
                    exceptions.add(re);
                    throw re;
                } catch(Error er) {
                    // SLING-5244: make sure we're not silently running into an unchecked exception
                    logger.info("asyncVote["+debugInfo+"] Error: "+er, er);
                    exceptions.add(er);
                    throw er;
                } catch(Exception e) {
                    // SLING-5244: make sure we're not silently running into an unchecked exception
                    logger.info("asyncVote["+debugInfo+"] Exception: "+e, e);
                    exceptions.add(e);
                } finally {
                    // SLING-5244: make sure we're getting informed when this thread is done - be it normal or not
                    logger.info("asyncVote["+debugInfo+"] finally [released="+released+"]");
                }
            }
            
        };
        threadPool.execute(r);
    }
    
    @Test
    public void testConcurrentVotesSingleNode() throws Exception {
        doTestConcurrentVotes(123, 12, votingHandler1);
    }
    
    @Category(Slow.class) //TODO: takes env 15sec
    @Test
    public void testConcurrentVotesTwoNodes() throws Exception {
        doTestConcurrentVotes(456, 12, votingHandler1, votingHandler2);
    }
    
    @Test
    public void testFastConcurrentVotesTwoNodes() throws Exception {
        doTestConcurrentVotes(42, 12, votingHandler1, votingHandler2);
    }

    @Category(Slow.class) //TODO: takes env 10sec
    @Test
    public void testConcurrentVotesThreeNodes() throws Exception {
        doTestConcurrentVotes(234, 12, votingHandler1, votingHandler2, votingHandler3);
    }
    
    @Category(Slow.class) //TODO: takes env 30sec
    @Test
    public void testConcurrentVotesFourNodes() throws Exception {
        doTestConcurrentVotes(247, 12, votingHandler1, votingHandler2, votingHandler3, votingHandler4);
    }
    
    @Category(Slow.class) //TODO: takes env 25sec
    @Test
    public void testConcurrentVotesFiveNodes() throws Exception {
        doTestConcurrentVotes(285, 12, votingHandler1, votingHandler2, votingHandler3, votingHandler4, votingHandler5);
    }
    
    @Test
    public void testFastConcurrentVotesFiveNodes() throws Exception {
        doTestConcurrentVotes(12, 12, votingHandler1, votingHandler2, votingHandler3, votingHandler4, votingHandler5);
    }

    private void add(List<VotingDetail> votingDetails, Map<VotingDetail, Integer> totals) {
        for (VotingDetail d : votingDetails) {
            Integer i = totals.get(d);
            if (i==null) {
                i = 0;
            }
            i++;
            totals.put(d, i);
        }
    }

    private int count(List<VotingDetail> votingDetails, VotingDetail detail) {
        int result = 0;
        for (VotingDetail d : votingDetails) {
            if (d.equals(detail)) {
                result++;
            }
        }
        return result;
    }

    public void doTestConcurrentVotes(int votingsLoopCnt, int perVotingInnerLoopCnt, VotingHandler... votingHandler) throws Exception {
        config.setHeartbeatInterval(999);
        config.setHeartbeatTimeout(120);
        
        for (VotingHandler handler : votingHandler) {
            handler.activate(null);
        }
        
        int[] totals = new int[votingHandler.length];
        
        List<Map<VotingDetail,Integer>> totalDetails = new LinkedList<Map<VotingDetail,Integer>>();
        for(int i=0; i<votingHandler.length; i++) {
            HashMap<VotingDetail, Integer> d = new HashMap<VotingHandler.VotingDetail, Integer>();
            totalDetails.add(d);
        }
        
        String[] slingIds = new String[votingHandler.length];
        for(int k=0; k<votingHandler.length; k++) {
            slingIds[k] = (String) PrivateAccessor.getField(votingHandler[k], "slingId");
        }
        
        for(int i=0; i<votingsLoopCnt; i++) { // large voting loop
            logger.info("testConcurrentVotes: loop i="+i+", votingHandler.cnt="+votingHandler.length);

            for(int k=0; k<votingHandler.length; k++) {
                heartbeat(slingIds[k]);
            }
            for(int k=0; k<votingHandler.length; k++) {
                String initiatorId = (String) PrivateAccessor.getField(votingHandler[k], "slingId");
                VotingView voting = newVoting(initiatorId, slingIds);
                assertNotNull(voting);
            }
            Semaphore ready = new Semaphore(0);
            Semaphore go = new Semaphore(0);
            Semaphore done = new Semaphore(0);
            Set<Throwable> e = new ConcurrentHashSet<Throwable>();
            boolean success = false;
            
            List<List<VotingDetail>> detailList = new LinkedList<List<VotingDetail>>();
            for(int k=0; k<votingHandler.length; k++) {
                detailList.add(new LinkedList<VotingHandler.VotingDetail>());
            }
            
            for(int j=0; j<perVotingInnerLoopCnt; j++) {
                logger.info("testConcurrentVotes: loop i="+i+", votingHandler.cnt="+votingHandler.length+", j="+j);
                for(int k=0; k<votingHandler.length; k++) {
                    logger.info("testConcurrentVotes: <heartbeat for slingId,k="+k+"> loop i="+i+", votingHandler.cnt="+votingHandler.length+", j="+j);
                    heartbeat(slingIds[k]);
                    logger.info("testConcurrentVotes: <asyncVote for slingId,k="+k+"> loop i="+i+", votingHandler.cnt="+votingHandler.length+", j="+j);
                    asyncVote("k="+k, votingHandler[k], detailList.get(k), ready, go, done, e);
                }
                assertTrue("threads were not ready within 30sec", ready.tryAcquire(votingHandler.length, 30, TimeUnit.SECONDS));

                // both are now ready, so lets go
                logger.info("testConcurrentVotes: GO loop i="+i+", votingHandler.cnt="+votingHandler.length+", j="+j);
                go.release(votingHandler.length);
                assertTrue("threads were not done within 120sec", done.tryAcquire(votingHandler.length, 120, TimeUnit.SECONDS));
                if (e.size()!=0) {
                    fail("Got exceptions: "+e.size()+", first: "+e.iterator().next());
                }
                
                int promotionTotalCount = 0;
                int noTotalCount = 0;
                for(int k=0; k<votingHandler.length; k++) {
                    int promotedCnt = count(detailList.get(k), VotingDetail.PROMOTED);
                    int noCnt = count(detailList.get(k), VotingDetail.VOTED_NO);
                    totals[k] += promotedCnt;
                    promotionTotalCount += promotedCnt;
                    noTotalCount += noCnt;
                }
                if (promotionTotalCount==0) {
                    continue; // should have 1 promotionTotalCount, if not, repeat
                } else if (promotionTotalCount>1) {
                    fail("more than 1 promoted views: "+promotionTotalCount);
                } else if (noTotalCount<votingHandler.length-1) {
                    continue; // should have votingHandler.length-1 no votes, if not, repeat
                } else {
                    // done
                    success = true;
                    break;
                }
            }
            assertTrue("did not promote within "+perVotingInnerLoopCnt+" loops", success);
            for(int k=0; k<votingHandler.length; k++) {
                add(detailList.get(k), totalDetails.get(k));
            }

        }
        StringBuffer sb = new StringBuffer();
        for(int k=0; k<votingHandler.length; k++) {
            sb.append(" - by slingId");
            sb.append(k+1);
            sb.append(": ");
            sb.append(totals[k]);
        }
        
        logger.info("testConcurrentVotes: promoted "+sb);
        int totalPromotion = 0;
        for(int k=0; k<votingHandler.length; k++) {
            for (Map.Entry<VotingDetail, Integer> anEntry : totalDetails.get(k).entrySet()) {
                logger.info("testConcurrentVotes: slingId"+(k+1)+", detail="+anEntry.getKey()+", value="+anEntry.getValue());
            }
            // SLING-5244 : cannot assume that we have '(votingHandler.length-1) * votingsLoopCnt'
            // because: it can happen that a voting concludes within one j-loop above:
            // that is the case when the instance that does not initiate the vote comes first, then
            // the initiator - in that case the initiator finds an already completed vote - and it
            // will then not do any no-votes ..
            // so .. this check is a) not possible and b) just also not necessary, cos 
            // we already make sure that we at least get 'votingHandler.length-1' no votes in the j-loop
            // and that is precise enough. so as unfortuante as it is, we can't make below assertion..
            // unless we do more white-box-assertions into analyzeVotings, which is probably not helping
            // test-stability either..
//            Integer noVotes = totalDetails.get(k).get(VotingDetail.VOTED_NO);
//            int expected = (votingHandler.length-1) * votingsLoopCnt;
//            if (expected>0) {
//                assertEquals(expected, (int)noVotes);
//            }
            final Map<VotingDetail, Integer> map = totalDetails.get(k);
            final Integer i = map.get(VotingDetail.PROMOTED);
            if (i != null) {
                totalPromotion += i;
            }
        }
        assertEquals((int)votingsLoopCnt, totalPromotion);
    }

}
