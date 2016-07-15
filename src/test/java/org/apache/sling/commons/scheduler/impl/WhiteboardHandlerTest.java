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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

public class WhiteboardHandlerTest {
    private WhiteboardHandler handler;
    private BundleContext context;
    private QuartzScheduler quartzScheduler;
    //We will test WhiteboardHandler methods from ServiceTrackerCustomizer
    //since it uses its methods.
    private ServiceTrackerCustomizer customizer;

    @Before
    public void setUp() throws Exception {
        context = MockOsgi.newBundleContext();
        handler = new WhiteboardHandler();

        //Getting private field through injection
        Field schedulerField = WhiteboardHandler.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);

        //Creating quartzscheduler for private field and activating it
        quartzScheduler = ActivatedQuartzSchedulerFactory.create(context, "testName");

        //Injecting quartzScheduler to WhiteboardHandler
        schedulerField.set(handler, quartzScheduler);

        handler.activate(context);
        Field trackerField = WhiteboardHandler.class.getDeclaredField("serviceTracker");
        trackerField.setAccessible(true);

        ServiceTracker serviceTracker = (ServiceTracker) trackerField.get(handler);
        Field customizerField = ServiceTracker.class.getDeclaredField("customizer");
        customizerField.setAccessible(true);
        customizer = (ServiceTrackerCustomizer) customizerField.get(serviceTracker);
    }

    @Test
    public void testAddingService() throws SchedulerException {
        Thread service = new Thread();
        String serviceName = "serviceName";
        String schedulerName = "testScheduler";
        Long period = 1L;
        Integer times = 2;

        Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_RUN_ON, Scheduler.VALUE_RUN_ON_LEADER);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT, Boolean.FALSE);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_IMMEDIATE, Boolean.FALSE);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_NAME, schedulerName);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_PERIOD, period);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_TIMES, times);
        serviceProps.put(Constants.SERVICE_PID, "1");
        serviceProps.put(Constants.SERVICE_ID, 1L);

        //Register service only to get a reference of it.
        //This reference is needed to test our method.
        context.registerService(serviceName, service, serviceProps);
        ServiceReference reference = context.getServiceReference(serviceName);
        JobKey jobKey = JobKey.jobKey(schedulerName + "." + reference.getProperty(Constants.SERVICE_ID));

        assertNull(quartzScheduler.getSchedulers().get("testName"));
        customizer.addingService(reference);
        assertNotNull(quartzScheduler.getSchedulers().get("testName").getScheduler().getJobDetail(jobKey));
    }

    @Test
    public void testUnregisterService() throws SchedulerException {
        Thread service = new Thread();
        String serviceName = "serviceName";
        String schedulerName = "testScheduler";
        Long period = 1L;
        Integer times = 2;

        Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_RUN_ON, Scheduler.VALUE_RUN_ON_LEADER);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_EXPRESSION, "0 * * * * ?");
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT, Boolean.FALSE);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_IMMEDIATE, Boolean.FALSE);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_NAME, schedulerName);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_PERIOD, period);
        serviceProps.put(Scheduler.PROPERTY_SCHEDULER_TIMES, times);
        serviceProps.put(Constants.SERVICE_PID, "1");
        serviceProps.put(Constants.SERVICE_ID, 1L);

        //Register service only to get a reference of it.
        //This reference is needed to test our method.
        context.registerService(serviceName, service, serviceProps);
        ServiceReference reference = context.getServiceReference(serviceName);
        JobKey jobKey = JobKey.jobKey(schedulerName + "." + reference.getProperty(Constants.SERVICE_ID));

        assertNull(quartzScheduler.getSchedulers().get("testName"));
        customizer.addingService(reference);
        assertNotNull(quartzScheduler.getSchedulers().get("testName").getScheduler().getJobDetail(jobKey));
        customizer.removedService(reference, service);
        assertNull(quartzScheduler.getSchedulers().get("testName").getScheduler().getJobDetail(jobKey));
    }

    @After
    public void deactivateScheduler() {
        handler.deactivate();
        quartzScheduler.deactivate(context);
    }
}
