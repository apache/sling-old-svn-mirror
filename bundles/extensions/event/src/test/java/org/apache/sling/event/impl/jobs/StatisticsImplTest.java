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
package org.apache.sling.event.impl.jobs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.sling.event.impl.jobs.stats.StatisticsImpl;

public class StatisticsImplTest {

    protected StatisticsImpl stat;

    static long START_TIME = System.currentTimeMillis();

    @org.junit.Before public void setup() {
        this.stat = new StatisticsImpl();
    }

    @org.junit.Test public void testInitial() {
        assertTrue(this.stat.getStartTime() >= START_TIME);
        assertEquals(0, this.stat.getAverageProcessingTime());
        assertEquals(0, this.stat.getAverageWaitingTime());
        assertEquals(0, this.stat.getNumberOfActiveJobs());
        assertEquals(0, this.stat.getNumberOfCancelledJobs());
        assertEquals(0, this.stat.getNumberOfFailedJobs());
        assertEquals(0, this.stat.getNumberOfFinishedJobs());
        assertEquals(0, this.stat.getNumberOfJobs());
        assertEquals(0, this.stat.getNumberOfProcessedJobs());
        assertEquals(0, this.stat.getNumberOfQueuedJobs());
        assertEquals(-1, this.stat.getLastActivatedJobTime());
        assertEquals(-1, this.stat.getLastFinishedJobTime());
    }

    @org.junit.Test public void testIncDecQueued() {
        this.stat.incQueued();
        assertTrue(this.stat.getStartTime() >= START_TIME);
        assertEquals(0, this.stat.getAverageProcessingTime());
        assertEquals(0, this.stat.getAverageWaitingTime());
        assertEquals(0, this.stat.getNumberOfActiveJobs());
        assertEquals(0, this.stat.getNumberOfCancelledJobs());
        assertEquals(0, this.stat.getNumberOfFailedJobs());
        assertEquals(0, this.stat.getNumberOfFinishedJobs());
        assertEquals(1, this.stat.getNumberOfJobs());
        assertEquals(0, this.stat.getNumberOfProcessedJobs());
        assertEquals(1, this.stat.getNumberOfQueuedJobs());
        assertEquals(-1, this.stat.getLastActivatedJobTime());
        assertEquals(-1, this.stat.getLastFinishedJobTime());

        this.stat.incQueued();
        assertTrue(this.stat.getStartTime() >= START_TIME);
        assertEquals(0, this.stat.getAverageProcessingTime());
        assertEquals(0, this.stat.getAverageWaitingTime());
        assertEquals(0, this.stat.getNumberOfActiveJobs());
        assertEquals(0, this.stat.getNumberOfCancelledJobs());
        assertEquals(0, this.stat.getNumberOfFailedJobs());
        assertEquals(0, this.stat.getNumberOfFinishedJobs());
        assertEquals(2, this.stat.getNumberOfJobs());
        assertEquals(0, this.stat.getNumberOfProcessedJobs());
        assertEquals(2, this.stat.getNumberOfQueuedJobs());
        assertEquals(-1, this.stat.getLastActivatedJobTime());
        assertEquals(-1, this.stat.getLastFinishedJobTime());

        this.stat.decQueued();
        assertTrue(this.stat.getStartTime() >= START_TIME);
        assertEquals(0, this.stat.getAverageProcessingTime());
        assertEquals(0, this.stat.getAverageWaitingTime());
        assertEquals(0, this.stat.getNumberOfActiveJobs());
        assertEquals(0, this.stat.getNumberOfCancelledJobs());
        assertEquals(0, this.stat.getNumberOfFailedJobs());
        assertEquals(0, this.stat.getNumberOfFinishedJobs());
        assertEquals(1, this.stat.getNumberOfJobs());
        assertEquals(0, this.stat.getNumberOfProcessedJobs());
        assertEquals(1, this.stat.getNumberOfQueuedJobs());
        assertEquals(-1, this.stat.getLastActivatedJobTime());
        assertEquals(-1, this.stat.getLastFinishedJobTime());
    }

    @org.junit.Test public void testFinished() {
        long now = System.currentTimeMillis();
        this.stat.incQueued();
        this.stat.addActive(100);
        this.stat.finishedJob(200);
        this.stat.incQueued();
        this.stat.addActive(300);
        this.stat.finishedJob(800);

        assertTrue(this.stat.getStartTime() >= START_TIME);
        assertEquals(500, this.stat.getAverageProcessingTime());
        assertEquals(200, this.stat.getAverageWaitingTime());
        assertEquals(0, this.stat.getNumberOfActiveJobs());
        assertEquals(0, this.stat.getNumberOfCancelledJobs());
        assertEquals(0, this.stat.getNumberOfFailedJobs());
        assertEquals(2, this.stat.getNumberOfFinishedJobs());
        assertEquals(0, this.stat.getNumberOfJobs());
        assertEquals(2, this.stat.getNumberOfProcessedJobs());
        assertEquals(0, this.stat.getNumberOfQueuedJobs());
        assertTrue(this.stat.getLastActivatedJobTime() >= now);
        assertTrue(this.stat.getLastFinishedJobTime() >= now);

        now = System.currentTimeMillis();
        this.stat.incQueued();
        this.stat.addActive(200);
        assertTrue(this.stat.getStartTime() >= START_TIME);
        assertEquals(500, this.stat.getAverageProcessingTime());
        assertEquals(200, this.stat.getAverageWaitingTime());
        assertEquals(1, this.stat.getNumberOfActiveJobs());
        assertEquals(0, this.stat.getNumberOfCancelledJobs());
        assertEquals(0, this.stat.getNumberOfFailedJobs());
        assertEquals(2, this.stat.getNumberOfFinishedJobs());
        assertEquals(1, this.stat.getNumberOfJobs());
        assertEquals(2, this.stat.getNumberOfProcessedJobs());
        assertEquals(0, this.stat.getNumberOfQueuedJobs());
        assertTrue(this.stat.getLastActivatedJobTime() >= now);
        assertTrue(this.stat.getLastFinishedJobTime() <= now);

        now = System.currentTimeMillis();
        this.stat.finishedJob(200);
        assertTrue(this.stat.getStartTime() >= START_TIME);
        assertEquals(400, this.stat.getAverageProcessingTime());
        assertEquals(200, this.stat.getAverageWaitingTime());
        assertEquals(0, this.stat.getNumberOfActiveJobs());
        assertEquals(0, this.stat.getNumberOfCancelledJobs());
        assertEquals(0, this.stat.getNumberOfFailedJobs());
        assertEquals(3, this.stat.getNumberOfFinishedJobs());
        assertEquals(0, this.stat.getNumberOfJobs());
        assertEquals(3, this.stat.getNumberOfProcessedJobs());
        assertEquals(0, this.stat.getNumberOfQueuedJobs());
        assertTrue(this.stat.getLastActivatedJobTime() <= now);
        assertTrue(this.stat.getLastFinishedJobTime() >= now);
    }

    @org.junit.Test public void testFailAndCancel() {
        // we start with the results from the previous test!
        this.testFinished();

        long now = System.currentTimeMillis();
        this.stat.incQueued();
        this.stat.addActive(200);
        this.stat.failedJob();
        assertTrue(this.stat.getStartTime() >= START_TIME);
        assertEquals(400, this.stat.getAverageProcessingTime());
        assertEquals(200, this.stat.getAverageWaitingTime());
        assertEquals(0, this.stat.getNumberOfActiveJobs());
        assertEquals(0, this.stat.getNumberOfCancelledJobs());
        assertEquals(1, this.stat.getNumberOfFailedJobs());
        assertEquals(3, this.stat.getNumberOfFinishedJobs());
        assertEquals(1, this.stat.getNumberOfJobs());
        assertEquals(4, this.stat.getNumberOfProcessedJobs());
        assertEquals(1, this.stat.getNumberOfQueuedJobs());
        assertTrue(this.stat.getLastActivatedJobTime() >= now);
        assertTrue(this.stat.getLastFinishedJobTime() <= now);

        now = System.currentTimeMillis();
        this.stat.addActive(200);
        this.stat.cancelledJob();
        assertTrue(this.stat.getStartTime() >= START_TIME);
        assertEquals(400, this.stat.getAverageProcessingTime());
        assertEquals(200, this.stat.getAverageWaitingTime());
        assertEquals(0, this.stat.getNumberOfActiveJobs());
        assertEquals(1, this.stat.getNumberOfCancelledJobs());
        assertEquals(1, this.stat.getNumberOfFailedJobs());
        assertEquals(3, this.stat.getNumberOfFinishedJobs());
        assertEquals(0, this.stat.getNumberOfJobs());
        assertEquals(5, this.stat.getNumberOfProcessedJobs());
        assertEquals(0, this.stat.getNumberOfQueuedJobs());
        assertTrue(this.stat.getLastActivatedJobTime() >= now);
        assertTrue(this.stat.getLastFinishedJobTime() <= now);
    }

    @org.junit.Test public void  testMisc() {
        final StatisticsImpl stat2 = new StatisticsImpl(200);
        assertEquals(200, stat2.getStartTime());

        // update stat
        this.testFailAndCancel();

        long now = System.currentTimeMillis();
        final StatisticsImpl copy = new StatisticsImpl();
        copy.copyFrom(this.stat);
        assertTrue(copy.getStartTime() >= now);
        assertEquals(400, copy.getAverageProcessingTime());
        assertEquals(200, copy.getAverageWaitingTime());
        assertEquals(0, copy.getNumberOfActiveJobs());
        assertEquals(1, copy.getNumberOfCancelledJobs());
        assertEquals(1, copy.getNumberOfFailedJobs());
        assertEquals(3, copy.getNumberOfFinishedJobs());
        assertEquals(0, copy.getNumberOfJobs());
        assertEquals(5, copy.getNumberOfProcessedJobs());
        assertEquals(0, copy.getNumberOfQueuedJobs());
        assertTrue(copy.getLastActivatedJobTime() <= now);
        assertTrue(copy.getLastFinishedJobTime() <= now);

        now = System.currentTimeMillis();
        this.stat.incQueued();
        this.stat.addActive(200);
        this.stat.finishedJob(400);
        assertEquals(400, this.stat.getAverageProcessingTime());
        assertEquals(200, this.stat.getAverageWaitingTime());
        assertEquals(0, this.stat.getNumberOfActiveJobs());
        assertEquals(1, this.stat.getNumberOfCancelledJobs());
        assertEquals(1, this.stat.getNumberOfFailedJobs());
        assertEquals(4, this.stat.getNumberOfFinishedJobs());
        assertEquals(0, this.stat.getNumberOfJobs());
        assertEquals(6, this.stat.getNumberOfProcessedJobs());
        assertEquals(0, this.stat.getNumberOfQueuedJobs());
        assertTrue(this.stat.getLastActivatedJobTime() >= now);
        assertTrue(this.stat.getLastFinishedJobTime() >= now);

        copy.add(this.stat);
        assertTrue(copy.getStartTime() <= now);
        assertEquals(400, copy.getAverageProcessingTime());
        assertEquals(200, copy.getAverageWaitingTime());
        assertEquals(0, copy.getNumberOfActiveJobs());
        assertEquals(2, copy.getNumberOfCancelledJobs());
        assertEquals(2, copy.getNumberOfFailedJobs());
        assertEquals(7, copy.getNumberOfFinishedJobs());
        assertEquals(0, copy.getNumberOfJobs());
        assertEquals(11, copy.getNumberOfProcessedJobs());
        assertEquals(0, copy.getNumberOfQueuedJobs());
        assertTrue(copy.getLastActivatedJobTime() >= now);
        assertTrue(copy.getLastFinishedJobTime() >= now);

        this.stat.incQueued();
        this.stat.incQueued();
        assertEquals(400, this.stat.getAverageProcessingTime());
        assertEquals(200, this.stat.getAverageWaitingTime());
        assertEquals(0, this.stat.getNumberOfActiveJobs());
        assertEquals(1, this.stat.getNumberOfCancelledJobs());
        assertEquals(1, this.stat.getNumberOfFailedJobs());
        assertEquals(4, this.stat.getNumberOfFinishedJobs());
        assertEquals(2, this.stat.getNumberOfJobs());
        assertEquals(6, this.stat.getNumberOfProcessedJobs());
        assertEquals(2, this.stat.getNumberOfQueuedJobs());
        assertTrue(this.stat.getLastActivatedJobTime() >= now);
        assertTrue(this.stat.getLastFinishedJobTime() >= now);

        this.stat.clearQueued();
        assertEquals(400, this.stat.getAverageProcessingTime());
        assertEquals(200, this.stat.getAverageWaitingTime());
        assertEquals(0, this.stat.getNumberOfActiveJobs());
        assertEquals(1, this.stat.getNumberOfCancelledJobs());
        assertEquals(1, this.stat.getNumberOfFailedJobs());
        assertEquals(4, this.stat.getNumberOfFinishedJobs());
        assertEquals(0, this.stat.getNumberOfJobs());
        assertEquals(6, this.stat.getNumberOfProcessedJobs());
        assertEquals(0, this.stat.getNumberOfQueuedJobs());
        assertTrue(this.stat.getLastActivatedJobTime() >= now);
        assertTrue(this.stat.getLastFinishedJobTime() >= now);
    }
}
