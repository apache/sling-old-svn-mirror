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

import org.apache.sling.jobs.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by ieb on 06/04/2016.
 */
public class JobImplTest {

    private Job job;
    private String jobId;
    @Mock
    private JobController jobController;
    private final long before = System.currentTimeMillis();
    private long after = System.currentTimeMillis();
    private boolean jobAborted;
    private boolean jobStopped;

    public JobImplTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void setUp() throws Exception {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                jobAborted = true;
                return null;
            }
        }).when(jobController).abort();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                jobStopped = true;
                return null;
            }
        }).when(jobController).stop();

        Thread.sleep(1);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("job.name", "Jobname");
        jobId = Utils.generateId();
        job = new JobImpl(Types.jobQueue("testtopic"), jobId, Types.jobType("testtype"), properties);
        Thread.sleep(1);
        after = System.currentTimeMillis();



    }


    @Test
    public void testGetTopic() throws Exception {
        assertEquals(Types.jobQueue("testtopic"), job.getQueue());
    }

    @Test
    public void testGetId() throws Exception {
        assertEquals(jobId, job.getId());
    }

    @Test
    public void testGetProperties() throws Exception {
        assertEquals(1, job.getProperties().size());
        assertEquals("Jobname", job.getProperties().get("job.name"));
    }

    @Test
    public void testGetRetryCount() throws Exception {
        assertEquals(0,job.getRetryCount());
    }

    @Test
    public void testGetNumberOfRetries() throws Exception {
        assertEquals(0,job.getNumberOfRetries());

    }

    @Test
    public void testGetStarted() throws Exception {
        assertEquals(0, job.getStarted());

    }

    @Test
    public void testGetCreated() throws Exception {
        long created = job.getCreated();
        assertTrue(created > before && created < after);

    }

    @Test
    public void testGetJobState() throws Exception {
        assertEquals(Job.JobState.CREATED, job.getJobState());

    }

    @Test
    public void testSetState() throws Exception {
        assertEquals(Job.JobState.CREATED,job.getJobState());
        job.setState(Job.JobState.STOPPED);
        assertEquals(Job.JobState.STOPPED, job.getJobState());

    }

    @Test
    public void testGetFinished() throws Exception {
        assertEquals(0,job.getFinished());

    }

    @Test
    public void testGetResultMessage() throws Exception {
        assertEquals("",job.getResultMessage());

    }

    @Test
    public void testGetController() throws Exception {
        assertEquals(null,job.getController());

    }

    @Test
    public void testSetJobController() throws Exception {
        assertEquals(null,job.getController());
        job.setJobController(jobController);
        assertEquals(jobController,job.getController());

    }

    @Test
    public void testRemoveJobController() throws Exception {
        assertEquals(null,job.getController());
        job.setJobController(jobController);
        assertEquals(jobController, job.getController());
        job.removeJobController();
        assertEquals(null, job.getController());

    }

    @Test
    public void testUpdate() throws Exception {
        JobImpl jobImpl = (JobImpl) job;
        JobUpdateBuilder jobUpdateBuilder = jobImpl.newJobUpdateBuilder();
        assertNotNull(jobUpdateBuilder);
        Map<String,Object> updateProperties = new HashMap<String, Object>();
        updateProperties.put("updateprop", "updatevalue");
        JobUpdate jobUpdate = jobUpdateBuilder
                .command(JobUpdate.JobUpdateCommand.START_JOB)
                .putAll(updateProperties)
                .put("extra", "extravalue")
                .build();

        jobImpl.update(jobUpdate);

        // Job update does not change the job state even if the command says so.
        // the state change comes from a job starter.
        assertEquals(Job.JobState.CREATED, job.getJobState());
        assertEquals(3, job.getProperties().size());
        assertEquals("Jobname", job.getProperties().get("job.name"));
        assertEquals("extravalue", job.getProperties().get("extra"));
        assertEquals("updatevalue", job.getProperties().get("updateprop"));

        JobImpl jobImp = new JobImpl(jobUpdate);
        assertEquals(job.getId(), jobImp.getId());


        // update with some, but not all the new properties.
        updateProperties = new HashMap<String, Object>();
        updateProperties.put("updateprop", "updatevalue2");


        jobImpl.update(jobImpl.newJobUpdateBuilder()
                .command(JobUpdate.JobUpdateCommand.UPDATE_JOB)
                .putAll(updateProperties)
                .put("extra", "extravalue")
                .build());

        assertEquals(Job.JobState.CREATED, job.getJobState());
        assertEquals(3, job.getProperties().size());
        assertEquals("Jobname", job.getProperties().get("job.name"));
        assertEquals("extravalue", job.getProperties().get("extra"));
        assertEquals("updatevalue2", job.getProperties().get("updateprop"));

        // remove a property.
        jobImpl.update(jobImpl.newJobUpdateBuilder()
                .command(JobUpdate.JobUpdateCommand.UPDATE_JOB)
                .put("extra", JobUpdate.JobPropertyAction.REMOVE)
                .build());

        assertEquals(Job.JobState.CREATED, job.getJobState());
        assertEquals(2, job.getProperties().size());
        assertEquals("Jobname", job.getProperties().get("job.name"));
        assertEquals("updatevalue2", job.getProperties().get("updateprop"));

        // remove a property.
        JobUpdate retryUpdate = jobImpl.newJobUpdateBuilder()
                .command(JobUpdate.JobUpdateCommand.RETRY_JOB)
                .put("updateprop", "updatevalue2")
                .build();
        set(retryUpdate, "numberOfRetries", 10);
        set(retryUpdate, "retryCount", 10);
        jobImpl.update(retryUpdate);

        assertEquals(Job.JobState.CREATED, job.getJobState());
        assertEquals(2, job.getProperties().size());
        assertEquals("Jobname", job.getProperties().get("job.name"));
        assertEquals("updatevalue2", job.getProperties().get("updateprop"));
        assertEquals(20, job.getNumberOfRetries());
        assertEquals(10,job.getRetryCount());



        // Abort the job without a controller present.
        jobAborted = false;
        jobStopped = false;
        jobImpl.update(jobImpl.newJobUpdateBuilder()
                .command(JobUpdate.JobUpdateCommand.ABORT_JOB)
                .put("extra2", "newextra2")
                .build());

        assertEquals(Job.JobState.CREATED, job.getJobState());
        assertEquals(2, job.getProperties().size());
        assertEquals("Jobname", job.getProperties().get("job.name"));
        assertEquals("updatevalue2", job.getProperties().get("updateprop"));
        assertFalse(jobAborted);
        assertFalse(jobStopped);

        // set a controller as if the job was stated.
        job.setJobController(jobController);
        jobAborted = false;
        jobStopped = false;

        // abort the job
        jobImpl.update(jobImpl.newJobUpdateBuilder()
                .command(JobUpdate.JobUpdateCommand.ABORT_JOB)
                .put("extra2", "newextra3")
                .build());

        assertEquals(Job.JobState.CREATED, job.getJobState());
        // abort does not update properties.
        assertEquals(2, job.getProperties().size());
        assertEquals("Jobname", job.getProperties().get("job.name"));
        assertEquals("updatevalue2", job.getProperties().get("updateprop"));
        assertTrue(jobAborted);
        assertFalse(jobStopped);

        jobAborted = false;
        jobStopped = false;

        JobUpdate wrongOrder = jobImpl.newJobUpdateBuilder()
                .command(JobUpdate.JobUpdateCommand.UPDATE_JOB)
                .put("extra2", "newextra4")
                .build();

        Thread.sleep(2);
        // abort the job
        jobImpl.update(jobImpl.newJobUpdateBuilder()
                .command(JobUpdate.JobUpdateCommand.STOP_JOB)
                .put("extra2", "newextra4")
                .build());

        assertEquals(Job.JobState.CREATED, job.getJobState());
        assertEquals(2, job.getProperties().size());
        assertEquals("Jobname", job.getProperties().get("job.name"));
        assertEquals("updatevalue2", job.getProperties().get("updateprop"));
        assertFalse(jobAborted);
        assertTrue(jobStopped);


        try {
            jobImpl.update(wrongOrder);
            fail("Update should have been rejected due to out of sequence");
        } catch ( IllegalStateException e) {
            // ok
        }

        JobUpdate wrongId = new JobUpdateImpl(Utils.generateId(), JobUpdate.JobUpdateCommand.UPDATE_JOB);
        try {
            jobImpl.update(wrongId);
            fail("Update should have been rejected due wrong Id");
        } catch ( IllegalArgumentException e) {
            // ok
        }

        JobUpdate expired = jobImpl.newJobUpdateBuilder()
                .command(JobUpdate.JobUpdateCommand.STOP_JOB)
                .put("extra2", "newextra4")
                .build();
        set(expired, "expires", System.currentTimeMillis());
        Thread.sleep(1);
        try {
            jobImpl.update(expired);
            fail("Update should have expired");
        } catch ( IllegalStateException e) {
            // ok
        }

        try {
            JobUpdate failJobUpdate = new JobUpdateBuilderImpl(job.getId()).command(JobUpdate.JobUpdateCommand.START_JOB).build();
            fail("should not be able to build a job by ID only");
        } catch ( IllegalStateException e) {
            // ok
            JobUpdate stopJob = new JobUpdateBuilderImpl(job.getId()).command(JobUpdate.JobUpdateCommand.STOP_JOB).build();
            JobUpdate abortJob = new JobUpdateBuilderImpl(job.getId()).command(JobUpdate.JobUpdateCommand.ABORT_JOB).build();
        }


    }

    private void set(Object obj, String name, Object v) throws NoSuchFieldException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, v);
    }


    @Test
    public  void testNewJobUpdateBuilder() throws Exception {
        long updateBefore = System.currentTimeMillis();
        Thread.sleep(1);

        JobImpl jobImpl = (JobImpl) job;
        JobUpdateBuilder jobUpdateBuilder = jobImpl.newJobUpdateBuilder();
        assertNotNull(jobUpdateBuilder);
        Map<String,Object> updateProperties = new HashMap<String, Object>();
        updateProperties.put("updateprop", "updatevalue");
        JobUpdate jobUpdate = jobUpdateBuilder
                .command(JobUpdate.JobUpdateCommand.START_JOB)
                .putAll(updateProperties)
                .put("extra", "extravalue")
                .build();


        Thread.sleep(1);
        long updateAfter = System.currentTimeMillis();
        assertNotNull(jobUpdate);
        assertEquals(jobId, jobUpdate.getId());
        assertEquals(Types.jobQueue("testtopic"), jobUpdate.getQueue());
        assertEquals("", jobUpdate.getResultMessage());
        assertTrue(jobUpdate.expires() > updateAfter);
        assertTrue(jobUpdate.updateTimestamp() > updateBefore && jobUpdate.updateTimestamp() < updateAfter);
        assertTrue(jobUpdate.getCreated() > before && jobUpdate.getCreated() < after);
        assertEquals(JobUpdate.JobUpdateCommand.START_JOB, jobUpdate.getCommand());
        assertEquals(Job.JobState.CREATED, jobUpdate.getState());
        assertEquals(0, jobUpdate.getStarted());
        assertEquals(0,jobUpdate.getFinished());
        assertEquals(0,jobUpdate.getRetryCount());
        assertEquals(0,jobUpdate.getNumberOfRetries());
        assertEquals(2,jobUpdate.getProperties().size());
        assertEquals("extravalue", jobUpdate.getProperties().get("extra"));
        assertEquals("updatevalue",jobUpdate.getProperties().get("updateprop"));
    }
}