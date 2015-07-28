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
                btx.createFilter("(|(" + Constants.OBJECTCLASS + "=" + Runnable.class.getName() + ")" +
                 "(" + Constants.OBJECTCLASS + "=" + Job.class.getName() + "))"),
                new ServiceTrackerCustomizer() {

            public synchronized void  removedService(final ServiceReference reference, final Object service) {
                unregister(reference, service);
                btx.ungetService(reference);
            }

            public synchronized void modifiedService(final ServiceReference reference, final Object service) {
                unregister(reference, service);
                register(reference, service);
            }

            public synchronized Object addingService(final ServiceReference reference) {
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
     * @param ctx The component context.
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

    /**
     * Create unique identifier
     * @param type
     * @param ref
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
        name = name + "." + ref.getProperty(Constants.SERVICE_ID);
        return name;
    }

    /**
     * Register a job or task
     * @param type The type (job or task)
     * @param ref The service reference
     * @throws IllegalArgumentException
     */
    private void register(final ServiceReference ref, final Object job) {
        try {
            final String name = getServiceIdentifier(ref);
            final Boolean concurrent = getBooleanProperty(ref, Scheduler.PROPERTY_SCHEDULER_CONCURRENT);
            final Object runOn = ref.getProperty(Scheduler.PROPERTY_SCHEDULER_RUN_ON);
            String[] runOnOpts = null;
            if ( runOn instanceof String ) {
                runOnOpts = new String[] {runOn.toString()};
            } else if ( runOn instanceof String[] ) {
                runOnOpts = (String[])runOn;
            } else if ( runOn != null ) {
                this.logger.warn("Property {} ignored for scheduler {}", Scheduler.PROPERTY_SCHEDULER_RUN_ON, ref);
            }
            final String expression = getStringProperty(ref, Scheduler.PROPERTY_SCHEDULER_EXPRESSION);
            if ( expression != null ) {
                this.scheduler.schedule(ref.getBundle().getBundleId(), (Long)ref.getProperty(Constants.SERVICE_ID),
                        job, this.scheduler.EXPR(expression)
                        .name(name)
                        .canRunConcurrently((concurrent != null ? concurrent : true))
                        .onInstancesOnly(runOnOpts));
                this.idToNameMap.put((Long)ref.getProperty(Constants.SERVICE_ID), name);
            } else {
                final Long period = getLongProperty(ref, Scheduler.PROPERTY_SCHEDULER_PERIOD);
                if ( period != null ) {
                    if ( period < 1 ) {
                        this.logger.debug("Ignoring service {} : scheduler period is less than 1.", ref);
                    } else {
                        boolean immediate = false;
                        if ( ref.getProperty(Scheduler.PROPERTY_SCHEDULER_IMMEDIATE) != null ) {
                            immediate = getBooleanProperty(ref, Scheduler.PROPERTY_SCHEDULER_IMMEDIATE);
                        }
                        final Date date = new Date();
                        if ( !immediate ) {
                            date.setTime(System.currentTimeMillis() + period * 1000);
                        }
                        final Integer times = getIntegerProperty(ref, Scheduler.PROPERTY_SCHEDULER_TIMES);
                        if ( times != null && times < 1 ) {
                            this.logger.debug("Ignoring service {} : scheduler times is less than 1.", ref);
                        } else {
                            final int t = (times != null ? times : -1);
                            this.scheduler.schedule(ref.getBundle().getBundleId(), (Long)ref.getProperty(Constants.SERVICE_ID),
                                    job, this.scheduler.AT(date, t, period)
                                    .name(name)
                                    .canRunConcurrently((concurrent != null ? concurrent : true))
                                    .onInstancesOnly(runOnOpts));
                            this.idToNameMap.put((Long)ref.getProperty(Constants.SERVICE_ID), name);
                        }
                    }
                } else {
                    this.logger.debug("Ignoring servce {} : no scheduling property found.", ref);
                }
            }
        } catch ( final IllegalArgumentException iae) {
            this.logger.warn("Ignoring service {} : {}", ref, iae.getMessage());
        }
    }

    /**
     * Unregister a service.
     * @param ref The service reference.
     */
    private void unregister(final ServiceReference reference, final Object service) {
        final String name = idToNameMap.remove(reference.getProperty(Constants.SERVICE_ID));
        if ( name != null ) {
            this.scheduler.unschedule(reference.getBundle().getBundleId(), name);
        }
    }
}
