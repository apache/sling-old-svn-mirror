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

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The quartz based implementation of the scheduler.
 *
 */
@Component(immediate=true)
public class WhiteboardHandler {

    private static final String SCHEDULED_JOB_FILTER =
            "(|(" + Constants.OBJECTCLASS + "=" + Runnable.class.getName() + ")" +
            "(" + Constants.OBJECTCLASS + "=" + Job.class.getName() + "))";

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private QuartzScheduler scheduler;

    private ServiceTracker serviceTracker;

    private final Map<Long, String> idToNameMap = new ConcurrentHashMap<Long, String>();

    /**
     * Activate this component.
     * @throws InvalidSyntaxException
     */
    @Activate
    protected void activate(final BundleContext btx) throws InvalidSyntaxException {
        this.serviceTracker = new ServiceTracker(btx,
                btx.createFilter(SCHEDULED_JOB_FILTER),
                new ServiceTrackerCustomizer() {

            @Override
            public void  removedService(final ServiceReference reference, final Object service) {
                unregister(reference);
                btx.ungetService(reference);
            }

            @Override
            public void modifiedService(final ServiceReference reference, final Object service) {
                unregister(reference);
                register(reference, service);
            }

            @Override
            public Object addingService(final ServiceReference reference) {
                final Object obj = btx.getService(reference);
                if ( obj != null ) {
                    register(reference, obj);
                }
                return obj;
            }
        });
        this.serviceTracker.open();
    }

    /**
     * Deactivate this component.
     * Stop the scheduler.
     */
    @Deactivate
    protected void deactivate() {
        if ( this.serviceTracker != null ) {
            this.serviceTracker.close();
            this.serviceTracker = null;
        }
    }

    private String getStringProperty(final ServiceReference ref, final String name) {
        final Object obj = ref.getProperty(name);
        if ( obj == null ) {
            return null;
        }
        if ( obj instanceof String ) {
            return (String)obj;
        }
        throw new IllegalArgumentException("Property " + name + " is not of type String");
    }

    private Boolean getBooleanProperty(final ServiceReference ref, final String name) {
        final Object obj = ref.getProperty(name);
        if ( obj == null ) {
            return null;
        }
        if ( obj instanceof Boolean ) {
            return (Boolean)obj;
        }
        throw new IllegalArgumentException("Property " + name + " is not of type Boolean");
    }

    private boolean getBooleanOrDefault(final ServiceReference ref, final String name, final boolean defaultValue) {
        final Boolean value = getBooleanProperty(ref, name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    private Long getLongProperty(final ServiceReference ref, final String name) {
        final Object obj = ref.getProperty(name);
        if ( obj == null ) {
            return null;
        }
        if ( obj instanceof Long ) {
            return (Long)obj;
        }
        throw new IllegalArgumentException("Property " + name + " is not of type Long");
    }

    private Integer getIntegerProperty(final ServiceReference ref, final String name) {
        final Object obj = ref.getProperty(name);
        if ( obj == null ) {
            return null;
        }
        if ( obj instanceof Integer ) {
            return (Integer)obj;
        }
        throw new IllegalArgumentException("Property " + name + " is not of type Integer");
    }

    private String[] getStringArray(final ServiceReference ref, final String name) {
        final Object value = ref.getProperty(name);
        if ( value instanceof String[] ) {
            return (String[])value;
        } else if ( value != null ) {
            return new String[] {value.toString()};
        }
        return null;
    }

    /**
     * Create unique identifier
     * @param ref The service reference
     * @throws IllegalArgumentException
     */
    private String getServiceIdentifier(final ServiceReference ref) {
        String name = getStringProperty(ref, Scheduler.PROPERTY_SCHEDULER_NAME);
        if ( name == null ) {
            name = getStringProperty(ref, Constants.SERVICE_PID);
            if ( name == null ) {
                name = "Registered Service";
            }
        }
        // now append service id to create a unique identifier
        name = name + "." + getLongProperty(ref, Constants.SERVICE_ID);
        return name;
    }

    /**
     * Register a job or task
     * @param ref The service reference
     * @param job The job object
     * @throws IllegalArgumentException
     */
    private void register(final ServiceReference ref, final Object job) {
        try {
            if (!tryScheduleExpression(ref, job) && !trySchedulePeriod(ref, job)) {
                this.logger.debug("Ignoring service {} : no scheduling property found.", ref);
            }

        } catch ( final IllegalArgumentException iae) {
            this.logger.warn("Ignoring service {} : {}", ref, iae.getMessage());
        }
    }

    /**
     * Unregister a service.
     * @param reference The service reference.
     */
    private void unregister(final ServiceReference reference) {
        final String name = idToNameMap.remove(getLongProperty(reference, Constants.SERVICE_ID));
        if ( name != null ) {
            this.scheduler.unschedule(reference.getBundle().getBundleId(), name);
        }
    }

    private boolean trySchedulePeriod(final ServiceReference ref, final Object job) {
        final Long period = getLongProperty(ref, Scheduler.PROPERTY_SCHEDULER_PERIOD);
        if ( period == null ) {
            return false;
        }

        if ( period < 1 ) {
            this.logger.debug("Ignoring service {} : scheduler period is less than 1.", ref);
        } else {
            final Date date = new Date();
            boolean immediate = getBooleanOrDefault(ref, Scheduler.PROPERTY_SCHEDULER_IMMEDIATE, false);
            if ( !immediate ) {
                date.setTime(System.currentTimeMillis() + period * 1000);
            }
            final Integer times = getIntegerProperty(ref, Scheduler.PROPERTY_SCHEDULER_TIMES);
            if ( times != null && times < 1 ) {
                this.logger.debug("Ignoring service {} : scheduler times is less than 1.", ref);
            } else {
                final int t = (times != null ? times : -1);
                scheduleJob(ref, job, this.scheduler.AT(date, t, period));
                return true;
            }
        }
        return false;
    }

    private boolean tryScheduleExpression(final ServiceReference ref, final Object job) {
        final String expression = getStringProperty(ref, Scheduler.PROPERTY_SCHEDULER_EXPRESSION);
        if ( expression != null ) {
            scheduleJob(ref, job, this.scheduler.EXPR(expression));
            return true;
        }
        return false;
    }

    private String[] getRunOpts(final ServiceReference ref) {
        return getStringArray(ref, Scheduler.PROPERTY_SCHEDULER_RUN_ON);
    }

    private void scheduleJob(final ServiceReference ref, final Object job, final ScheduleOptions scheduleOptions) {
        final String name = getServiceIdentifier(ref);
        final Boolean concurrent = getBooleanProperty(ref, Scheduler.PROPERTY_SCHEDULER_CONCURRENT);
        final String[] runOnOpts = getRunOpts(ref);

        final Object poolNameObj = ref.getProperty(Scheduler.PROPERTY_SCHEDULER_THREAD_POOL);
        final String poolName;
        if ( poolNameObj != null && poolNameObj.toString().trim().length() > 0 ) {
            poolName = poolNameObj.toString().trim();
        } else {
            poolName = null;
        }

        final ScheduleOptions options = scheduleOptions
                .name(name)
                .canRunConcurrently((concurrent != null ? concurrent : true))
                .threadPoolName(poolName)
                .onInstancesOnly(runOnOpts);

        final long bundleId = ref.getBundle().getBundleId();
        final Long serviceId = getLongProperty(ref, Constants.SERVICE_ID);
        this.scheduler.schedule(bundleId, serviceId, job, options);
        this.idToNameMap.put(serviceId, name);
    }
}
