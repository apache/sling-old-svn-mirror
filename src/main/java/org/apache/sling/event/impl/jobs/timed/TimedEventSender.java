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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.JobContext;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.TimedEventStatusProvider;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An event handler for timed events.
 *
 */
@Component(immediate=true)
@Service(value={TimedEventStatusProvider.class, EventHandler.class, TopologyEventListener.class})
@Property(name=EventConstants.EVENT_TOPIC,
          value={SlingConstants.TOPIC_RESOURCE_ADDED,
                 SlingConstants.TOPIC_RESOURCE_REMOVED,
                 SlingConstants.TOPIC_RESOURCE_CHANGED,
                 ResourceHelper.BUNDLE_EVENT_STARTED,
                 ResourceHelper.BUNDLE_EVENT_UPDATED})
public class TimedEventSender
    implements Job, TimedEventStatusProvider, EventHandler, TopologyEventListener {

    private static final String JOB_TOPIC = "topic";

    private static final String JOB_CONFIG = "config";

    private static final String JOB_SCHEDULE_INFO = "info";

    /** Default logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Is the background task still running? */
    private volatile boolean running;

    /** Is this active? */
    private volatile boolean active;

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private TimedEventConfiguration config;

    @Reference
    private EventAdmin eventAdmin;

    /** A local queue for serializing the event processing. */
    private final BlockingQueue<Event> queue = new LinkedBlockingQueue<Event>();

    /** Started jobs. */
    private final Set<String> startedSchedulerJobs = new HashSet<String>();

    /** Unloaded events. */
    private Set<String>unloadedEvents = new HashSet<String>();

    private final AtomicBoolean threadStarted = new AtomicBoolean(false);

    /**
     * Activate this component.
     */
    @Activate
    protected void activate() {
        this.running = true;
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        this.running = false;
        this.stopScheduling();
    }

    private void stopScheduling() {
        final Scheduler localScheduler = this.scheduler;
        if ( localScheduler != null ) {
            for(final String id : this.startedSchedulerJobs ) {
                localScheduler.unschedule(id);
            }
        }
        this.startedSchedulerJobs.clear();

        // stop background threads by putting empty objects into the queue
        this.queue.clear();
        try {
            this.queue.put(new Event(Utility.TOPIC_STOPPED, (Dictionary<String, Object>)null));
        } catch (final InterruptedException e) {
            this.ignoreException(e);
            Thread.currentThread().interrupt();
        }
    }

    private void startScheduling() {
        final long now = System.currentTimeMillis();
        final Thread backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                loadEvents(now);
                if ( threadStarted.compareAndSet(false, true) ) {
                    try {
                        runInBackground();
                    } catch (final Throwable t) { //NOSONAR
                        logger.error("Background thread stopped with exception: " + t.getMessage(), t);
                        running = false;
                    }
                }
            }
        });
        backgroundThread.start();
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#runInBackground()
     */
    protected void runInBackground() {
        Event event = null;
        while ( this.running ) {
            // so let's wait/get the next event from the queue
            if ( event == null ) {
                try {
                    event = this.queue.take();
                } catch (final InterruptedException e) {
                    this.ignoreException(e);
                    Thread.currentThread().interrupt();
                    this.running = false;
                }
            }
            if ( event != null && this.running ) {
                // check event type
                if ( event.getTopic().equals(SlingConstants.TOPIC_RESOURCE_ADDED)
                     || event.getTopic().equals(SlingConstants.TOPIC_RESOURCE_CHANGED)) {
                    final String path = (String)event.getProperty(SlingConstants.PROPERTY_PATH);
                    event = null;
                    ResourceResolver resolver = null;
                    try {
                        resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
                        final Resource eventResource = resolver.getResource(path);
                        if ( TimedEventReceiver.TIMED_EVENT_RESOURCE_TYPE.equals(eventResource.getResourceType()) ) {
                            final ReadResult result = this.readEvent(eventResource);
                            if ( result != null ) {
                                if ( result.hasReadErrors ) {
                                    synchronized ( this.unloadedEvents ) {
                                        this.unloadedEvents.add(eventResource.getPath());
                                    }
                                } else {
                                    event = result.event;
                                }
                            }
                        }
                    } catch (final LoginException le) {
                        this.ignoreException(le);
                    } finally {
                        if ( resolver != null ) {
                            resolver.close();
                        }
                    }
                } else if ( event.getTopic().equals(SlingConstants.TOPIC_RESOURCE_REMOVED) ) {
                    final String path = (String)event.getProperty(SlingConstants.PROPERTY_PATH);
                    final String jobId = ResourceUtil.getName(path);
                    this.startedSchedulerJobs.remove(jobId);
                    logger.debug("Stopping job with id : {}", jobId);
                    this.scheduler.unschedule(jobId);
                    event = null;

                } else if ( !Utility.TOPIC_STOPPED.equals(event.getTopic()) ) {
                    ScheduleInfo scheduleInfo = null;
                    try {
                        scheduleInfo = new ScheduleInfo(event);
                    } catch (final IllegalArgumentException iae) {
                        this.logger.error(iae.getMessage());
                    }
                    if ( scheduleInfo != null ) {
                        // if something went wrong, we reschedule
                        if ( !this.processEvent(event, scheduleInfo) ) {
                            try {
                                this.queue.put(event);
                            } catch (final InterruptedException e) {
                                this.ignoreException(e);
                                Thread.currentThread().interrupt();
                                this.running = false;
                            }
                        }
                    }

                    event = null;
                } else if (Utility.TOPIC_STOPPED.equals(event.getTopic())){
                    // stopScheduling() puts this event on the queue, but the intention is unclear to me.
                    // as the threadStarted flag ensures the background thread is only started once, we must not stop
                    // the thread, otherwise its never started again upon topology changes.
                    event = null;
                } else {
                    // to ensure the event is reset to null in any case, in order to take from the queue again
                    // and to not fall into an endless busy loop
                    event = null;
                }
            }
        }
    }

    /**
     * Process the event.
     * If a scheduler is available, a job is scheduled or stopped.
     * @param event The incoming event.
     * @return
     */
    protected boolean processEvent(final Event event, final ScheduleInfo scheduleInfo) {
        final Scheduler localScheduler = this.scheduler;
        if ( localScheduler != null ) {
            // is this a stop event?
            if ( scheduleInfo.isStopEvent() ) {
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Stopping timed event " + event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_TOPIC) + "(" + scheduleInfo.jobId + ")");
                }
                this.startedSchedulerJobs.remove(scheduleInfo.jobId);
                localScheduler.unschedule(scheduleInfo.jobId);
                return true;
            }

            // Create configuration for scheduled job
            final Map<String, Serializable> config = new HashMap<String, Serializable>();
            // copy properties
            final Hashtable<String, Object> properties = new Hashtable<String, Object>();
            config.put(JOB_TOPIC, (String)event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_TOPIC));
            final String[] names = event.getPropertyNames();
            if ( names != null ) {
                for(int i=0; i<names.length; i++) {
                    properties.put(names[i], event.getProperty(names[i]));
                }
            }
            config.put(JOB_CONFIG, properties);
            config.put(JOB_SCHEDULE_INFO, scheduleInfo);

            final ScheduleOptions options;
            if ( scheduleInfo.expression != null ) {
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Adding timed event " + config.get(JOB_TOPIC) + "(" + scheduleInfo.jobId + ")" + " with cron expression " + scheduleInfo.expression);
                }
                options = localScheduler.EXPR(scheduleInfo.expression);
            } else if ( scheduleInfo.period != null ) {
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Adding timed event " + config.get(JOB_TOPIC) + "(" + scheduleInfo.jobId + ")" + " with period " + scheduleInfo.period);
                }
                final Date startDate = new Date(System.currentTimeMillis() + scheduleInfo.period * 1000);
                options = localScheduler.AT(startDate, -1, scheduleInfo.period);
            } else {
                // then it must be date
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Adding timed event " + config.get(JOB_TOPIC) + "(" + scheduleInfo.jobId + ")" + " with date " + scheduleInfo.date);
                }
                options = localScheduler.AT(scheduleInfo.date);
            }
            localScheduler.schedule(this, options.canRunConcurrently(false).name(scheduleInfo.jobId).config(config));
            this.startedSchedulerJobs.add(scheduleInfo.jobId);
            return true;
        } else {
            this.logger.error("No scheduler available to start timed event " + event);
        }
        return false;
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        if ( this.active ) {
            if ( ResourceHelper.BUNDLE_EVENT_STARTED.equals(event.getTopic())
              || ResourceHelper.BUNDLE_EVENT_UPDATED.equals(event.getTopic()) ) {
                // bundle event started or updated
                boolean doIt = false;
                synchronized ( this.unloadedEvents ) {
                    if ( this.unloadedEvents.size() > 0 ) {
                        doIt = true;
                    }
                }
                if ( doIt ) {
                    final Runnable t = new Runnable() {

                        @Override
                        public void run() {
                            synchronized (unloadedEvents) {
                                ResourceResolver resolver = null;
                                final Set<String> newUnloadedEvents = new HashSet<String>();
                                newUnloadedEvents.addAll(unloadedEvents);
                                try {
                                    resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
                                    for(final String path : unloadedEvents ) {
                                        newUnloadedEvents.remove(path);
                                        final Resource eventResource = resolver.getResource(path);
                                        final ReadResult result = readEvent(eventResource);
                                        if ( result != null ) {
                                            if ( result.hasReadErrors ) {
                                                newUnloadedEvents.add(path);
                                            } else {
                                                try {
                                                    queue.put(result.event);
                                                } catch (InterruptedException e) {
                                                    ignoreException(e);
                                                    Thread.currentThread().interrupt();
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                } catch (final LoginException re) {
                                    // unable to create resource resolver so we try it again next time
                                    ignoreException(re);
                                } finally {
                                    if ( resolver != null ) {
                                        resolver.close();
                                    }
                                    unloadedEvents.clear();
                                    unloadedEvents.addAll(newUnloadedEvents);
                                }
                            }
                        }

                    };
                    Environment.THREAD_POOL.execute(t);
                }
            } else {
                final String path = (String)event.getProperty(SlingConstants.PROPERTY_PATH);
                final String resourceType = (String)event.getProperty(SlingConstants.PROPERTY_RESOURCE_TYPE);
                if ( path != null && path.startsWith(this.config.getResourcePathWithSlash())
                     && (resourceType == null || TimedEventReceiver.TIMED_EVENT_RESOURCE_TYPE.equals(resourceType))) {
                    logger.debug("Received resource event for {} : {}", path, resourceType);
                    try {
                        this.queue.put(event);
                    } catch (final InterruptedException ignore) {
                        this.ignoreException(ignore);
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private void removeEvent(final ScheduleInfo info) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final StringBuilder sb = new StringBuilder(this.config.getResourcePathWithSlash());
            sb.append(info.topic.replace('/', '.'));
            sb.append('/');
            sb.append(info.jobId);
            final String path = sb.toString();

            final Resource eventResource = resolver.getResource(path);
            if ( eventResource != null ) {
                resolver.delete(eventResource);
                resolver.commit();
            }
        } catch (final LoginException le) {
            this.ignoreException(le);
        } catch (final PersistenceException pe) {
            // we ignore the exception if removing fails
            ignoreException(pe);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
    }

    /**
     * @see org.apache.sling.commons.scheduler.Job#execute(org.apache.sling.commons.scheduler.JobContext)
     */
    @Override
    public void execute(final JobContext context) {
        final String topic = (String) context.getConfiguration().get(JOB_TOPIC);
        @SuppressWarnings("unchecked")
        final Dictionary<String, Object> properties = (Dictionary<String, Object>) context.getConfiguration().get(JOB_CONFIG);
        final EventAdmin ea = this.eventAdmin;
        if ( ea != null ) {
            try {
                ea.postEvent(new Event(topic, properties));
            } catch (IllegalArgumentException iae) {
                this.logger.error("Scheduled event has illegal topic: " + topic, iae);
            }
        } else {
            this.logger.warn("Unable to send timed event as no event admin service is available.");
        }
        final ScheduleInfo info = (ScheduleInfo) context.getConfiguration().get(JOB_SCHEDULE_INFO);
        // is this job scheduled for a specific date?
        if ( info.date != null ) {
            // we can remove it from the resource tree
            this.removeEvent(info);
        }
    }

    /**
     * Load all active timed events from the resource tree.
     */
    private void loadEvents(final long startTime) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Calendar startDate = Calendar.getInstance();
            startDate.setTimeInMillis(startTime);

            final StringBuilder buf = new StringBuilder(64);

            buf.append("//element(*,");
            buf.append(TimedEventReceiver.TIMED_EVENT_RESOURCE_TYPE);
            buf.append(")[@");
            buf.append(ISO9075.encode(JobUtil.PROPERTY_JOB_CREATED));
            buf.append(" < xs:dateTime('");
            buf.append(ISO8601.format(startDate));
            buf.append("')] order by @");
            buf.append(ISO9075.encode(JobUtil.PROPERTY_JOB_CREATED));
            buf.append(" ascending");
            final Iterator<Resource> result = resolver.findResources(buf.toString(), "xpath");

            while ( result.hasNext() ) {
                final Resource eventResource = result.next();
                // sanity check for the path
                if ( eventResource.getPath().startsWith(this.config.getResourcePathWithSlash()) ) {
                    final ReadResult readResult = this.readEvent(eventResource);
                    if ( readResult != null ) {
                        if ( readResult.hasReadErrors ) {
                            synchronized ( this.unloadedEvents ) {
                                this.unloadedEvents.add(eventResource.getPath());
                            }
                        } else {
                            try {
                                this.queue.put(readResult.event);
                            } catch (final InterruptedException e) {
                                this.ignoreException(e);
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            }

        } catch (final QuerySyntaxException qse) {
            this.ignoreException(qse);
        } catch (final LoginException le) {
            this.ignoreException(le);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
    }

    private static final class ReadResult {
        public Event event;
        public boolean hasReadErrors;
    }

    /**
     * Read an event from the resource
     * @return The event or <code>null</code>
     */
    private ReadResult readEvent(final Resource eventResource) {
        if ( eventResource != null ) {
            try {
                final ValueMap vm = ResourceHelper.getValueMap(eventResource);
                final Map<String, Object> properties = ResourceHelper.cloneValueMap(vm);
                String topic = (String)properties.get(EventConstants.EVENT_TOPIC);
                if ( topic == null ) {
                    topic = (String)properties.remove("slingevent:topic");
                }
                final ReadResult result = new ReadResult();
                if ( topic == null ) {
                    logger.warn("Resource at {} does not look like a timed event: {}", eventResource.getPath(), properties);
                    result.hasReadErrors = true;
                    return result;
                }
                @SuppressWarnings("unchecked")
                final List<Exception> readErrorList = (List<Exception>) properties.remove(ResourceHelper.PROPERTY_MARKER_READ_ERROR_LIST);
                result.hasReadErrors = readErrorList != null;
                if ( readErrorList != null ) {
                    for(final Exception e : readErrorList) {
                        logger.warn("Unable to read timed event job from " + eventResource.getPath(), e);
                    }
                }
                properties.remove(EventConstants.EVENT_TOPIC);
                properties.put(TimedEventStatusProvider.PROPERTY_EVENT_ID, topic.replace('/', '.') + '/' + eventResource.getName());

                try {
                    result.event = new Event(topic, properties);
                    return result;
                } catch (final IllegalArgumentException iae) {
                    // this exception occurs if the topic is not correct (it should never happen,
                    // but you never know)
                    logger.error("Unable to read event: " + iae.getMessage(), iae);
                }
            } catch (final InstantiationException ie) {
                // something happened with the resource in the meanitime
                this.ignoreException(ie);
            }
        }
        return null;
    }

    /**
     * @see org.apache.sling.event.TimedEventStatusProvider#getScheduledEvent(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Event getScheduledEvent(final String topic, final String eventId, final String jobId) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final String scheduleId = ScheduleInfo.getJobId(topic, eventId, jobId);
            final StringBuilder sb = new StringBuilder(this.config.getResourcePathWithSlash());
            sb.append(topic.replace('/', '.'));
            sb.append('/');
            sb.append(scheduleId);
            final String path = sb.toString();

            final Resource eventResource = resolver.getResource(path);
            final ReadResult result = this.readEvent(eventResource);
            if ( result != null ) {
                return result.event;
            }
        } catch (final LoginException re) {
            this.logger.error("Unable to create a resource resolver.", re);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
        return null;
    }

    /**
     * @see org.apache.sling.event.TimedEventStatusProvider#getScheduledEvents(java.lang.String, java.util.Map...)
     */
    @Override
    public Collection<Event> getScheduledEvents(final String topic, final Map<String, Object>... filterProps) {
        final List<Event> result = new ArrayList<Event>();
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

            final StringBuilder buf = new StringBuilder(64);

            buf.append("//element(*,");
            buf.append(TimedEventReceiver.TIMED_EVENT_RESOURCE_TYPE);
            buf.append(")[@");
            buf.append(ISO9075.encode(EventConstants.EVENT_TOPIC));
            buf.append(" = '");
            buf.append(topic);
            buf.append("'");
            if ( filterProps != null && filterProps.length > 0 ) {
                buf.append(" and (");
                int index = 0;
                for (final Map<String,Object> template : filterProps) {
                    if ( index > 0 ) {
                        buf.append(" or ");
                    }
                    buf.append('(');
                    final Iterator<Map.Entry<String, Object>> i = template.entrySet().iterator();
                    boolean first = true;
                    while ( i.hasNext() ) {
                        final Map.Entry<String, Object> current = i.next();
                        final String propName = ISO9075.encode(current.getKey());
                        if ( first ) {
                            first = false;
                            buf.append('@');
                        } else {
                            buf.append(" and @");
                        }
                        buf.append(propName);
                        buf.append(" = '");
                        buf.append(current.getValue());
                        buf.append("'");
                    }
                    buf.append(')');
                    index++;
                }
                buf.append(')');
            }
            buf.append("]");
            if ( logger.isDebugEnabled() ) {
                logger.debug("Executing query {}", buf);
            }
            final Iterator<Resource> iter = resolver.findResources(buf.toString(), "xpath");
            while ( iter.hasNext() ) {
                final Resource eventResource = iter.next();
                if ( eventResource.getPath().startsWith(this.config.getResourcePathWithSlash()) ) {
                    final ReadResult readResult = this.readEvent(eventResource);
                    if ( readResult != null && readResult.event != null ) {
                        result.add(readResult.event);
                    }
                }
            }
        } catch (final QuerySyntaxException qse) {
            this.ignoreException(qse);
        } catch (final LoginException le) {
            this.ignoreException(le);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
        return result;
    }

    /**
     * @see org.apache.sling.event.TimedEventStatusProvider#cancelTimedEvent(java.lang.String)
     */
    @Override
    public void cancelTimedEvent(final String jobId) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final StringBuilder sb = new StringBuilder(this.config.getResourcePathWithSlash());
            sb.append(jobId);
            final String path = sb.toString();

            final Resource eventResource = resolver.getResource(path);
            if ( eventResource != null ) {
                resolver.delete(eventResource);
                resolver.commit();
            }
        } catch (final LoginException re) {
            this.logger.error("Unable to create a resource resolver.", re);
        } catch (final PersistenceException pe) {
            // we ignore the exception if removing fails
            ignoreException(pe);
        } finally {
            if ( resolver != null ) {
                resolver.close();
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

    /**
     * @see org.apache.sling.discovery.TopologyEventListener#handleTopologyEvent(org.apache.sling.discovery.TopologyEvent)
     */
    @Override
    public void handleTopologyEvent(final TopologyEvent event) {
        if ( event.getType() == Type.TOPOLOGY_CHANGING ) {
            this.active = false;
            this.stopScheduling();
        } else if ( event.getType() == Type.TOPOLOGY_CHANGED || event.getType() == Type.TOPOLOGY_INIT ) {
            final boolean previouslyActive = this.active;
            this.active = event.getNewView().getLocalInstance().isLeader();
            if ( this.active && !previouslyActive ) {
                this.startScheduling();
            }
            if ( !this.active && previouslyActive ) {
                this.stopScheduling();
            }
        }
    }
}
