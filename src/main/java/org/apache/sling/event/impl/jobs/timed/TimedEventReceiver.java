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
package org.apache.sling.event.impl.jobs.timed;

import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An event handler for timed events.
 * The events are written into the resource tree in an async thread.
 */
@Component(immediate=true)
@Service(EventHandler.class)
@Property(name=EventConstants.EVENT_TOPIC, value=EventUtil.TOPIC_TIMED_EVENT)
public class TimedEventReceiver implements EventHandler {

    public static final String RESOURCE_PROPERTY_TE_EXPRESSION = "slingevent:expression";
    public static final String RESOURCE_PROPERTY_TE_DATE = "slingevent:date";
    public static final String RESOURCE_PROPERTY_TE_PERIOD = "slingevent:period";

    public static final String TIMED_EVENT_RESOURCE_TYPE = "slingevent:TimedEvent";

    /** Default logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private TimedEventConfiguration config;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /** A local queue for writing received events into the resource tree. */
    private final BlockingQueue<Event> writeQueue = new LinkedBlockingQueue<Event>();

    /** Is the background task still running? */
    private volatile boolean running;

    /**
     * Activate this component.
     * Start writer thread
     */
    @Activate
    protected void activate() {
        this.running = true;
        // start writer thread
        final Thread writerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ResourceResolver writerResolver = null;
                try {
                    writerResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
                    ResourceHelper.getOrCreateBasePath(writerResolver, config.getResourcePath());
                    writerResolver.commit();
                } catch (final Exception e) {
                    // there is nothing we can do except log!
                    logger.error("Error during resource resolver creation.", e);
                    running = false;
                } finally {
                    if ( writerResolver != null ) {
                        writerResolver.close();
                        writerResolver = null;
                    }
                }
                try {
                    processWriteQueue();
                } catch (final Throwable t) { //NOSONAR
                    logger.error("Writer thread stopped with exception: " + t.getMessage(), t);
                    running = false;
                }
            }
        });
        writerThread.start();
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        // stop background threads by putting empty objects into the queue
        this.running = false;
        try {
            this.writeQueue.put(new Event(Utility.TOPIC_STOPPED, (Dictionary<String, Object>)null));
        } catch (final InterruptedException e) {
            this.ignoreException(e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Background thread processing the write queue
     */
    private void processWriteQueue() {
        while ( this.running ) {
            Event event = null;
            try {
                event = this.writeQueue.take();
            } catch (final InterruptedException e) {
                this.ignoreException(e);
                Thread.currentThread().interrupt();
                this.running = false;
            }
            if ( this.running && event != null ) {
                // check for schedule info
                ScheduleInfo scheduleInfo = null;
                try {
                    scheduleInfo = new ScheduleInfo(event);
                    this.writeEvent(event, scheduleInfo);
                } catch (final IllegalArgumentException iae) {
                    this.logger.error(iae.getMessage(), iae);
                }
            }
        }
    }

    private void writeEvent(final Event event, final ScheduleInfo scheduleInfo) {
        ResourceResolver writerResolver = null;
        try {
            final StringBuilder sb = new StringBuilder(this.config.getResourcePathWithSlash());
            sb.append(scheduleInfo.topic.replace('/', '.'));
            sb.append('/');
            sb.append(scheduleInfo.jobId);
            final String path = sb.toString();

            // is there already a resource?
            writerResolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Resource foundResource = writerResolver.getResource(path);
            if ( scheduleInfo.isStopEvent() ) {
                logger.debug("Received stop event for {}", scheduleInfo.jobId);
                // if this is a stop event, we should remove the resource from the resource tree
                // if there is no resource someone else was faster and we can ignore this
                if ( foundResource != null ) {
                    try {
                        writerResolver.delete(foundResource);
                        writerResolver.commit();
                    } catch (final PersistenceException pe) {
                        // we ignore this
                        this.ignoreException(pe);
                    }
                }
            } else {
                logger.debug("Received start/update event for {}", scheduleInfo.jobId);
                // if there is already a resource, it means we must handle an update
                // TODO - we should use a hash for this!
                if ( foundResource != null ) {
                    try {
                        writerResolver.delete(foundResource);
                        writerResolver.commit();
                    } catch (final PersistenceException pe) {
                        // we ignore this
                        this.ignoreException(pe);
                        writerResolver.refresh();
                    }
                }

                try {
                    // create properties
                    final Map<String, Object> properties = new HashMap<String, Object>();

                    final String[] propNames = event.getPropertyNames();
                    if ( propNames != null && propNames.length > 0 ) {
                        for(final String propName : propNames) {
                            if ( !ResourceHelper.ignoreProperty(propName) ) {
                                properties.put(propName, event.getProperty(propName));
                            }
                        }
                    }
                    properties.put(EventConstants.EVENT_TOPIC, scheduleInfo.topic);
                    properties.put(JobUtil.PROPERTY_JOB_CREATED, Calendar.getInstance());
                    if ( scheduleInfo.date != null ) {
                        final Calendar c = Calendar.getInstance();
                        c.setTime(scheduleInfo.date);
                        properties.put(RESOURCE_PROPERTY_TE_DATE, c);
                    }
                    if ( scheduleInfo.expression != null ) {
                        properties.put(RESOURCE_PROPERTY_TE_EXPRESSION, scheduleInfo.expression);
                    }
                    if ( scheduleInfo.period != null ) {
                        properties.put(RESOURCE_PROPERTY_TE_PERIOD, scheduleInfo.period.longValue());
                    }

                    // write event to the resource tree
                    properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, TIMED_EVENT_RESOURCE_TYPE);
                    ResourceHelper.getOrCreateResource(writerResolver,
                            path,
                            properties);
                } catch (final PersistenceException pe) {
                    // we ignore this
                    this.ignoreException(pe);
                }
            }
        } catch (final LoginException le) {
            this.ignoreException(le);
        } finally {
            if ( writerResolver != null ) {
                writerResolver.close();
                writerResolver = null;
            }
        }
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        if ( logger.isDebugEnabled() ) {
            logger.debug("Received timed event {}", EventUtil.toString(event));
        }
        // queue the event in order to respond quickly
        try {
            this.writeQueue.put(event);
        } catch (final InterruptedException e) {
            this.ignoreException(e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Helper method which just logs the exception in debug mode.
     * @param e
     */
    private void ignoreException(final Exception e) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Ignored exception " + e.getMessage(), e);
        }
    }
}
