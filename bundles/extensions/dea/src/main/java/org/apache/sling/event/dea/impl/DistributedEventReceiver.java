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
package org.apache.sling.event.dea.impl;

import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.event.dea.DEAConstants;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the distributed event receiver.
 * It listens for all distributable events and stores them in the
 * repository for other cluster instance to pick them up.
 * <p>
 * This component is scheduled to run some clean up tasks in the
 * background periodically.
 * <p>
 */
public class DistributedEventReceiver
    implements EventHandler, Runnable, TopologyEventListener {

    /** Special topic to stop the queue. */
    private static final String TOPIC_STOPPED = "org/apache/sling/event/dea/impl/STOPPED";

    /** Logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** A local queue for writing received events into the repository. */
    private final BlockingQueue<Event> writeQueue = new LinkedBlockingQueue<Event>();

    /** The resource resolver factory. */
    private final ResourceResolverFactory resourceResolverFactory;

    /** The current instance id. */
    private final String slingId;

    /** The root path for events . */
    private final String rootPath;

    /** The root path for events written by this instance. */
    private final String ownRootPath;

    /** The cleanup period. */
    private final int cleanupPeriod;

    /** Resolver used for writing. */
    private volatile ResourceResolver writerResolver;

    /** Is the background task still running? */
    private volatile boolean running;

    /** The current instances if this is the leader. */
    private volatile Set<String> instances;

    /** The service registration. */
    private volatile ServiceRegistration serviceRegistration;

    public DistributedEventReceiver(final BundleContext bundleContext,
            final String rootPath,
            final String ownRootPath,
            final int cleanupPeriod,
            final ResourceResolverFactory rrFactory,
            final SlingSettingsService settings) {
        this.rootPath = rootPath;
        this.ownRootPath = ownRootPath;
        this.resourceResolverFactory = rrFactory;
        this.slingId = settings.getSlingId();
        this.cleanupPeriod = cleanupPeriod;

        this.running = true;
        // start writer thread
        final Thread writerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // create service registration properties
                final Dictionary<String, Object> props = new Hashtable<String, Object>();
                props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

                // listen for all OSGi events with the distributable flag
                props.put(EventConstants.EVENT_TOPIC, "*");
                props.put(EventConstants.EVENT_FILTER, "(" + DEAConstants.PROPERTY_DISTRIBUTE + "=*)");
                // schedule this service every 30 minutes
                props.put("scheduler.period", 1800L);
                props.put("scheduler.concurrent", Boolean.FALSE);

                final ServiceRegistration reg =
                        bundleContext.registerService(new String[] {EventHandler.class.getName(),
                                                                   Runnable.class.getName(),
                                                                   TopologyEventListener.class.getName()},
                                                      DistributedEventReceiver.this, props);

                DistributedEventReceiver.this.serviceRegistration = reg;

                try {
                    writerResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
                    ResourceUtil.getOrCreateResource(writerResolver,
                            ownRootPath,
                            DistributedEventAdminImpl.RESOURCE_TYPE_FOLDER,
                            DistributedEventAdminImpl.RESOURCE_TYPE_FOLDER,
                            true);
                } catch (final Exception e) {
                    // there is nothing we can do except log!
                    logger.error("Error during resource resolver creation.", e);
                    running = false;
                }
                try {
                    processWriteQueue();
                } catch (final Throwable t) { //NOSONAR
                    logger.error("Writer thread stopped with exception: " + t.getMessage(), t);
                    running = false;
                }
                if ( writerResolver != null ) {
                    writerResolver.close();
                    writerResolver = null;
                }
            }
        });
        writerThread.start();
    }

    /**
     * Deactivate this component.
     */
    public void stop() {
        if ( this.serviceRegistration != null ) {
            this.serviceRegistration.unregister();
            this.serviceRegistration = null;
        }
        // stop background threads by putting empty objects into the queue
        this.running = false;
        try {
            this.writeQueue.put(new Event(TOPIC_STOPPED, (Dictionary<String, Object>)null));
        } catch (final InterruptedException e) {
            this.ignoreException(e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Background thread writing events into the queue.
     */
    private void processWriteQueue() {
        while ( this.running ) {
            // so let's wait/get the next event from the queue
            Event event = null;
            try {
                event = this.writeQueue.take();
            } catch (final InterruptedException e) {
                this.ignoreException(e);
                Thread.currentThread().interrupt();
                this.running = false;
            }
            if ( event != null && this.running ) {
                try {
                    this.writeEvent(event);
                } catch (final Exception e) {
                    this.logger.error("Exception during writing the event to the resource tree.", e);
                }
            }
        }
    }

    /** Counter for events. */
    private final AtomicLong eventCounter = new AtomicLong(0);

    /**
     * Write an event to the resource tree.
     * @param event The event
     * @throws PersistenceException
     */
    private void writeEvent(final Event event)
    throws PersistenceException {
        final Calendar now = Calendar.getInstance();

        final StringBuilder sb = new StringBuilder(this.ownRootPath);
        sb.append('/');
        sb.append(now.get(Calendar.YEAR));
        sb.append('/');
        sb.append(now.get(Calendar.MONTH) + 1);
        sb.append('/');
        sb.append(now.get(Calendar.DAY_OF_MONTH));
        sb.append('/');
        sb.append(now.get(Calendar.HOUR_OF_DAY));
        sb.append('/');
        sb.append(now.get(Calendar.MINUTE));
        sb.append('/');
        sb.append("event-");
        sb.append(String.valueOf(eventCounter.getAndIncrement()));

        // create properties
        final Map<String, Object> properties = new HashMap<String, Object>();

        final String[] propNames = event.getPropertyNames();
        if ( propNames != null && propNames.length > 0 ) {
            for(final String propName : propNames) {
                properties.put(propName, event.getProperty(propName));
            }
        }

        properties.remove(DEAConstants.PROPERTY_DISTRIBUTE);
        properties.put(EventConstants.EVENT_TOPIC, event.getTopic());
        properties.put(DEAConstants.PROPERTY_APPLICATION, this.slingId);
        final Object oldRT = properties.get(ResourceResolver.PROPERTY_RESOURCE_TYPE);
        if ( oldRT != null ) {
            properties.put("event.dea." + ResourceResolver.PROPERTY_RESOURCE_TYPE, oldRT);
        }
        properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, DistributedEventAdminImpl.RESOURCE_TYPE_EVENT);
        ResourceUtil.getOrCreateResource(this.writerResolver,
                sb.toString(),
                properties,
                DistributedEventAdminImpl.RESOURCE_TYPE_FOLDER,
                true);
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        try {
            this.writeQueue.put(event);
        } catch (final InterruptedException ex) {
            this.ignoreException(ex);
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

    /**
     * This method is invoked periodically.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        this.cleanUpObsoleteInstances();
        this.cleanUpObsoleteEvents();
    }

    private void cleanUpObsoleteInstances() {
        final Set<String> slingIds = this.instances;
        if ( slingIds != null ) {
            this.instances = null;
            this.logger.debug("Checking for old instance trees for distributed events.");
            ResourceResolver resolver = null;
            try {
                resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

                final Resource baseResource = resolver.getResource(this.rootPath);
                // sanity check - should never be null
                if ( baseResource != null ) {
                    final ResourceHelper.BatchResourceRemover brr = ResourceHelper.getBatchResourceRemover(50);
                    final Iterator<Resource> iter = baseResource.listChildren();
                    while ( iter.hasNext() ) {
                        final Resource rootResource = iter.next();
                        if ( !slingIds.contains(rootResource.getName()) ) {
                            brr.delete(rootResource);
                        }
                    }
                    // final commit for outstanding deletes
                    resolver.commit();
                }

            } catch (final PersistenceException pe) {
                // in the case of an error, we just log this as a warning
                this.logger.warn("Exception during job resource tree cleanup.", pe);
            } catch (final LoginException ignore) {
                this.ignoreException(ignore);
            } finally {
                if ( resolver != null ) {
                    resolver.close();
                }
            }
        }
    }

    private void cleanUpObsoleteEvents() {
        if ( this.cleanupPeriod > 0 ) {
            this.logger.debug("Cleaning up distributed events, removing all entries older than {} minutes.", this.cleanupPeriod);

            ResourceResolver resolver = null;
            try {
                resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
                final ResourceHelper.BatchResourceRemover brr = ResourceHelper.getBatchResourceRemover(50);

                final Resource baseResource = resolver.getResource(this.ownRootPath);
                // sanity check - should never be null
                if ( baseResource != null ) {
                    final Calendar oldDate = Calendar.getInstance();
                    oldDate.add(Calendar.MINUTE, -1 * this.cleanupPeriod);

                    // check years
                    final int oldYear = oldDate.get(Calendar.YEAR);
                    final Iterator<Resource> yearIter = baseResource.listChildren();
                    while ( yearIter.hasNext() ) {
                        final Resource yearResource = yearIter.next();
                        final int year = Integer.valueOf(yearResource.getName());
                        if ( year < oldYear ) {
                            brr.delete(yearResource);
                        } else if ( year == oldYear ) {

                            // same year - check months
                            final int oldMonth = oldDate.get(Calendar.MONTH) + 1;
                            final Iterator<Resource> monthIter = yearResource.listChildren();
                            while ( monthIter.hasNext() ) {
                                final Resource monthResource = monthIter.next();
                                final int month = Integer.valueOf(monthResource.getName());
                                if ( month < oldMonth ) {
                                    brr.delete(monthResource);
                                } else if ( month == oldMonth ) {

                                    // same month - check days
                                    final int oldDay = oldDate.get(Calendar.DAY_OF_MONTH);
                                    final Iterator<Resource> dayIter = monthResource.listChildren();
                                    while ( dayIter.hasNext() ) {
                                        final Resource dayResource = dayIter.next();
                                        final int day = Integer.valueOf(dayResource.getName());
                                        if ( day < oldDay ) {
                                            brr.delete(dayResource);
                                        } else if ( day == oldDay ) {

                                            // same day - check hours
                                            final int oldHour = oldDate.get(Calendar.HOUR_OF_DAY);
                                            final Iterator<Resource> hourIter = dayResource.listChildren();
                                            while ( hourIter.hasNext() ) {
                                                final Resource hourResource = hourIter.next();
                                                final int hour = Integer.valueOf(hourResource.getName());
                                                if ( hour < oldHour ) {
                                                    brr.delete(hourResource);
                                                } else if ( hour == oldHour ) {

                                                    // same hour - check minutes
                                                    final int oldMinute = oldDate.get(Calendar.MINUTE);
                                                    final Iterator<Resource> minuteIter = hourResource.listChildren();
                                                    while ( minuteIter.hasNext() ) {
                                                        final Resource minuteResource = minuteIter.next();

                                                        final int minute = Integer.valueOf(minuteResource.getName());
                                                        if ( minute < oldMinute ) {
                                                            brr.delete(minuteResource);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // final commit for outstanding resources
                resolver.commit();

            } catch (final PersistenceException pe) {
                // in the case of an error, we just log this as a warning
                this.logger.warn("Exception during job resource tree cleanup.", pe);
            } catch (final LoginException ignore) {
                this.ignoreException(ignore);
            } finally {
                if ( resolver != null ) {
                    resolver.close();
                }
            }
        }
    }

    /**
     * @see org.apache.sling.discovery.TopologyEventListener#handleTopologyEvent(org.apache.sling.discovery.TopologyEvent)
     */
    @Override
    public void handleTopologyEvent(final TopologyEvent event) {
        if ( event.getType() == Type.TOPOLOGY_CHANGING ) {
            this.instances = null;
        } else if ( event.getType() == Type.TOPOLOGY_CHANGED || event.getType() == Type.TOPOLOGY_INIT ) {
            if ( event.getNewView().getLocalInstance().isLeader() ) {
                final Set<String> set = new HashSet<String>();
                for(final InstanceDescription desc : event.getNewView().getInstances() ) {
                    set.add(desc.getSlingId());
                }
                this.instances = set;
            }
        }
    }
}

