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
package org.apache.sling.event.impl.dea;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Deactivate;

/**
 * This event handler distributes events across an application cluster.
 *
 * We schedule this event handler to run in the background and clean up
 * obsolete events.
 */
@Component(immediate=true)
@Service(value={EventHandler.class})
@Property(name=EventConstants.EVENT_TOPIC, value=SlingConstants.TOPIC_RESOURCE_ADDED)
public class DistributedEventSender
    implements EventHandler {

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Is the background task still running? */
    private volatile boolean running;

    /** A local queue for serializing the event processing. */
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<String>();

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private DistributedEventAdminConfiguration config;

    @Reference
    private EventAdmin eventAdmin;

    @Activate
    protected void activate() {
        this.running = true;
        final Thread backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runInBackground();
                } catch (Throwable t) { //NOSONAR
                    logger.error("Background thread stopped with exception: " + t.getMessage(), t);
                    running = false;
                }
            }
        });
        backgroundThread.start();
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        // stop background threads by putting empty objects into the queue
        this.running = false;
        try {
            this.queue.put("");
        } catch (final InterruptedException e) {
            this.ignoreException(e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Read an event from the resource
     * @return The event object or <code>null</code>
     */
    private Event readEvent(final Resource eventResource) {
        try {
            final ValueMap vm = ResourceHelper.getValueMap(eventResource);
            final String topic = vm.get(EventConstants.EVENT_TOPIC, String.class);
            final Map<String, Object> properties = ResourceHelper.cloneValueMap(vm);
            // only send event if there are no read errors, otherwise discard it
            @SuppressWarnings("unchecked")
            final List<Exception> readErrorList = (List<Exception>) properties.remove(ResourceHelper.PROPERTY_MARKER_READ_ERROR_LIST);
            if ( readErrorList == null ) {
                properties.remove(EventConstants.EVENT_TOPIC);

                try {
                    // special handling for job notification jobs for compatibility
                    if ( topic.startsWith("org/apache/sling/event/notification/job/") ) {
                        final String jobTopic = (String)properties.get(JobUtil.NOTIFICATION_PROPERTY_JOB_TOPIC);
                        if ( jobTopic != null) {
                            final Event jobEvent = new Event(jobTopic, properties);
                            properties.put(JobUtil.PROPERTY_NOTIFICATION_JOB, jobEvent);
                        }
                    }
                    final Event event = new Event(topic, properties);
                    return event;
                } catch (final IllegalArgumentException iae) {
                    // this exception occurs if the topic is not correct (it should never happen,
                    // but you never know)
                    logger.error("Unable to read event: " + iae.getMessage(), iae);
                }
            } else {
                for(final Exception e : readErrorList) {
                    logger.warn("Unable to read distributed event from " + eventResource.getPath(), e);
                }
            }
        } catch (final InstantiationException ie) {
            // something happened with the resource in the meanitime
            this.ignoreException(ie);
        }
        return null;
    }

    /**
     * Background thread
     */
    private void runInBackground() {
        while ( this.running ) {
            // so let's wait/get the next event from the queue
            String path = null;
            try {
                path = this.queue.take();
            } catch (final InterruptedException e) {
                this.ignoreException(e);
                Thread.currentThread().interrupt();
                this.running = false;
            }
            if ( path != null && path.length() > 0 && this.running ) {
                ResourceResolver resolver = null;
                try {
                    resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
                    final Resource eventResource = resolver.getResource(path);
                    if ( eventResource.isResourceType(ResourceHelper.RESOURCE_TYPE_EVENT) ) {
                        final Event e = this.readEvent(eventResource);
                        if ( e != null ) {
                            // we check event admin as processing is async
                            final EventAdmin localEA = this.eventAdmin;
                            if ( localEA != null ) {
                                localEA.postEvent(e);
                            } else {
                                this.logger.error("Unable to post event as no event admin is available.");
                            }
                        }
                    }
                } catch (final LoginException ex) {
                    this.logger.error("Exception during creation of resource resolver.", ex);
                } finally {
                    if ( resolver != null ) {
                        resolver.close();
                    }
                }
            }
        }
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        if ( path != null
             && path.startsWith(this.config.getRootPathWithSlash())
             && !path.startsWith(this.config.getOwnRootPathWithSlash()) ) {

            try {
                this.queue.put(path);
            } catch (final InterruptedException ex) {
                this.ignoreException(ex);
                Thread.currentThread().interrupt();
            }
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
