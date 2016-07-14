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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;

import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;

@RunWith(MockitoJUnitRunner.class)
public class QuartzSchedulerTest {

    private Scheduler s;
    private SchedulerProxy proxy;
    private BundleContext context;
    private QuartzScheduler quartzScheduler;

    @Mock
    private Bundle bundle;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        context = MockOsgi.newBundleContext();
        quartzScheduler = ActivatedQuartzSchedulerFactory.create(context, "testName");
        s = quartzScheduler.getScheduler();
        Field sField = QuartzScheduler.class.getDeclaredField("scheduler");
        sField.setAccessible(true);
        this.proxy = (SchedulerProxy) sField.get(quartzScheduler);
    }

    @Test
    public void testRunNow() {
        InternalScheduleOptions scheduleOptions = (InternalScheduleOptions) quartzScheduler.NOW();
        assertNotNull("Trigger cannot be null", scheduleOptions.trigger);
        assertNull("IllegalArgumentException must be null", scheduleOptions.argumentException);

        scheduleOptions = (InternalScheduleOptions) quartzScheduler.NOW(1, 1);
        assertEquals("Times argument must be higher than 1 or -1", scheduleOptions.argumentException.getMessage());

        scheduleOptions = (InternalScheduleOptions) quartzScheduler.NOW(-1, 0);
        assertEquals("Period argument must be higher than 0", scheduleOptions.argumentException.getMessage());

        scheduleOptions = (InternalScheduleOptions) quartzScheduler.NOW(-1, 2);
        assertNull(scheduleOptions.argumentException);
        assertNotNull(scheduleOptions.trigger);

        scheduleOptions = (InternalScheduleOptions) quartzScheduler.NOW(2, 2);
        assertNull(scheduleOptions.argumentException);
        assertNotNull(scheduleOptions.trigger);
    }

    @Test
    public void testAddJobWithIncorrectJobObject() throws SchedulerException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Job object is neither an instance of " + Runnable.class.getName() + " nor " + Job.class.getName());
        quartzScheduler.addJob(1L, 1L, "testName", new Object(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
    }

    @Test
    public void testAddJobWithoutCronExpression() throws SchedulerException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Expression can't be null");
        quartzScheduler.addJob(1L, 1L, "testName", new Thread(), new HashMap<String, Serializable>(), null, true);
    }

    @Test
    public void testAddJobWithInvalidCronExpression() throws SchedulerException {
        String invalidExpression = "invalidExpression";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Expressionis invalid : " + invalidExpression);
        quartzScheduler.addJob(1L, 1L, "testName", new Thread(), new HashMap<String, Serializable>(), invalidExpression, true);
    }

    @Test
    public void testWithoutScheduler() throws Exception {
        setInternalSchedulerToNull();

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Scheduler is not available anymore.");
        quartzScheduler.addJob(1L, 1L, "testName", new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);

        returnInternalSchedulerBack();
    }

    @Test
    public void testAddJob() throws SchedulerException {
        quartzScheduler.addJob(1L, 1L, "testName", new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        assertTrue(s.checkExists(JobKey.jobKey("testName")));
        assertFalse(s.checkExists(JobKey.jobKey("wrongName")));
    }

    @Test
    public void testAddJobTwice() throws SchedulerException {
        quartzScheduler.addJob(1L, 1L, "testName", new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        assertTrue(s.checkExists(JobKey.jobKey("testName")));
        //Add very same job twice to check that there is no conflicts and previous
        quartzScheduler.addJob(1L, 1L, "testName", new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        assertTrue(s.checkExists(JobKey.jobKey("testName")));
    }

    @Test
    public void testRemoveJob() throws SchedulerException {
        String jobName = "testName";
        quartzScheduler.addJob(1L, 1L, jobName, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        assertTrue(s.checkExists(JobKey.jobKey(jobName)));
        quartzScheduler.removeJob(1L, jobName);
        assertFalse(s.checkExists(JobKey.jobKey(jobName)));
    }

    @Test
    public void testAtDateTime() {
        InternalScheduleOptions options = (InternalScheduleOptions) quartzScheduler.AT(null, 2, 1);
        assertEquals("Date can't be null", options.argumentException.getMessage());

        options = (InternalScheduleOptions) quartzScheduler.AT(new Date(), 1, 1);
        assertEquals("Times argument must be higher than 1 or -1", options.argumentException.getMessage());

        options = (InternalScheduleOptions) quartzScheduler.AT(new Date(), 2, 0);
        assertEquals("Period argument must be higher than 0", options.argumentException.getMessage());

        options = (InternalScheduleOptions) quartzScheduler.AT(new Date(), 2, 1);
        assertNull("IllegalArgumentException must be null", options.argumentException);
        assertNotNull("Trigger cannot be null", options.trigger);

        options = (InternalScheduleOptions) quartzScheduler.AT(new Date(), -1, 1);
        assertNull("IllegalArgumentException must be null", options.argumentException);
        assertNotNull("Trigger cannot be null", options.trigger);
    }

    @Test
    public void testAtTime() {
        InternalScheduleOptions options = (InternalScheduleOptions) quartzScheduler.AT(null);
        assertNotNull(options.argumentException);
        assertEquals("Date can't be null", options.argumentException.getMessage());
        assertNull(options.trigger);

        options = (InternalScheduleOptions) quartzScheduler.AT(new Date());
        assertNotNull(options.trigger);
        assertNull(options.argumentException);
    }

    @Test
    public void testPeriodicWithIncorrectPeriod() throws SchedulerException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Period argument must be higher than 0");
        quartzScheduler.addPeriodicJob(3L, 3L, "anyName", new Thread(), new HashMap(), 0L, true, true);
    }

    @Test
    public void testPeriodic() throws SchedulerException {
        String jobName = "anyName";
        String otherJobName = "anyOtherName";

        quartzScheduler.addPeriodicJob(4L, 4L, jobName, new Thread(), new HashMap(), 2L, true, true);
        assertTrue("Job must exists", s.checkExists(JobKey.jobKey(jobName)));

        quartzScheduler.addPeriodicJob(5L, 5L, otherJobName, new Thread(), new HashMap(), 2L, true, false);
        assertTrue("Job must exists", s.checkExists(JobKey.jobKey(otherJobName)));
    }

    @Test
    public void testSchedule() {
        assertTrue(quartzScheduler.schedule(2L, 2L, new Thread(), new InternalScheduleOptions(TriggerBuilder.newTrigger())));
        assertFalse(quartzScheduler.schedule(2L, 2L, new Thread(), new InternalScheduleOptions(new IllegalArgumentException())));
    }

    @Test
    public void testUnschedule() throws Exception {
        String jobName = "jobToUnschedule";
        quartzScheduler.addJob(6L, 6L, jobName, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        assertTrue(quartzScheduler.unschedule(6L, jobName));

        quartzScheduler.addJob(6L, 6L, jobName, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        assertFalse(quartzScheduler.unschedule(6L, null));
        assertFalse(quartzScheduler.unschedule(6L, "incorrectName"));

        setInternalSchedulerToNull();
        assertFalse(quartzScheduler.unschedule(6L, jobName));
        returnInternalSchedulerBack();
    }

    @Test
    public void testBundleChangedWithStoppedBundle() throws SchedulerException {
        String firstJob = "testName1";
        String secondJob = "testName2";
        when(bundle.getBundleId()).thenReturn(2L);

        quartzScheduler.addJob(1L, 1L, firstJob, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        quartzScheduler.addJob(2L, 2L, secondJob, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);

        BundleEvent event = new BundleEvent(BundleEvent.STOPPED, bundle);
        quartzScheduler.bundleChanged(event);

        assertTrue(s.checkExists(JobKey.jobKey(firstJob)));
        assertFalse(s.checkExists(JobKey.jobKey(secondJob)));
    }

    @Test
    public void testBundleChangedWithStartedBundle() throws SchedulerException {
        String firstJob = "testName1";
        String secondJob = "testName2";
        when(bundle.getBundleId()).thenReturn(2L);

        quartzScheduler.addJob(1L, 1L, firstJob, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        quartzScheduler.addJob(2L, 2L, secondJob, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);

        BundleEvent event = new BundleEvent(BundleEvent.STARTED, bundle);
        quartzScheduler.bundleChanged(event);

        assertTrue(s.checkExists(JobKey.jobKey(firstJob)));
        assertTrue(s.checkExists(JobKey.jobKey(secondJob)));
    }

    @Test
    public void testBundleChangedWithoutScheduler() throws Exception {
        String firstJob = "testName1";
        String secondJob = "testName2";
        Long bundleIdToRemove = 2L;
        when(bundle.getBundleId()).thenReturn(bundleIdToRemove);

        quartzScheduler.addJob(1L, 1L, firstJob, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        quartzScheduler.addJob(bundleIdToRemove, 2L, secondJob, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);

        setInternalSchedulerToNull();

        BundleEvent event = new BundleEvent(BundleEvent.STOPPED, bundle);
        quartzScheduler.bundleChanged(event);

        assertTrue(s.checkExists(JobKey.jobKey(firstJob)));
        assertTrue(s.checkExists(JobKey.jobKey(secondJob)));

        returnInternalSchedulerBack();
    }

    @After
    public void deactivateScheduler() throws NoSuchFieldException, IllegalAccessException {
        if (quartzScheduler.getScheduler() == null) {
            returnInternalSchedulerBack();
        }
        quartzScheduler.deactivate(context);
    }

    private void setInternalSchedulerToNull() throws NoSuchFieldException, IllegalAccessException {
        Field sField = QuartzScheduler.class.getDeclaredField("scheduler");
        sField.setAccessible(true);
        sField.set(quartzScheduler, null);
    }

    private void returnInternalSchedulerBack() throws NoSuchFieldException, IllegalAccessException {
        Field sField = QuartzScheduler.class.getDeclaredField("scheduler");
        sField.setAccessible(true);
        if (quartzScheduler.getScheduler() == null && proxy != null) {
            sField.set(quartzScheduler, proxy);
        }
    }
}
