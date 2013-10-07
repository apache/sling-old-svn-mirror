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
package org.apache.sling.event.impl.jobs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
import org.apache.sling.commons.scheduler.JobContext;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.impl.support.ScheduleInfo;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A scheduler for scheduling jobs
 *
 * TODO check handling of running and active flag
 */
public class JobSchedulerImpl
    implements EventHandler, TopologyEventListener, org.apache.sling.commons.scheduler.Job {

    /** We use the same resource type as for timed events. */
    private static final String SCHEDULED_JOB_RESOURCE_TYPE = "slingevent:TimedEvent";

    private static final String TOPIC_READ_JOB = "org/apache/sling/event/impl/jobs/READSCHEDULEDJOB";

    private static final String PROPERTY_READ_JOB = "properties";

    /** Default logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Is the background task still running? */
    private volatile boolean running;

    /** Is this active? */
    private volatile boolean active;

    private final ResourceResolverFactory resourceResolverFactory;

    private final JobManagerConfiguration config;

    private final Scheduler scheduler;

    private final JobManagerImpl jobManager;

    /** A local queue for serializing the event processing. */
    private final BlockingQueue<Event> queue = new LinkedBlockingQueue<Event>();

    /** Unloaded events. */
    private final Set<String>unloadedEvents = new HashSet<String>();

    private final Map<String, ScheduledJobInfoImpl> scheduledJobs = new HashMap<String, ScheduledJobInfoImpl>();

    public JobSchedulerImpl(final JobManagerConfiguration configuration,
            final ResourceResolverFactory resourceResolverFactory,
            final Scheduler scheduler,
            final JobManagerImpl jobManager) {
        this.config = configuration;
        this.resourceResolverFactory = resourceResolverFactory;
        this.scheduler = scheduler;
        this.running = true;
        this.jobManager = jobManager;
    }

    /**
     * Deactivate this component.
     */
    public void deactivate() {
        this.running = false;
        this.stopScheduling();
    }

    private void stopScheduling() {
        if ( this.active ) {
            final List<ScheduledJobInfoImpl> jobs = new ArrayList<ScheduledJobInfoImpl>();
            synchronized ( this.scheduledJobs ) {
                for(final ScheduledJobInfoImpl job : this.scheduledJobs.values() ) {
                    jobs.add(job);
                }
            }
            for(final ScheduledJobInfoImpl info : jobs) {
                try {
                    logger.debug("Stopping scheduled job : {}", info.getName());
                    this.scheduler.removeJob(info.getSchedulerJobId());
                } catch ( final NoSuchElementException nsee ) {
                    this.ignoreException(nsee);
                }
            }
        }
        synchronized ( this.scheduledJobs ) {
            this.scheduledJobs.clear();
        }

        // stop background threads by putting empty objects into the queue
        this.queue.clear();
        try {
            this.queue.put(new Event(Utility.TOPIC_STOPPED, (Dictionary<String, Object>)null));
        } catch (final InterruptedException e) {
            this.ignoreException(e);
        }
    }

    private void startScheduling() {
        final long now = System.currentTimeMillis();
        final Thread backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                loadScheduledJobs(now);
                try {
                    runInBackground();
                } catch (final Throwable t) { //NOSONAR
                    logger.error("Background thread stopped with exception: " + t.getMessage(), t);
                    running = false;
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
                    // we ignore this
                    this.ignoreException(e);
                }
            }
            if ( event != null && this.running ) {
                Event nextEvent = null;

                // check event type
                if ( event.getTopic().equals(TOPIC_READ_JOB) ) {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> properties = (Map<String, Object>) event.getProperty(PROPERTY_READ_JOB);
                    properties.remove(ResourceResolver.PROPERTY_RESOURCE_TYPE);
                    properties.remove(Job.PROPERTY_JOB_CREATED);
                    properties.remove(Job.PROPERTY_JOB_CREATED_INSTANCE);

                    final String jobTopic = (String) properties.remove(JobUtil.PROPERTY_JOB_TOPIC);
                    final String jobName = (String) properties.remove(JobUtil.PROPERTY_JOB_NAME);
                    final String schedulerName = (String) properties.remove(ResourceHelper.PROPERTY_SCHEDULER_NAME);
                    final ScheduleInfo scheduleInfo = (ScheduleInfo)  properties.remove(ResourceHelper.PROPERTY_SCHEDULER_INFO);

                    // and now schedule (TODO)
                    final ScheduledJobInfoImpl info = new ScheduledJobInfoImpl(this, jobTopic, jobName, properties, schedulerName, scheduleInfo);
                    synchronized ( this.scheduledJobs ) {
                        this.scheduledJobs.put(ResourceHelper.filterName(schedulerName), info);
                    }
                    if ( this.active ) {
                        this.startScheduledJob(info);
                    }
                }
                if ( event.getTopic().equals(SlingConstants.TOPIC_RESOURCE_ADDED)
                     || event.getTopic().equals(SlingConstants.TOPIC_RESOURCE_CHANGED)) {
                    final String path = (String)event.getProperty(SlingConstants.PROPERTY_PATH);
                    ResourceResolver resolver = null;
                    try {
                        resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
                        final Resource eventResource = resolver.getResource(path);
                        if ( ResourceHelper.RESOURCE_TYPE_SCHEDULED_JOB.equals(eventResource.getResourceType()) ) {
                            final ReadResult result = this.readScheduledJob(eventResource);
                            if ( result != null ) {
                                if ( result.hasReadErrors ) {
                                    synchronized ( this.unloadedEvents ) {
                                        this.unloadedEvents.add(eventResource.getPath());
                                    }
                                } else {
                                    nextEvent = result.event;
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
                    final String scheduleName = ResourceUtil.getName(path);
                    final ScheduledJobInfoImpl info;
                    synchronized ( this.scheduledJobs ) {
                        info = this.scheduledJobs.remove(scheduleName);
                    }
                    if ( info != null && this.active ) {
                        logger.debug("Stopping scheduled job : {}", info.getName());
                        try {
                            this.scheduler.removeJob(info.getSchedulerJobId());
                        } catch (final NoSuchElementException nsee) {
                            // this can happen if the job is scheduled on another node
                            // so we can just ignore this
                        }

                    }
                }
                event = nextEvent;
            }
        }
    }

    private void startScheduledJob(final ScheduledJobInfoImpl info) {
        // Create configuration for scheduled job
        final Map<String, Serializable> config = new HashMap<String, Serializable>();
        config.put(PROPERTY_READ_JOB, info);

        logger.debug("Adding scheduled job: {}", info.getName());
        try {
            switch ( info.getScheduleType() ) {
                case DAILY:
                    // TODO
                    break;
                case DATE:
                    this.scheduler.fireJobAt(info.getSchedulerJobId(), this, config, info.getNextScheduledExecution());
                    break;
                case PERIODICALLY:
                    this.scheduler.addPeriodicJob(info.getSchedulerJobId(), this, config, info.getPeriod() * 1000, false);
                    break;
                case WEEKLY:
                    // TODO
                    break;
                }
        } catch (final Exception e) {
            // we ignore it if scheduled fails...
            this.ignoreException(e);
        }
    }

    /**
     * @see org.apache.sling.commons.scheduler.Job#execute(org.apache.sling.commons.scheduler.JobContext)
     */
    @Override
    public void execute(final JobContext context) {
        final ScheduledJobInfoImpl info = (ScheduledJobInfoImpl) context.getConfiguration().get(PROPERTY_READ_JOB);

        this.jobManager.addJob(info.getJobTopic(), info.getJobName(), info.getJobProperties());

        // is this job scheduled for a specific date?
        if ( info.getScheduleType() == ScheduledJobInfo.ScheduleType.DATE ) {
            // we can remove it from the resource tree
            this.unschedule(info);
        }
    }

    public void unschedule(final ScheduledJobInfoImpl info) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final StringBuilder sb = new StringBuilder(this.config.getScheduledJobsPathWithSlash());
            sb.append('/');
            sb.append(ResourceHelper.filterName(info.getName()));
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
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        if ( this.running ) {
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
                                        final ReadResult result = readScheduledJob(eventResource);
                                        if ( result != null ) {
                                            if ( result.hasReadErrors ) {
                                                newUnloadedEvents.add(path);
                                            } else {
                                                try {
                                                    queue.put(result.event);
                                                } catch (InterruptedException e) {
                                                    // we ignore this exception as this should never occur
                                                    ignoreException(e);
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
                if ( path != null && path.startsWith(this.config.getScheduledJobsPathWithSlash())
                     && (resourceType == null || ResourceHelper.RESOURCE_TYPE_SCHEDULED_JOB.equals(resourceType))) {
                    logger.debug("Received resource event for {} : {}", path, resourceType);
                    try {
                        this.queue.put(event);
                    } catch (final InterruptedException ignore) {
                        this.ignoreException(ignore);
                    }
                }
            }
        }
    }

    /**
     * Load all scheduled jobs from the resource tree
     */
    private void loadScheduledJobs(final long startTime) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Calendar startDate = Calendar.getInstance();
            startDate.setTimeInMillis(startTime);

            final StringBuilder buf = new StringBuilder(64);

            buf.append("//element(*,");
            buf.append(SCHEDULED_JOB_RESOURCE_TYPE);
            buf.append(")[@");
            buf.append(ISO9075.encode(org.apache.sling.event.jobs.Job.PROPERTY_JOB_CREATED));
            buf.append(" < xs:dateTime('");
            buf.append(ISO8601.format(startDate));
            buf.append("')] order by @");
            buf.append(ISO9075.encode(org.apache.sling.event.jobs.Job.PROPERTY_JOB_CREATED));
            buf.append(" ascending");
            final Iterator<Resource> result = resolver.findResources(buf.toString(), "xpath");

            while ( result.hasNext() ) {
                final Resource eventResource = result.next();
                // sanity check for the path
                if ( eventResource.getPath().startsWith(this.config.getScheduledJobsPathWithSlash()) ) {
                    final ReadResult readResult = this.readScheduledJob(eventResource);
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
     * Read a scheduled job from the resource
     * @return The job or <code>null</code>
     */
    private ReadResult readScheduledJob(final Resource eventResource) {
        if ( eventResource != null ) {
            try {
                final ValueMap vm = ResourceHelper.getValueMap(eventResource);
                final Map<String, Object> properties = ResourceHelper.cloneValueMap(vm);
                final ReadResult result = new ReadResult();
                @SuppressWarnings("unchecked")
                final List<Exception> readErrorList = (List<Exception>) properties.remove(ResourceHelper.PROPERTY_MARKER_READ_ERROR_LIST);
                result.hasReadErrors = readErrorList != null;
                if ( readErrorList != null ) {
                    for(final Exception e : readErrorList) {
                        logger.warn("Unable to read scheduled job from " + eventResource.getPath(), e);
                    }
                }
                final Map<String, Object> eventProps = Collections.singletonMap(PROPERTY_READ_JOB, (Object)properties);
                result.event = new Event(TOPIC_READ_JOB, eventProps);

                return result;
            } catch (final InstantiationException ie) {
                // something happened with the resource in the meantime
                this.ignoreException(ie);
            }
        }
        return null;
    }

    /**
     * Write a schedule job to the resource tree.
     * @throws PersistenceException
     */
    public boolean writeJob(
            final String jobTopic,
            final String jobName,
            final Map<String, Object> jobProperties,
            final String schedulerName,
            final ScheduleInfo scheduleInfo)
    throws PersistenceException {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

            // create properties
            final Map<String, Object> properties = new HashMap<String, Object>();

            if ( jobProperties != null ) {
                for(final Map.Entry<String, Object> entry : jobProperties.entrySet() ) {
                    final String propName = entry.getKey();
                    if ( !ResourceHelper.ignoreProperty(propName) ) {
                        properties.put(propName, entry.getValue());
                    }
                }
            }

            properties.put(JobUtil.PROPERTY_JOB_TOPIC, jobTopic);
            if ( jobName != null ) {
                properties.put(JobUtil.PROPERTY_JOB_NAME, jobName);
            }
            properties.put(Job.PROPERTY_JOB_CREATED, Calendar.getInstance());
            properties.put(Job.PROPERTY_JOB_CREATED_INSTANCE, Environment.APPLICATION_ID);

            // put scheduler name and scheduler info
            properties.put(ResourceHelper.PROPERTY_SCHEDULER_NAME, schedulerName);
            properties.put(ResourceHelper.PROPERTY_SCHEDULER_INFO, scheduleInfo);

            // create path and resource
            properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, ResourceHelper.RESOURCE_TYPE_SCHEDULED_JOB);

            final String path = this.config.getScheduledJobsPathWithSlash() + ResourceHelper.filterName(schedulerName);

            // update existing resource
            final Resource existingInfo = resolver.getResource(path);
            if ( existingInfo != null ) {
                resolver.delete(existingInfo);
                if ( logger.isDebugEnabled() ) {
                    logger.debug("Updating scheduled job {} at {}", properties, path);
                }
            } else {
                if ( logger.isDebugEnabled() ) {
                    logger.debug("Storing new scheduled job {} at {}", properties, path);
                }
            }
            ResourceHelper.getOrCreateResource(resolver,
                    path,
                    properties);
            return true;
        } catch ( final LoginException le ) {
            // we ignore this
            this.ignoreException(le);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
        return false;
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

    public JobBuilder.ScheduleBuilder createJobBuilder(final ScheduledJobInfoImpl info) {
        final JobBuilder builder = this.jobManager.createJob(info.getJobTopic()).name(info.getJobTopic()).properties(info.getJobProperties());
        return builder.schedule(info.getName());
    }
}
