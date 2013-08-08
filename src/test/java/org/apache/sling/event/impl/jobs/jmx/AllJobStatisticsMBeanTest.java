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
package org.apache.sling.event.impl.jobs.jmx;

import java.util.Date;

import junitx.util.PrivateAccessor;

import org.apache.sling.event.jobs.JobManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AllJobStatisticsMBeanTest {

    private AllJobStatisticsMBean mbean;
    @Mock
    private JobManager jobManager;
    private long seed;

    public AllJobStatisticsMBeanTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void setup() throws NoSuchFieldException {
        mbean = new AllJobStatisticsMBean();
        PrivateAccessor.setField(mbean, "jobManager", jobManager);
        seed = System.currentTimeMillis();
        Mockito.when(jobManager.getStatistics()).thenReturn(
                new DummyStatistics(seed));
    }

    @Test
    public void testStatistics() {
        Assert.assertEquals(seed + 1, mbean.getStartTime());
        Assert.assertEquals(seed + 2, mbean.getNumberOfFinishedJobs());
        Assert.assertEquals(seed + 3, mbean.getNumberOfCancelledJobs());
        Assert.assertEquals(seed + 4, mbean.getNumberOfFailedJobs());
        Assert.assertEquals(seed + 5, mbean.getNumberOfProcessedJobs());
        Assert.assertEquals(seed + 6, mbean.getNumberOfActiveJobs());
        Assert.assertEquals(seed + 7, mbean.getNumberOfQueuedJobs());
        Assert.assertEquals(seed + 8, mbean.getNumberOfJobs());
        Assert.assertEquals(seed + 9, mbean.getLastActivatedJobTime());
        Assert.assertEquals(new Date(seed + 9), mbean.getLastActivatedJobDate());
        Assert.assertEquals(seed + 10, mbean.getLastFinishedJobTime());
        Assert.assertEquals(new Date(seed + 10), mbean.getLastFinishedJobDate());
        Assert.assertEquals(seed + 11, mbean.getAverageWaitingTime());
        Assert.assertEquals(seed + 12, mbean.getAverageProcessingTime());
    }

}
