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
package org.apache.sling.event.impl.jobs.scheduling;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.scheduler.JobContext;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.event.impl.jobs.JobManagerImpl;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.jobs.config.ConfigurationChangeListener;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.config.TopologyCapabilities;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.impl.support.ScheduleInfoImpl;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.ScheduleInfo;
import org.apache.sling.event.jobs.ScheduleInfo.ScheduleType;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The scheduler for managing scheduled jobs.
 *
 * This is not a component by itself, it's directly created from the job manager.
 * The job manager is also registering itself as an event handler and forwards
 * the events to this service.
 */
public class JobSchedulerImpl
    implements EventHandler,
               ConfigurationChangeListener,
               org.apache.sling.commons.scheduler.Job {

    private static final String PROPERTY_READ_JOB = "properties";

    private static final String PROPERTY_SCHEDULE_INDEX = "index";

    /** Default logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Is this active? */
    private final AtomicBoolean active = new AtomicBoolean(false);

    /** Central job handling configuration. */
    private final JobManagerConfiguration configuration;

    /** Scheduler service. */
    private final Scheduler scheduler;

    /** Job manager. */
    private final JobManagerImpl jobManager;

    /** Scheduled job handler. */
    private final ScheduledJobHandler scheduledJobHandler;

    /** All scheduled jobs, by scheduler name */
    private final Map<String, ScheduledJobInfoImpl> scheduledJobs = new HashMap<String, ScheduledJobInfoImpl>();

    /**
     * Create the scheduler
     * @param configuration Central job manager configuration
     * @param scheduler The scheduler service
     * @param jobManager The job manager
     */
    public JobSchedulerImpl(final JobManagerConfiguration configuration,
            final Scheduler scheduler,
            final JobManagerImpl jobManager) {
        this.configuration = configuration;
        this.scheduler = scheduler;
        this.jobManager = jobManager;

        this.configuration.addListener(this);

        this.scheduledJobHandler = new ScheduledJobHandler(configuration, this);
    }

    /**
     * Deactivate this component.
     */
    public void deactivate() {
        this.configuration.removeListener(this);

        this.scheduledJobHandler.deactivate();

        if ( this.active.compareAndSet(true, false) ) {
            this.stopScheduling();
        }
        synchronized ( this.scheduledJobs ) {
            this.scheduledJobs.clear();
        }
    }

    /**
     * @see org.apache.sling.event.impl.jobs.config.ConfigurationChangeListener#configurationChanged(boolean)
     */
    @Override
    public void configurationChanged(final boolean processingActive) {
        // scheduling is only active if
        // - processing is active and
        // - configuration is still available and active
        // - and current instance is leader
        final boolean schedulingActive;
        if ( processingActive ) {
            final TopologyCapabilities caps = this.configuration.getTopologyCapabilities();
            if ( caps != null && caps.isActive() ) {
                schedulingActive = caps.isLeader();
            } else {
                schedulingActive = false;
            }
        } else {
            schedulingActive = false;
        }

        // switch activation based on current state and new state
        if ( schedulingActive ) {
            // activate if inactive
            if ( this.active.compareAndSet(false, true) ) {
                this.startScheduling();
            }
        } else {
            // deactivate if active
            if ( this.active.compareAndSet(true, false) ) {
                this.stopScheduling();
            }
        }
    }

    /**
     * Start all scheduled jobs
     */
    private void startScheduling() {
        synchronized ( this.scheduledJobs ) {
            for(final ScheduledJobInfo info : this.scheduledJobs.values()) {
                this.startScheduledJob(((ScheduledJobInfoImpl)info));
            }
        }
    }

    /**
     * Stop all scheduled jobs.
     */
    private void stopScheduling() {
        synchronized ( this.scheduledJobs ) {
            for(final ScheduledJobInfo info : this.scheduledJobs.values()) {
                this.stopScheduledJob((ScheduledJobInfoImpl)info);
            }
        }
    }

    /**
     * Add a scheduled job
     */
    public void scheduleJob(final ScheduledJobInfoImpl info) {
        synchronized ( this.scheduledJobs ) {
            this.scheduledJobs.put(info.getName(), info);
            this.startScheduledJob(info);
        }
    }

    /**
     * Unschedule a scheduled job
     */
    public void unscheduleJob(final ScheduledJobInfoImpl info) {
        synchronized ( this.scheduledJobs ) {
            if ( this.scheduledJobs.remove(info.getName()) != null ) {
                this.stopScheduledJob(info);
            }
        }
    }

    /**
     * Remove a scheduled job
     */
    public void removeJob(final ScheduledJobInfoImpl info) {
        this.unscheduleJob(info);
        this.scheduledJobHandler.remove(info);
    }

    /**
     * Start a scheduled job
     * @param info The scheduling info
     */
    private void startScheduledJob(final ScheduledJobInfoImpl info) {
        if ( this.active.get() ) {
            if ( !info.isSuspended() ) {
                this.configuration.getAuditLogger().debug("SCHEDULED OK name={}, topic={}, properties={} : {}",
                        new Object[] {info.getName(),
                                      info.getJobTopic(),
                                      info.getJobProperties()},
                                      info.getSchedules());
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
                    // Create configuration for scheduled job
                    final Map<String, Serializable> config = new HashMap<String, Serializable>();
                    config.put(PROPERTY_READ_JOB, info);
                    config.put(PROPERTY_SCHEDULE_INDEX, index);
                    this.scheduler.schedule(this, options.name(name).config(config).canRunConcurrently(false));
                    index++;
                }
            } else {
                this.configuration.getAuditLogger().debug("SCHEDULED SUSPENDED name={}, topic={}, properties={} : {}",
                        new Object[] {info.getName(),
                                      info.getJobTopic(),
                                      info.getJobProperties(),
                                      info.getSchedules()});
            }
        }
    }

    /**
     * Stop a scheduled job
     * @param info The scheduling info
     */
    private void stopScheduledJob(final ScheduledJobInfoImpl info) {
        if ( this.active.get() ) {
            this.configuration.getAuditLogger().debug("SCHEDULED STOP name={}, topic={}, properties={} : {}",
                    new Object[] {info.getName(),
                                  info.getJobTopic(),
                                  info.getJobProperties(),
                                  info.getSchedules()});
            for(int index = 0; index<info.getSchedules().size(); index++) {
                final String name = info.getSchedulerJobId() + "-" + String.valueOf(index);
                this.scheduler.unschedule(name);
            }
        }
    }

    /**
     * @see org.apache.sling.commons.scheduler.Job#execute(org.apache.sling.commons.scheduler.JobContext)
     */
    @Override
    public void execute(final JobContext context) {
        final ScheduledJobInfoImpl info = (ScheduledJobInfoImpl) context.getConfiguration().get(PROPERTY_READ_JOB);

        if ( info.isSuspended() ) {
            return;
        }

        this.jobManager.addJob(info.getJobTopic(), info.getJobProperties());
        final int index = (Integer)context.getConfiguration().get(PROPERTY_SCHEDULE_INDEX);
        final Iterator<ScheduleInfo> iter = info.getSchedules().iterator();
        ScheduleInfo si = iter.next();
        for(int i=0; i<index; i++) {
            si = iter.next();
        }
        // if scheduled once (DATE), remove from schedule
        if ( si.getType() == ScheduleType.DATE ) {
            if ( index == 0 && info.getSchedules().size() == 1 ) {
                // remove
                this.scheduledJobHandler.remove(info);
            } else {
                // update schedule list
                final List<ScheduleInfo> infos = new ArrayList<ScheduleInfo>();
                for(final ScheduleInfo i : info.getSchedules() ) {
                    if ( i != si ) { // no need to use equals
                        infos.add(i);
                    }
                }
                info.update(infos);
                this.scheduledJobHandler.updateSchedule(info.getName(), infos);
            }
        }
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        if ( ResourceHelper.BUNDLE_EVENT_STARTED.equals(event.getTopic())
             || ResourceHelper.BUNDLE_EVENT_UPDATED.equals(event.getTopic()) ) {
            this.scheduledJobHandler.bundleEvent();
        } else {
            // resource event
            final String path = (String)event.getProperty(SlingConstants.PROPERTY_PATH);
            if ( path != null && path.startsWith(this.configuration.getScheduledJobsPath(true)) ) {
                if ( SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic()) ) {
                    // removal
                    this.scheduledJobHandler.handleRemove(path);
                } else {
                    // add or update
                    this.scheduledJobHandler.handleAddUpdate(path);
                }
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
     * Create a schedule builder for a currently scheduled job
     */
    public JobBuilder.ScheduleBuilder createJobBuilder(final ScheduledJobInfoImpl info) {
        final JobBuilder.ScheduleBuilder sb = new JobScheduleBuilderImpl(info.getJobTopic(),
                info.getJobProperties(), info.getName(), this);
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
                        if ( op == Operation.LESS && result > -1 ) {
                            return false;
                        } else if ( op == Operation.LESS_OR_EQUALS && result > 0 ) {
                            return false;
                        } else if ( op == Operation.GREATER_OR_EQUALS && result < 0 ) {
                            return false;
                        } else if ( op == Operation.GREATER && result < 1 ) {
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

    /**
     * Change the suspended flag for a scheduled job
     * @param info The schedule info
     * @param flag The corresponding flag
     */
    public void setSuspended(final ScheduledJobInfoImpl info, final boolean flag) {
        final ResourceResolver resolver = configuration.createResourceResolver();
        try {
            final StringBuilder sb = new StringBuilder(this.configuration.getScheduledJobsPath(true));
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
            if ( flag ) {
                this.stopScheduledJob(info);
            } else {
                this.startScheduledJob(info);
            }
        } catch (final PersistenceException pe) {
            // we ignore the exception if removing fails
            ignoreException(pe);
        } finally {
            resolver.close();
        }
    }

    /**
     * Add a scheduled job
     * @param topic The job topic
     * @param properties The job properties
     * @param scheduleName The schedule name
     * @param isSuspended Whether it is suspended
     * @param scheduleInfos The scheduling information
     * @param errors Optional list to contain potential errors
     * @return A new job info or {@code null}
     */
    public ScheduledJobInfo addScheduledJob(final String topic,
            final Map<String, Object> properties,
            final String scheduleName,
            final boolean isSuspended,
            final List<ScheduleInfoImpl> scheduleInfos,
            final List<String> errors) {
        final List<String> msgs = new ArrayList<String>();
        if ( scheduleName == null || scheduleName.length() == 0 ) {
            msgs.add("Schedule name not specified");
        }
        final String errorMessage = Utility.checkJob(topic, properties);
        if ( errorMessage != null ) {
            msgs.add(errorMessage);
        }
        if ( scheduleInfos.size() == 0 ) {
            msgs.add("No schedule defined for " + scheduleName);
        }
        for(final ScheduleInfoImpl info : scheduleInfos) {
            info.check(msgs);
        }
        if ( msgs.size() == 0 ) {
            try {
                final ScheduledJobInfo info = this.scheduledJobHandler.addOrUpdateJob(topic, properties, scheduleName, isSuspended, scheduleInfos);
                if ( info != null ) {
                    return info;
                }
                msgs.add("Unable to persist scheduled job.");
            } catch ( final PersistenceException pe) {
                msgs.add("Unable to persist scheduled job: " + scheduleName);
                logger.warn("Unable to persist scheduled job", pe);
            }
        } else {
            for(final String msg : msgs) {
                logger.warn(msg);
            }
        }
        if ( errors != null ) {
            errors.addAll(msgs);
        }
        return null;
    }

    public void maintenance() {
        this.scheduledJobHandler.maintenance();
    }
}
