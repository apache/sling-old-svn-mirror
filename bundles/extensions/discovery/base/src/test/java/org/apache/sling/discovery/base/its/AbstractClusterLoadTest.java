/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.discovery.base.its;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.sling.commons.testing.junit.categories.Slow;
import org.apache.sling.discovery.base.commons.UndefinedClusterViewException;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.discovery.base.its.setup.WithholdingAppender;
import org.apache.sling.testing.tools.retry.RetryLoop;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrated from org.apache.sling.discovery.impl.cluster.ClusterLoadTest
 */
public abstract class AbstractClusterLoadTest {

    // wait up to 120 sec - in 1sec wait-intervals
    private static final int INSTANCE_VIEW_TIMEOUT_SECONDS = 120;
    private static final int INSTANCE_VIEW_POLL_INTERVAL_MILLIS = 500;

    private final Random random = new Random();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    List<VirtualInstance> instances = new LinkedList<VirtualInstance>();

    @After
    public void tearDown() throws Exception {
    	if (instances==null || instances.size()==0) {
    		return;
    	}
    	for (Iterator<VirtualInstance> it = instances.iterator(); it.hasNext();) {
    	    VirtualInstance i = it.next();
			i.stop();
			it.remove();
		}
    }
    
    public abstract VirtualInstanceBuilder newBuilder();
    

    @Test
    public void testFramework() throws Exception {
        logger.info("testFramework: building 1st instance..");
        VirtualInstanceBuilder builder = newBuilder()
                .newRepository("/var/discovery/impl/ClusterLoadTest/testFramework/", true)
                .setDebugName("firstInstance")
                .setConnectorPingTimeout(3)
                .setConnectorPingInterval(20)
                .setMinEventDelay(0);
        VirtualInstance firstInstance = builder.build();
		instances.add(firstInstance);
    	Thread.sleep(2000);
    	// without any heartbeat action, the discovery service reports its local instance
    	// in so called 'isolated' mode - lets test for that
    	try{
    	    firstInstance.getClusterViewService().getLocalClusterView();
    	    fail("should complain");
    	} catch(UndefinedClusterViewException e) {
    	    // SLING-5030:
    	}
        firstInstance.startViewChecker(1);
        Thread.sleep(4000);
        // after a heartbeat and letting it settle, the discovery service must have
        // established a view - test for that
        firstInstance.dumpRepo();
        firstInstance.assertEstablishedView();

        VirtualInstanceBuilder builder2 = newBuilder()
                .useRepositoryOf(builder)
                .setDebugName("secondInstance")
                .setConnectorPingTimeout(3)
                .setConnectorPingInterval(20)
                .setMinEventDelay(0);
        firstInstance.dumpRepo();
        logger.info("testFramework: building 2nd instance..");
        VirtualInstance secondInstance = builder2.build();
        instances.add(secondInstance);
        secondInstance.startViewChecker(1);
        Thread.sleep(4000);
        firstInstance.dumpRepo();
        assertEquals(firstInstance.getClusterViewService().getLocalClusterView().getInstances().size(), 2);
        assertEquals(secondInstance.getClusterViewService().getLocalClusterView().getInstances().size(), 2);
    }

    @Test
    public void testTwoInstancesFast() throws Throwable {
    	doTest(2, 3);
    }
    
    @Test
    public void testThreeInstancesFast() throws Throwable {
    	doTest(3, 3);
    }

    @Category(Slow.class)
    @Test
    public void testTwoInstances() throws Throwable {
    	doTest(2, 5);
    }

    @Category(Slow.class)
    @Test
    public void testThreeInstances() throws Throwable {
    	doTest(3, 6);
    }

    @Category(Slow.class)
    @Test
    public void testFourInstances() throws Throwable {
    	doTest(4, 7);
    }

    @Category(Slow.class)
    @Test
    public void testFiveInstances() throws Throwable {
    	doTest(5, 8);
    }

    @Category(Slow.class)
    @Test
    public void testSixInstances() throws Throwable {
    	doTest(6, 9);
    }

    @Category(Slow.class)
    @Test
    public void testSevenInstances() throws Throwable {
        doTest(7, 10);
    }
    
    @Category(Slow.class)
    @Test
    public void testEightInstances() throws Throwable {
        doTest(8, 50);
    }

    private void doTest(final int size, final int loopCnt) throws Throwable {
        WithholdingAppender withholdingAppender = null;
        boolean failure = true;
        try{
            logger.info("doTest("+size+","+loopCnt+"): muting log output...");
            withholdingAppender = WithholdingAppender.install();
            doDoTest(size, loopCnt);
            failure = false;
        } finally {
            if (withholdingAppender!=null) {
                if (failure) {
                    logger.info("doTest("+size+","+loopCnt+"): writing muted log output due to failure...");
                }
                withholdingAppender.release(failure);
                if (!failure) {
                    logger.info("doTest("+size+","+loopCnt+"): not writing muted log output due to success...");
                }
            }
            logger.info("doTest("+size+","+loopCnt+"): unmuted log output.");
        }
    }
    
	private void doDoTest(final int size, final int loopCnt) throws Throwable {
		if (size<2) {
			fail("can only test 2 or more instances");
		}
        VirtualInstanceBuilder builder = newBuilder()
                .newRepository("/var/discovery/impl/ClusterLoadTest/doTest-"+size+"-"+loopCnt+"/", true)
                .setDebugName("firstInstance-"+size+"_"+loopCnt)
                .setConnectorPingTimeout(3)
                .setConnectorPingInterval(20)
                .setMinEventDelay(0);
		VirtualInstance firstInstance = builder.build();
		firstInstance.startViewChecker(1);
		instances.add(firstInstance);
		for(int i=1; i<size; i++) {
		    VirtualInstanceBuilder builder2 = newBuilder()
		            .useRepositoryOf(builder)
		            .setDebugName("subsequentInstance-"+i+"-"+size+"_"+loopCnt)
	                .setConnectorPingTimeout(3)
	                .setMinEventDelay(0)
	                .setConnectorPingInterval(20);
			VirtualInstance subsequentInstance = builder2.build();
			instances.add(subsequentInstance);
			subsequentInstance.startViewChecker(1);
		}

		for(int i=0; i<loopCnt; i++) {
			logger.info("=====================");
			logger.info(" START of LOOP "+i);
			logger.info("=====================");

			// count how many instances had heartbeats running in the first place
			int aliveCnt = 0;
			for (Iterator<VirtualInstance> it = instances.iterator(); it.hasNext();) {
			    VirtualInstance instance = it.next();
				if (instance.isViewCheckerRunning()) {
					aliveCnt++;
				}
			}
			logger.info("=====================");
			logger.info(" original aliveCnt "+aliveCnt);
			logger.info("=====================");
			if (aliveCnt==0) {
				// if no one is sending heartbeats, all instances go back to isolated mode
				aliveCnt=1;
			}

            final int aliveCntFinal = aliveCnt;

			for (Iterator<VirtualInstance> it = instances.iterator(); it.hasNext();) {
			    VirtualInstance instance = it.next();
				try {
                    instance.dumpRepo();
                } catch (Exception e) {
                    logger.error("Failed dumping repo for instance " + instance.getSlingId(), e);
                }
			}

			// then verify that each instance sees that many instances
			for (Iterator<VirtualInstance> it = instances.iterator(); it.hasNext();) {
                final VirtualInstance instance = it.next();
				if (!instance.isViewCheckerRunning()) {
					// if the heartbeat is not running, this instance is considered dead
					// hence we're not doing any assert here (as the count is only
					// valid if heartbeat/checkView is running and that would void the test)
				} else {
                    new RetryLoop(new ConditionImplementation(instance, aliveCntFinal), INSTANCE_VIEW_TIMEOUT_SECONDS,
                            INSTANCE_VIEW_POLL_INTERVAL_MILLIS);
				}
			}

			// start/stop heartbeats accordingly
			logger.info("Starting/Stopping heartbeats with count="+instances.size());
			for (Iterator<VirtualInstance> it = instances.iterator(); it.hasNext();) {
			    VirtualInstance instance = it.next();
				if (random.nextBoolean()) {
					logger.info("Starting heartbeats with "+instance.slingId);
					instance.startViewChecker(1);
					logger.info("Started heartbeats with "+instance.slingId);
				} else {
					logger.info("Stopping heartbeats with "+instance.slingId);
					instance.stopViewChecker();
					logger.info("Stopped heartbeats with "+instance.slingId);
				}
			}

		}
	}

    class ConditionImplementation implements RetryLoop.Condition {

        private final int expectedAliveCount;
        private final VirtualInstance instance;

        private ConditionImplementation(VirtualInstance instance, int expectedAliveCount) {
            this.expectedAliveCount = expectedAliveCount;
            this.instance = instance;
        }

        public boolean isTrue() throws Exception {
            boolean result = false;
            int actualAliveCount = -1;
            try{
                actualAliveCount = instance.getClusterViewService().getLocalClusterView().getInstances().size();
                result = expectedAliveCount == actualAliveCount;
            } catch(UndefinedClusterViewException e) {
                logger.info("no view at the moment: "+e);
                return false;
            } catch(Exception e) {
                logger.error("isTrue: got exception: "+e, e);
                throw e;
            }
            if (!result) {
                logger.info("isTrue: expected="+expectedAliveCount+", actual="+actualAliveCount+", result="+result);
            }
            return result;
        }

        public String getDescription() {
            return "Waiting for instance with " + instance.getSlingId() + " to see " + expectedAliveCount
                    + " instances";
        }
    }

}
