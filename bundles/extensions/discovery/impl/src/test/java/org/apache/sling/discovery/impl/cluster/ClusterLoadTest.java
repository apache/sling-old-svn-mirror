package org.apache.sling.discovery.impl.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.sling.discovery.impl.common.resource.EstablishedInstanceDescription;
import org.apache.sling.discovery.impl.common.resource.IsolatedInstanceDescription;
import org.apache.sling.discovery.impl.setup.Instance;
import org.apache.sling.discovery.impl.setup.WithholdingAppender;
import org.apache.sling.testing.tools.retry.RetryLoop;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ClusterLoadTest {

    // wait up to 4 heartbeat intervals
    private static final int INSTANCE_VIEW_WAIT_TIME_MILLIS = 5000;
    private static final int INSTANCE_VIEW_POLL_INTERVAL_MILLIS = 500;

    private final Random random = new Random();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    List<Instance> instances = new LinkedList<Instance>();

    @After
    public void tearDown() throws Exception {
    	if (instances==null || instances.size()==0) {
    		return;
    	}
    	for (Iterator<Instance> it = instances.iterator(); it.hasNext();) {
			Instance i = it.next();
			i.stop();
			it.remove();
		}
    }

    @Test
    public void testFramework() throws Exception {
		Instance firstInstance = Instance.newStandaloneInstance("/var/discovery/impl/ClusterLoadTest/testFramework/", "firstInstance", false, 2, 0);
		instances.add(firstInstance);
    	Thread.sleep(2000);
    	// without any heartbeat action, the discovery service reports its local instance
    	// in so called 'isolated' mode - lets test for that
        assertEquals(IsolatedInstanceDescription.class, firstInstance
                .getClusterViewService().getClusterView().getInstances().get(0)
                .getClass());
        firstInstance.startHeartbeats(1);
        Thread.sleep(4000);
        // after a heartbeat and letting it settle, the discovery service must have
        // established a view - test for that
        assertEquals(EstablishedInstanceDescription.class, firstInstance
                .getClusterViewService().getClusterView().getInstances().get(0)
                .getClass());

        Instance secondInstance = Instance.newClusterInstance("/var/discovery/impl/ClusterLoadTest/testFramework/", "secondInstance", firstInstance, false, 2, 0);
        instances.add(secondInstance);
        secondInstance.startHeartbeats(1);
        Thread.sleep(4000);
        assertEquals(firstInstance.getClusterViewService().getClusterView().getInstances().size(), 2);
        assertEquals(secondInstance.getClusterViewService().getClusterView().getInstances().size(), 2);
    }

    @Test
    public void testTwoInstances() throws Throwable {
    	doTest(2, 5);
    }

    @Test
    public void testThreeInstances() throws Throwable {
    	doTest(3, 6);
    }

    @Test
    public void testFourInstances() throws Throwable {
    	doTest(4, 7);
    }

    @Test
    public void testFiveInstances() throws Throwable {
    	doTest(5, 8);
    }

    @Test
    public void testSixInstances() throws Throwable {
    	doTest(6, 9);
    }

    @Test
    public void testSevenInstances() throws Throwable {
    	doTest(7, 10);
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
		Instance firstInstance = Instance.newStandaloneInstance("/var/discovery/impl/ClusterLoadTest/doTest-"+size+"-"+loopCnt+"/", "firstInstance", false, 2, 0);
		firstInstance.startHeartbeats(1);
		instances.add(firstInstance);
		for(int i=1; i<size; i++) {
			Instance subsequentInstance = Instance.newClusterInstance("/var/discovery/impl/ClusterLoadTest/doTest-"+size+"-"+loopCnt+"/", "subsequentInstance-"+i, firstInstance, false, 2, 0);
			instances.add(subsequentInstance);
			subsequentInstance.startHeartbeats(1);
		}

		for(int i=0; i<loopCnt; i++) {
			logger.info("=====================");
			logger.info(" START of LOOP "+i);
			logger.info("=====================");

			// count how many instances had heartbeats running in the first place
			int aliveCnt = 0;
			for (Iterator<Instance> it = instances.iterator(); it.hasNext();) {
				Instance instance = it.next();
				if (instance.isHeartbeatRunning()) {
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

			for (Iterator<Instance> it = instances.iterator(); it.hasNext();) {
				Instance instance = it.next();
				try {
                    instance.dumpRepo();
                } catch (Exception e) {
                    logger.error("Failed dumping repo for instance " + instance.getSlingId(), e);
                }
			}

			// then verify that each instance sees that many instances
			for (Iterator<Instance> it = instances.iterator(); it.hasNext();) {
                final Instance instance = it.next();
				if (!instance.isHeartbeatRunning()) {
					// if the heartbeat is not running, this instance is considered dead
					// hence we're not doing any assert here (as the count is only
					// valid if heartbeat/checkView is running and that would void the test)
				} else {
                    new RetryLoop(new ConditionImplementation(instance, aliveCntFinal), INSTANCE_VIEW_WAIT_TIME_MILLIS,
                            INSTANCE_VIEW_POLL_INTERVAL_MILLIS);
				}
			}

			// start/stop heartbeats accordingly
			logger.info("Starting/Stopping heartbeats with count="+instances.size());
			for (Iterator<Instance> it = instances.iterator(); it.hasNext();) {
				Instance instance = it.next();
				if (random.nextBoolean()) {
					logger.info("Starting heartbeats with "+instance.slingId);
					instance.startHeartbeats(1);
					logger.info("Started heartbeats with "+instance.slingId);
				} else {
					logger.info("Stopping heartbeats with "+instance.slingId);
					instance.stopHeartbeats();
					logger.info("Stopped heartbeats with "+instance.slingId);
				}
			}

		}
	}

    static class ConditionImplementation implements RetryLoop.Condition {

        private final int expectedAliveCount;
        private final Instance instance;

        private ConditionImplementation(Instance instance, int expectedAliveCount) {
            this.expectedAliveCount = expectedAliveCount;
            this.instance = instance;
        }

        public boolean isTrue() throws Exception {
            return expectedAliveCount == instance.getClusterViewService().getClusterView().getInstances().size();
        }

        public String getDescription() {
            return "Waiting for instance with " + instance.getSlingId() + " to see " + expectedAliveCount
                    + " instances";
        }
    }

}
