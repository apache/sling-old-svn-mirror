/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.scheduler.impl;

import static org.apache.sling.commons.scheduler.Scheduler.VALUE_RUN_ON_LEADER;
import static org.apache.sling.commons.scheduler.Scheduler.VALUE_RUN_ON_SINGLE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.JobContext;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

@RunWith(MockitoJUnitRunner.class)
public class QuartzJobExecutorTest {
    private BundleContext context;
    private QuartzJobExecutor jobExecutor;
    private QuartzScheduler quartzScheduler;
    private boolean isRunnablePseudoJobCompleted;

    @Mock
    private JobExecutionContext executionContext;

    @Before
    public void setUp() throws Exception {
        context = MockOsgi.newBundleContext();
        jobExecutor = new QuartzJobExecutor();

        quartzScheduler = ActivatedQuartzSchedulerFactory.create(context, "testName");
    }

    @Test
    public void testRunnableJob() throws SchedulerException, InterruptedException {
        Thread job = new Thread(new SimpleRunnableJob());
        String jobName = "testName";
        Map<String, Serializable> jobConfig = new HashMap<String, Serializable>();

        //Adding a job just to receive a JobDetail object which is needed for testing
        quartzScheduler.addJob(1L, 1L, jobName, job, jobConfig, "0 * * * * ?", true);

        JobDetail jobDetail = quartzScheduler.getSchedulers().get("testName").getScheduler().getJobDetail(JobKey.jobKey(jobName));
        when(executionContext.getJobDetail()).thenReturn(jobDetail);

        isRunnablePseudoJobCompleted = false;
        jobExecutor.execute(executionContext);
        if (job.isAlive()) {
            synchronized (job) {
                if (job.isAlive()) {
                    job.join();
                }
            }
        }
        assertTrue(isRunnablePseudoJobCompleted);
    }

    @Test
    public void testJob() throws SchedulerException {
        Job job = new SimpleJob();
        String jobName = "testName";
        Map<String, Serializable> jobConfig = new HashMap<String, Serializable>();

        //Adding a job just to receive a JobDetail object which is needed for testing
        quartzScheduler.addJob(1L, 1L, jobName, job, jobConfig, "0 * * * * ?", true);

        JobDetail jobDetail = quartzScheduler.getSchedulers().get("testName").getScheduler().getJobDetail(JobKey.jobKey(jobName));
        when(executionContext.getJobDetail()).thenReturn(jobDetail);

        isRunnablePseudoJobCompleted = false;
        jobExecutor.execute(executionContext);
        assertTrue(isRunnablePseudoJobCompleted);
    }

    @Test
    public void testJobNotExecuted() throws SchedulerException {
        Job job = new SimpleJob();
        String jobName = "testName";
        Map<String, Serializable> jobConfig = new HashMap<String, Serializable>();

        //Adding a job just to receive a JobDetail object which is needed for testing
        quartzScheduler.addJob(1L, 1L, jobName, job, jobConfig, "0 * * * * ?", true);

        JobDetail jobDetail = quartzScheduler.getSchedulers().get("testName").getScheduler().getJobDetail(JobKey.jobKey(jobName));
        when(executionContext.getJobDetail()).thenReturn(jobDetail);
        //Job with this config should not be executed
        jobDetail.getJobDataMap().put(QuartzScheduler.DATA_MAP_RUN_ON, new String[]{VALUE_RUN_ON_LEADER});

        isRunnablePseudoJobCompleted = false;
        jobExecutor.execute(executionContext);
        assertFalse(isRunnablePseudoJobCompleted);
    }

    @Test
    public void testJobNotExecutedWithTwoRunOnParams() throws SchedulerException {
        Job job = new SimpleJob();
        String jobName = "testName";
        Map<String, Serializable> jobConfig = new HashMap<String, Serializable>();

        //Adding a job just to receive a JobDetail object which is needed for testing
        quartzScheduler.addJob(1L, 1L, jobName, job, jobConfig, "0 * * * * ?", true);

        JobDetail jobDetail = quartzScheduler.getSchedulers().get("testName").getScheduler().getJobDetail(JobKey.jobKey(jobName));
        when(executionContext.getJobDetail()).thenReturn(jobDetail);
        //Job with this config should not be executed
        jobDetail.getJobDataMap().put(QuartzScheduler.DATA_MAP_RUN_ON,
                new String[]{VALUE_RUN_ON_LEADER, VALUE_RUN_ON_SINGLE});
        QuartzJobExecutor.SLING_ID = "ANY STRING NOT EQUAL TO OF VALUE_RUN_ON_LEADER OR" +
                "VALUE_RUN_ON_SINGLE JUST A TEST CASE, NOTHING MORE";

        isRunnablePseudoJobCompleted = false;
        jobExecutor.execute(executionContext);
        assertFalse(isRunnablePseudoJobCompleted);
    }

    @Test
    public void testJobExecutedWithTwoRunOnParams() throws SchedulerException {
        Job job = new SimpleJob();
        String jobName = "testName";
        Map<String, Serializable> jobConfig = new HashMap<String, Serializable>();

        //Adding a job just to receive a JobDetail object which is needed for testing
        quartzScheduler.addJob(1L, 1L, jobName, job, jobConfig, "0 * * * * ?", true);

        JobDetail jobDetail = quartzScheduler.getSchedulers().get("testName").getScheduler().getJobDetail(JobKey.jobKey(jobName));
        when(executionContext.getJobDetail()).thenReturn(jobDetail);
        //Job with this config should not be executed
        jobDetail.getJobDataMap().put(QuartzScheduler.DATA_MAP_RUN_ON,
                new String[]{VALUE_RUN_ON_LEADER, VALUE_RUN_ON_SINGLE});
        //In this case, when SLING_ID is equal to one of values above
        //Job should be executed
        QuartzJobExecutor.SLING_ID = VALUE_RUN_ON_SINGLE;

        isRunnablePseudoJobCompleted = false;
        jobExecutor.execute(executionContext);
        assertTrue(isRunnablePseudoJobCompleted);
    }

    @Test
    public void testReferences() {
        String testName = "testName";
        Map<String, Serializable> testMap = new HashMap<String, Serializable>();
        QuartzJobExecutor.JobContextImpl underTest = new QuartzJobExecutor.JobContextImpl(testName, testMap);

        assertTrue(underTest.getConfiguration().equals(testMap));
        assertTrue(underTest.getName().equals(testName));
    }

    @Test
    public void testLazyScheduler() {
        assertTrue(quartzScheduler.getSchedulers().isEmpty());
    }

    @After
    public void deactivateScheduler() {
        quartzScheduler.deactivate(context);
    }

    private class SimpleJob implements Job {
        @Override
        public void execute(JobContext context) {
            isRunnablePseudoJobCompleted = true;
        }
    }

    private class SimpleRunnableJob implements Runnable {
        @Override
        public void run() {
            isRunnablePseudoJobCompleted = true;
        }
    }

}
