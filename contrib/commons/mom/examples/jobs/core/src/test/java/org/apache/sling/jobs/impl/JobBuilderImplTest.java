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
package org.apache.sling.jobs.impl;

import org.apache.sling.jobs.Job;
import org.apache.sling.jobs.JobController;
import org.apache.sling.jobs.Types;
import org.apache.sling.jobs.impl.spi.JobStarter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static org.junit.Assert.*;

/**
 * Created by ieb on 05/04/2016.
 * Tests job builder.
 */
public class JobBuilderImplTest {

    @Mock
    private JobStarter jobStarter;

    private Queue<Job> queue;
    @Mock
    private JobController jobController;

    public JobBuilderImplTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void before() {
        queue = new ArrayBlockingQueue<Job>(1);
        Mockito.when(jobStarter.start(Mockito.any(Job.class))).then(new Answer<Job>() {
            @Override
            public Job answer(InvocationOnMock invocationOnMock) throws Throwable {
                queue.add((Job) invocationOnMock.getArguments()[0]);
                return (Job) invocationOnMock.getArguments()[0];
            }
        });
    }

    @Test
    public void testAddJob() {
        long start = System.currentTimeMillis();
        Map<String, Object> testMap = new HashMap<String, Object>();
        testMap.put("job.name", "Jobname");
        Job queuedJob = new JobBuilderImpl(jobStarter, Types.jobQueue("testtopic"), Types.jobType("testtype")).addProperties(testMap).add();
        assertEquals(1, queue.size());
        Job fromQueue = queue.remove();
        assertEquals(queuedJob, fromQueue);
        assertEquals(Types.jobQueue("testtopic"), fromQueue.getQueue());
        assertEquals("Jobname", fromQueue.getProperties().get("job.name"));
        assertNotNull(fromQueue.getId());
        long now = System.currentTimeMillis();
        assertTrue(fromQueue.getCreated() >= start);
        assertTrue(fromQueue.getCreated() <= now);
        assertEquals(Job.JobState.CREATED, fromQueue.getJobState());
        assertNull(fromQueue.getController());
        fromQueue.setJobController(jobController);
        assertEquals(jobController, fromQueue.getController());
        fromQueue.removeJobController();
        assertNull(fromQueue.getController());
        assertEquals("", fromQueue.getResultMessage());
        assertEquals(0, fromQueue.getFinished());
        assertEquals(0, fromQueue.getStarted());
        assertEquals(0, fromQueue.getNumberOfRetries());
        assertEquals(0, fromQueue.getRetryCount());

    }

}