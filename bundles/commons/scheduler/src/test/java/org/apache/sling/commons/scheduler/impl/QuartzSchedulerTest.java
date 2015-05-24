package org.apache.sling.commons.scheduler.impl;

import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.commons.threads.impl.DefaultThreadPoolManager;
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
import org.osgi.framework.Constants;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QuartzSchedulerTest {
    private final String SCHEDULER_SERVICE_NAME = "scheduler";
    private BundleContext context;
    private QuartzScheduler scheduler;
    private Dictionary<String, Object> props;
    private Map<String, Object> scheduleActivationProps;

    @Mock
    private Bundle bundle;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        context = MockOsgi.newBundleContext();

        props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "org.apache.sling.commons.threads.impl.DefaultThreadPoolManager");
        props.put(Constants.SERVICE_PID, "org.apache.sling.commons.threads.impl.DefaultThreadPoolManager");
        ThreadPoolManager manager = new DefaultThreadPoolManager(context, props);

        scheduler = new QuartzScheduler();
        Class<?> schedulerClass = scheduler.getClass();
        Field managerField = schedulerClass.getDeclaredField("threadPoolManager");
        managerField.setAccessible(true);
        managerField.set(scheduler, manager);

        scheduleActivationProps = new HashMap<String, Object>();
        scheduleActivationProps.put("poolName", "testName");
        scheduler.activate(context, scheduleActivationProps);
        context.registerService(SCHEDULER_SERVICE_NAME, scheduler, props);
    }

    @Test
    public void testRunNow() {
        InternalScheduleOptions scheduleOptions = (InternalScheduleOptions) scheduler.NOW();
        assertNotNull("Trigger cannot be null", scheduleOptions.trigger);
        assertNull("IllegalArgumentException must be null", scheduleOptions.argumentException);

        scheduleOptions = (InternalScheduleOptions) scheduler.NOW(1, 1);
        assertEquals("Times argument must be higher than 1 or -1", scheduleOptions.argumentException.getMessage());

        scheduleOptions = (InternalScheduleOptions) scheduler.NOW(-1, 0);
        assertEquals("Period argument must be higher than 0", scheduleOptions.argumentException.getMessage());

        scheduleOptions = (InternalScheduleOptions) scheduler.NOW(-1, 2);
        assertNull(scheduleOptions.argumentException);
        assertNotNull(scheduleOptions.trigger);

        scheduleOptions = (InternalScheduleOptions) scheduler.NOW(2, 2);
        assertNull(scheduleOptions.argumentException);
        assertNotNull(scheduleOptions.trigger);
    }

    @Test
    public void testAddJobWithIncorrectJobObject() throws SchedulerException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Job object is neither an instance of " + Runnable.class.getName() + " nor " + Job.class.getName());
        scheduler.addJob(1L, 1L, "testName", new Object(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
    }

    @Test
    public void testAddJobWithoutCronExpression() throws SchedulerException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Expression can't be null");
        scheduler.addJob(1L, 1L, "testName", new Thread(), new HashMap<String, Serializable>(), null, true);
    }

    @Test
    public void testAddJobWithInvalidCronExpression() throws SchedulerException {
        String invalidExpression = "invalidExpression";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Expressionis invalid : " + invalidExpression);
        scheduler.addJob(1L, 1L, "testName", new Thread(), new HashMap<String, Serializable>(), invalidExpression, true);
    }

    @Test
    public void testWithoutScheduler() throws Exception {
        //Setting scheduler.scheduler to null in deactivate method
        scheduler.deactivate(context);

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Scheduler is not available anymore.");
        scheduler.addJob(1L, 1L, "testName", new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);

        scheduler.activate(context, scheduleActivationProps);
    }

    @Test
    public void testAddJob() throws SchedulerException {
        Scheduler s = scheduler.getScheduler();
        scheduler.addJob(1L, 1L, "testName", new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        assertTrue(s.checkExists(JobKey.jobKey("testName")));
        assertFalse(s.checkExists(JobKey.jobKey("wrongName")));
    }

    @Test
    public void testRemoveJob() throws SchedulerException {
        String jobName = "testName";
        Scheduler s = scheduler.getScheduler();
        scheduler.addJob(1L, 1L, jobName, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        assertTrue(s.checkExists(JobKey.jobKey(jobName)));
        scheduler.removeJob(1L, jobName);
        assertFalse(s.checkExists(JobKey.jobKey(jobName)));
    }

    @Test
    public void testAtDateTime() {
        InternalScheduleOptions options = (InternalScheduleOptions) scheduler.AT(null, 2, 1);
        assertEquals("Date can't be null", options.argumentException.getMessage());

        options = (InternalScheduleOptions) scheduler.AT(new Date(), 1, 1);
        assertEquals("Times argument must be higher than 1 or -1", options.argumentException.getMessage());

        options = (InternalScheduleOptions) scheduler.AT(new Date(), 2, 0);
        assertEquals("Period argument must be higher than 0", options.argumentException.getMessage());

        options = (InternalScheduleOptions) scheduler.AT(new Date(), 2, 1);
        assertNull("IllegalArgumentException must be null", options.argumentException);
        assertNotNull("Trigger cannot be null", options.trigger);

        options = (InternalScheduleOptions) scheduler.AT(new Date(), -1, 1);
        assertNull("IllegalArgumentException must be null", options.argumentException);
        assertNotNull("Trigger cannot be null", options.trigger);
    }

    @Test
    public void testAtTime() {
        InternalScheduleOptions options = (InternalScheduleOptions) scheduler.AT(null);
        assertNotNull(options.argumentException);
        assertEquals("Date can't be null", options.argumentException.getMessage());
        assertNull(options.trigger);

        options = (InternalScheduleOptions) scheduler.AT(new Date());
        assertNotNull(options.trigger);
        assertNull(options.argumentException);
    }

    @Test
    public void testPeriodicWithIncorrectPeriod() throws SchedulerException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Period argument must be higher than 0");
        scheduler.addPeriodicJob(3L, 3L, "anyName", new Thread(), new HashMap(), 0L, true, true);
    }

    @Test
    public void testPeriodic() throws SchedulerException {
        String jobName = "anyName";
        String otherJobName = "anyOtherName";
        Scheduler s = scheduler.getScheduler();

        scheduler.addPeriodicJob(4L, 4L, jobName, new Thread(), new HashMap(), 2L, true, true);
        assertTrue("Job must exists", s.checkExists(JobKey.jobKey(jobName)));

        scheduler.addPeriodicJob(5L, 5L, otherJobName, new Thread(), new HashMap(), 2L, true, false);
        assertTrue("Job must exists", s.checkExists(JobKey.jobKey(otherJobName)));
    }

    @Test
    public void testSchedule() {
        assertTrue(scheduler.schedule(2L, 2L, new Thread(), new InternalScheduleOptions(TriggerBuilder.newTrigger())));
        assertFalse(scheduler.schedule(2L, 2L, new Thread(), new InternalScheduleOptions(new IllegalArgumentException())));
    }

    @Test
    public void testUnschedule() throws Exception {
        String jobName = "jobToUnschedule";
        scheduler.addJob(6L, 6L, jobName, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        assertTrue(scheduler.unschedule(6L, jobName));

        scheduler.addJob(6L, 6L, jobName, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        assertFalse(scheduler.unschedule(6L, null));
        assertFalse(scheduler.unschedule(6L, "incorrectName"));

        scheduler.deactivate(context);
        assertFalse(scheduler.unschedule(6L, jobName));
        scheduler.activate(context, scheduleActivationProps);
    }

    @Test
    public void testBundleChangedWithStoppedBundle() throws SchedulerException {
        String firstJob = "testName1";
        String secondJob = "testName2";
        when(bundle.getBundleId()).thenReturn(2L);

        scheduler.addJob(1L, 1L, firstJob, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        scheduler.addJob(2L, 2L, secondJob, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);

        BundleEvent event = new BundleEvent(BundleEvent.STOPPED, bundle);
        scheduler.bundleChanged(event);

        Scheduler s = scheduler.getScheduler();
        assertTrue(s.checkExists(JobKey.jobKey(firstJob)));
        assertFalse(s.checkExists(JobKey.jobKey(secondJob)));
    }

    @Test
    public void testBundleChangedWithStartedBundle() throws SchedulerException {
        String firstJob = "testName1";
        String secondJob = "testName2";
        when(bundle.getBundleId()).thenReturn(2L);

        scheduler.addJob(1L, 1L, firstJob, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        scheduler.addJob(2L, 2L, secondJob, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);

        BundleEvent event = new BundleEvent(BundleEvent.STARTED, bundle);
        scheduler.bundleChanged(event);

        Scheduler s = scheduler.getScheduler();
        assertTrue(s.checkExists(JobKey.jobKey(firstJob)));
        assertTrue(s.checkExists(JobKey.jobKey(secondJob)));
    }

    @Test
    public void testBundleChangedWithoutScheduler() throws Exception {
        String firstJob = "testName1";
        String secondJob = "testName2";
        Long bundleIdToRemove = 2L;
        when(bundle.getBundleId()).thenReturn(bundleIdToRemove);

        scheduler.addJob(1L, 1L, firstJob, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);
        scheduler.addJob(bundleIdToRemove, 2L, secondJob, new Thread(), new HashMap<String, Serializable>(), "0 * * * * ?", true);

        Scheduler s = scheduler.getScheduler();
        Field f = scheduler.getClass().getDeclaredField("scheduler");
        f.setAccessible(true);
        f.set(scheduler, null);

        BundleEvent event = new BundleEvent(BundleEvent.STOPPED, bundle);
        scheduler.bundleChanged(event);

        assertTrue(s.checkExists(JobKey.jobKey(firstJob)));
        assertTrue(s.checkExists(JobKey.jobKey(secondJob)));

        f.set(scheduler, s);
    }

    @After
    public void deactivateScheduler() {
        scheduler.deactivate(context);
    }
}
