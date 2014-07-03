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
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.scheduler.JobContext;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.impl.support.ScheduleInfoImpl;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.ScheduleInfo;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A scheduler for scheduling jobs
 *
 */
public class JobSchedulerImpl
    implements EventHandler, TopologyEventListener, org.apache.sling.commons.scheduler.Job {

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
     * Deactivate this component.
     */
    public void deactivate() {
        this.running = false;
        this.stopScheduling();
        synchronized ( this.scheduledJobs ) {
            this.scheduledJobs.clear();
        }

        // stop background threads by putting empty objects into the queue
        this.queue.clear();
        try {
            this.queue.put(new Event(Utility.TOPIC_STOPPED, (Dictionary<String, Object>)null));
        } catch (final InterruptedException e) {
            this.ignoreException(e);
            Thread.currentThread().interrupt();
        }
    }

    private void stopScheduling() {
        if ( this.active ) {
            final Collection<ScheduledJobInfo> jobs = this.getScheduledJobs(null, -1, (Map<String, Object>[])null);
            for(final ScheduledJobInfo info : jobs) {
                this.stopScheduledJob((ScheduledJobInfoImpl)info);
            }
        }
    }

    private void startScheduling() {
        if ( this.active ) {
            final Collection<ScheduledJobInfo> jobs = this.getScheduledJobs(null, -1, (Map<String, Object>[])null);
            for(final ScheduledJobInfo info : jobs) {
                this.startScheduledJob(((ScheduledJobInfoImpl)info));
            }
        }
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
                Event nextEvent = null;

                // check event type
                if ( event.getTopic().equals(TOPIC_READ_JOB) ) {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> properties = (Map<String, Object>) event.getProperty(PROPERTY_READ_JOB);
                    final ScheduledJobInfoImpl info = this.addOrUpdateScheduledJob(properties);

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
                        this.stopScheduledJob(info);
                    }
                }
                event = nextEvent;
            }
        }
    }

    private ScheduledJobInfoImpl addOrUpdateScheduledJob(final Map<String, Object> properties) {
        properties.remove(ResourceResolver.PROPERTY_RESOURCE_TYPE);
        properties.remove(Job.PROPERTY_JOB_CREATED);
        properties.remove(Job.PROPERTY_JOB_CREATED_INSTANCE);

        final String jobTopic = (String) properties.remove(ResourceHelper.PROPERTY_JOB_TOPIC);
        final String schedulerName = (String) properties.remove(ResourceHelper.PROPERTY_SCHEDULE_NAME);
        @SuppressWarnings("unchecked")
        final List<ScheduleInfo> scheduleInfos = (List<ScheduleInfo>) properties.remove(ResourceHelper.PROPERTY_SCHEDULE_INFO);
        final boolean isSuspended = properties.remove(ResourceHelper.PROPERTY_SCHEDULE_SUSPENDED) != null;
        // and now schedule
        final String key = ResourceHelper.filterName(schedulerName);
        ScheduledJobInfoImpl info;
        synchronized ( this.scheduledJobs ) {
            info = this.scheduledJobs.get(key);
            if ( info == null ) {
                info = new ScheduledJobInfoImpl(this, jobTopic,
                        properties, schedulerName);
                this.scheduledJobs.put(key, info);
            }
            info.update(isSuspended, scheduleInfos);
        }
        return info;
    }

    private void startScheduledJob(final ScheduledJobInfoImpl info) {
        if ( !info.isSuspended() ) {
            // Create configuration for scheduled job
            final Map<String, Serializable> config = new HashMap<String, Serializable>();
            config.put(PROPERTY_READ_JOB, info);

            logger.debug("Adding scheduled job: {}", info.getName());
            int index = 0;
            for(final ScheduleInfo si : info.getSchedules()) {
                final String name = info.getSchedulerJobId() + "-" + String.valueOf(index);
                ScheduleOptions options = null;
                switch ( si.getType() ) {
                    case DAILY:
                    case WEEKLY:
                    case HOURLY:
                    case MONTHLY:
                    case YEARLY:
                    case CRON:
                        options = this.scheduler.EXPR(((ScheduleInfoImpl)si).getCronExpression());

                        break;
                    case DATE:
                        options = this.scheduler.AT(((ScheduleInfoImpl)si).getNextScheduledExecution());
                        break;
                }
                this.scheduler.schedule(this, options.name(name).config(config).canRunConcurrently(false));
                index++;
            }
        }
    }

    private void stopScheduledJob(final ScheduledJobInfoImpl info) {
        logger.debug("Stopping scheduled job : {}", info.getName());
        for(int index = 0; index<info.getSchedules().size(); index++) {
            final String name = info.getSchedulerJobId() + "-" + String.valueOf(index);
            this.scheduler.unschedule(name);
        }
    }

    /**
     * @see org.apache.sling.commons.scheduler.Job#execute(org.apache.sling.commons.scheduler.JobContext)
     */
    @Override
    public void execute(final JobContext context) {
        final ScheduledJobInfoImpl info = (ScheduledJobInfoImpl) context.getConfiguration().get(PROPERTY_READ_JOB);

        this.jobManager.addJob(info.getJobTopic(), info.getJobProperties());
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
                                                    ignoreException(e);
                                                    Thread.currentThread().interrupt();
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
                        Thread.currentThread().interrupt();
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
            buf.append(ResourceHelper.RESOURCE_TYPE_SCHEDULED_JOB);
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
                                Thread.currentThread().interrupt();
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
    public ScheduledJobInfoImpl writeJob(
            final String jobTopic,
            final String jobName,
            final Map<String, Object> jobProperties,
            final String scheduleName,
            final boolean suspend,
            final List<ScheduleInfoImpl> scheduleInfos)
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

            properties.put(ResourceHelper.PROPERTY_JOB_TOPIC, jobTopic);
            if ( jobName != null ) {
                properties.put(ResourceHelper.PROPERTY_JOB_NAME, jobName);
            }
            properties.put(Job.PROPERTY_JOB_CREATED, Calendar.getInstance());
            properties.put(Job.PROPERTY_JOB_CREATED_INSTANCE, Environment.APPLICATION_ID);

            // put scheduler name and scheduler info
            properties.put(ResourceHelper.PROPERTY_SCHEDULE_NAME, scheduleName);
            final String[] infoArray = new String[scheduleInfos.size()];
            int index = 0;
            for(final ScheduleInfoImpl info : scheduleInfos) {
                infoArray[index] = info.getSerializedString();
                index++;
            }
            properties.put(ResourceHelper.PROPERTY_SCHEDULE_INFO, infoArray);
            if ( suspend ) {
                properties.put(ResourceHelper.PROPERTY_SCHEDULE_SUSPENDED, Boolean.TRUE);
            }

            // create path and resource
            properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, ResourceHelper.RESOURCE_TYPE_SCHEDULED_JOB);

            final String path = this.config.getScheduledJobsPathWithSlash() + ResourceHelper.filterName(scheduleName);

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
            // put back real schedule infos
            properties.put(ResourceHelper.PROPERTY_SCHEDULE_INFO, scheduleInfos);

            return this.addOrUpdateScheduledJob(properties);
        } catch ( final LoginException le ) {
            // we ignore this
            this.ignoreException(le);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
        return null;
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

    /**
     * Create a schedule builder for a currently scheduled job
     */
    public JobBuilder.ScheduleBuilder createJobBuilder(final ScheduledJobInfoImpl info) {
        final JobBuilderImpl builder = (JobBuilderImpl)this.jobManager.createJob(info.getJobTopic()).properties(info.getJobProperties());
        final JobBuilder.ScheduleBuilder sb = builder.schedule(info.getName());
        return (info.isSuspended() ? sb.suspend() : sb);
    }

    private enum Operation {
        LESS,
        LESS_OR_EQUALS,
        EQUALS,
        GREATER_OR_EQUALS,
        GREATER
    }

    /**
     * Check if the job matches the template
     */
    private boolean match(final ScheduledJobInfoImpl job, final Map<String, Object> template) {
        if ( template != null ) {
            for(final Map.Entry<String, Object> current : template.entrySet()) {
                final String key = current.getKey();
                final char firstChar = key.length() > 0 ? key.charAt(0) : 0;
                final String propName;
                final Operation op;
                if ( firstChar == '=' ) {
                    propName = key.substring(1);
                    op  = Operation.EQUALS;
                } else if ( firstChar == '<' ) {
                    final char secondChar = key.length() > 1 ? key.charAt(1) : 0;
                    if ( secondChar == '=' ) {
                        op = Operation.LESS_OR_EQUALS;
                        propName = key.substring(2);
                    } else {
                        op = Operation.LESS;
                        propName = key.substring(1);
                    }
                } else if ( firstChar == '>' ) {
                    final char secondChar = key.length() > 1 ? key.charAt(1) : 0;
                    if ( secondChar == '=' ) {
                        op = Operation.GREATER_OR_EQUALS;
                        propName = key.substring(2);
                    } else {
                        op = Operation.GREATER;
                        propName = key.substring(1);
                    }
                } else {
                    propName = key;
                    op  = Operation.EQUALS;
                }
                final Object value = current.getValue();

                if ( op == Operation.EQUALS ) {
                    if ( !value.equals(job.getJobProperties().get(propName)) ) {
                        return false;
                    }
                } else {
                    if ( value instanceof Comparable ) {
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        final int result = ((Comparable)value).compareTo(job.getJobProperties().get(propName));
                        if ( op == Operation.LESS && result != -1 ) {
                            return false;
                        } else if ( op == Operation.LESS_OR_EQUALS && result == 1 ) {
                            return false;
                        } else if ( op == Operation.GREATER_OR_EQUALS && result == -1 ) {
                            return false;
                        } else if ( op == Operation.GREATER && result != 1 ) {
                            return false;
                        }
                    } else {
                        // if the value is not comparable we simply don't match
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Get all scheduled jobs
     */
    public Collection<ScheduledJobInfo> getScheduledJobs(final String topic,
            final long limit,
            final Map<String, Object>... templates) {
        final List<ScheduledJobInfo> jobs = new ArrayList<ScheduledJobInfo>();
        long count = 0;
        synchronized ( this.scheduledJobs ) {
            for(final ScheduledJobInfoImpl job : this.scheduledJobs.values() ) {
                boolean add = true;
                if ( topic != null && !topic.equals(job.getJobTopic()) ) {
                    add = false;
                }
                if ( add && templates != null && templates.length != 0 ) {
                    add = false;
                    for (Map<String,Object> template : templates) {
                        add = this.match(job, template);
                        if ( add ) {
                            break;
                        }
                    }
                }
                if ( add ) {
                    jobs.add(job);
                    count++;
                    if ( limit > 0 && count == limit ) {
                        break;
                    }
                }
            }
        }
        return jobs;
    }

    public void setSuspended(final ScheduledJobInfoImpl info, final boolean flag) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final StringBuilder sb = new StringBuilder(this.config.getScheduledJobsPathWithSlash());
            sb.append('/');
            sb.append(ResourceHelper.filterName(info.getName()));
            final String path = sb.toString();

            final Resource eventResource = resolver.getResource(path);
            if ( eventResource != null ) {
                final ModifiableValueMap mvm = eventResource.adaptTo(ModifiableValueMap.class);
                if ( flag ) {
                    mvm.put(ResourceHelper.PROPERTY_SCHEDULE_SUSPENDED, Boolean.TRUE);
                } else {
                    mvm.remove(ResourceHelper.PROPERTY_SCHEDULE_SUSPENDED);
                }
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
}
