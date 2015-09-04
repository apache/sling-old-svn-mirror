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

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.impl.support.ScheduleInfoImpl;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.ScheduleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ScheduledJobHandler implements Runnable {

    public static final class Holder {
        public Calendar created;
        public ScheduledJobInfoImpl info;
        public long read;
    }

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The job manager configuration. */
    private final JobManagerConfiguration configuration;

    /** The job scheduler. */
    private final JobSchedulerImpl jobScheduler;

    /** The map of all scheduled jobs, key is the filtered schedule name */
    private final Map<String, Holder> scheduledJobs = new HashMap<String, Holder>();

    private final AtomicLong lastBundleActivity = new AtomicLong();

    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    /** A local queue for serializing the event processing. */
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

    /**
     * @param configuration Current job manager configuration
     */
    public ScheduledJobHandler(final JobManagerConfiguration configuration,
            final JobSchedulerImpl jobScheduler) {
        this.configuration = configuration;
        this.jobScheduler = jobScheduler;
        final Thread t = new Thread(this, "Apache Sling Scheduled Job Handler Thread");
        t.setDaemon(true);
        t.start();

        this.addFullScan();
    }

    /**
     * Add a task/runnable to the queue
     */
    private void addTask(final Runnable r) {
        try {
            this.queue.put(r);
        } catch (final InterruptedException e) {
            this.ignoreException(e);
            Thread.currentThread().interrupt();
        }
    }
    /**
     * Add a full scan to the task queue
     */
    private void addFullScan() {
        this.addTask(new Runnable() {
            @Override
            public void run() {
                scan();
            }
        });
    }

    public void deactivate() {
        this.isRunning.set(false);
        this.queue.clear();
        // put a NOP runnable to wake up the queue
        this.addTask(new Runnable() {
            @Override
            public  void run() {
                // do nothing
            }
        });
    }

    @Override
    public void run() {
        while ( this.isRunning.get() ) {
            Runnable r = null;
            try {
                r = this.queue.take();
            } catch (final InterruptedException e) {
                this.ignoreException(e);
                Thread.currentThread().interrupt();
                this.isRunning.set(false);
            }
            if ( this.isRunning.get() && r != null) {
                r.run();
            }
        }
    }

    private void scan() {
        final ResourceResolver resolver = configuration.createResourceResolver();
        if ( resolver != null ) {
            try {
                logger.debug("Scanning for scheduled jobs...");
                final String path = this.configuration.getScheduledJobsPath(false);
                final Resource startResource = resolver.getResource(path);
                if ( startResource != null ) {
                    final Map<String, Holder> newScheduledJobs = new HashMap<String, Holder>();
                    synchronized ( this.scheduledJobs ) {
                        for(final Resource rsrc : startResource.getChildren()) {
                            if ( !isRunning.get() ) {
                                break;
                            }
                            handleAddOrUpdate(newScheduledJobs, rsrc);
                        }
                        if ( isRunning.get() ) {
                            for(final Holder h : this.scheduledJobs.values()) {
                                if ( h.info != null ) {
                                    this.jobScheduler.unscheduleJob(h.info);
                                }
                            }
                            this.scheduledJobs.clear();
                            this.scheduledJobs.putAll(newScheduledJobs);
                        }
                    }
                }
                logger.debug("Finished scanning for scheduled jobs...");
            } finally {
                resolver.close();
            }
        }
    }

    /**
     * Read a scheduled job from the resource
     * @return The job or <code>null</code>
     */
    private Map<String, Object> readScheduledJob(final Resource eventResource) {
        try {
            final ValueMap vm = ResourceHelper.getValueMap(eventResource);
            final Map<String, Object> properties = ResourceHelper.cloneValueMap(vm);

            @SuppressWarnings("unchecked")
            final List<Exception> readErrorList = (List<Exception>) properties.remove(ResourceHelper.PROPERTY_MARKER_READ_ERROR_LIST);
            if ( readErrorList != null ) {
                for(final Exception e : readErrorList) {
                    logger.warn("Unable to read scheduled job from " + eventResource.getPath(), e);
                }
            } else {
                return properties;
            }
        } catch (final InstantiationException ie) {
            // something happened with the resource in the meantime
            this.ignoreException(ie);
        }
        return null;
    }

    /**
     * Write a scheduled job to the resource tree.
     * @throws PersistenceException
     */
    public ScheduledJobInfoImpl addOrUpdateJob(
            final String jobTopic,
            final Map<String, Object> jobProperties,
            final String scheduleName,
            final boolean suspend,
            final List<ScheduleInfoImpl> scheduleInfos)
    throws PersistenceException {
        final Map<String, Object> properties = this.writeScheduledJob(jobTopic, jobProperties, scheduleName, suspend, scheduleInfos);

        final String key = ResourceHelper.filterName(scheduleName);
        synchronized ( this.scheduledJobs ) {
            final Holder h = this.scheduledJobs.remove(key);
            if ( h != null && h.info != null ) {
                this.jobScheduler.unscheduleJob(h.info);
            }
            final Holder holder = new Holder();
            holder.created = (Calendar) properties.get(Job.PROPERTY_JOB_CREATED);
            holder.read = System.currentTimeMillis();
            holder.info = this.addOrUpdateScheduledJob(properties, h == null ? null : h.info);

            this.jobScheduler.scheduleJob(holder.info);
            return holder.info;
        }
    }

    private Map<String, Object> writeScheduledJob(final String jobTopic,
            final Map<String, Object> jobProperties,
            final String scheduleName,
            final boolean suspend,
            final List<ScheduleInfoImpl> scheduleInfos)
    throws PersistenceException {
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
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

            final String path = this.configuration.getScheduledJobsPath(true) + ResourceHelper.filterName(scheduleName);

            // update existing resource
            final Resource existingInfo = resolver.getResource(path);
            if ( existingInfo != null ) {
                resolver.delete(existingInfo);
                logger.debug("Updating scheduled job {} at {}", properties, path);
            } else {
                logger.debug("Storing new scheduled job {} at {}", properties, path);
            }
            ResourceHelper.getOrCreateResource(resolver,
                    path,
                    properties);
            // put back real schedule infos
            properties.put(ResourceHelper.PROPERTY_SCHEDULE_INFO, scheduleInfos);

            return properties;
        } finally {
            resolver.close();
        }
    }

    private ScheduledJobInfoImpl addOrUpdateScheduledJob(
            final Map<String, Object> properties,
            final ScheduledJobInfoImpl oldInfo) {
        properties.remove(ResourceResolver.PROPERTY_RESOURCE_TYPE);
        properties.remove(Job.PROPERTY_JOB_CREATED);
        properties.remove(Job.PROPERTY_JOB_CREATED_INSTANCE);

        final String jobTopic = (String) properties.remove(ResourceHelper.PROPERTY_JOB_TOPIC);
        final String schedulerName = (String) properties.remove(ResourceHelper.PROPERTY_SCHEDULE_NAME);

        final ScheduledJobInfoImpl info;
        if ( oldInfo == null ) {
            info = new ScheduledJobInfoImpl(jobScheduler, schedulerName);
        } else {
            info = oldInfo;
        }
        info.update(jobTopic, properties);

        return info;
    }

    /**
     * A bundle event occurred which means we can try loading jobs that previously
     * failed because of missing classes.
     */
    public void bundleEvent() {
        this.lastBundleActivity.set(System.currentTimeMillis());
        this.addTask(new Runnable() {
            @Override
            public void run() {
                final Map<String, Holder> updateJobs = new HashMap<String, ScheduledJobHandler.Holder>();
                synchronized ( scheduledJobs ) {
                    for(final Map.Entry<String, Holder> entry : scheduledJobs.entrySet()) {
                        if ( entry.getValue().info == null && entry.getValue().read < lastBundleActivity.get() ) {
                            updateJobs.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
                if ( !updateJobs.isEmpty() && isRunning.get() ) {
                    ResourceResolver resolver = configuration.createResourceResolver();
                    if ( resolver != null ) {
                        try {
                            for(final Map.Entry<String, Holder> entry : updateJobs.entrySet()) {
                                final String path = configuration.getScheduledJobsPath(true) + entry.getKey();
                                final Resource rsrc = resolver.getResource(path);
                                if ( !isRunning.get() ) {
                                    break;
                                }
                                if ( rsrc != null ) {
                                    synchronized ( scheduledJobs ) {
                                        handleAddOrUpdate(scheduledJobs, rsrc);
                                    }
                                }
                            }
                        } finally {
                            resolver.close();
                        }
                    }
                }
            }
        });
    }

    /**
     * Handle observation event for removing a scheduled job
     * @param path The path to the job
     */
    public void handleRemove(final String path) {
        this.addTask(new Runnable() {
            @Override
            public void run() {
                if ( isRunning.get() ) {
                    final String scheduleKey = ResourceHelper.filterName(ResourceUtil.getName(path));
                    if ( scheduleKey != null ) {
                        synchronized ( scheduledJobs ) {
                            final Holder h = scheduledJobs.remove(scheduleKey);
                            if ( h != null && h.info != null ) {
                                jobScheduler.unscheduleJob(h.info);
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Handle observation event for adding or updating a scheduled job
     * @param path The path to the job
     */
    public void handleAddUpdate(final String path) {
        this.addTask(new Runnable() {
            @Override
            public void run() {
                if ( isRunning.get() ) {
                    final ResourceResolver resolver = configuration.createResourceResolver();
                    if ( resolver != null ) {
                        try {
                            final Resource rsrc = resolver.getResource(path);
                            if ( rsrc != null ) {
                                synchronized ( scheduledJobs ) {
                                    handleAddOrUpdate(scheduledJobs, rsrc);
                                }
                            }
                        } finally {
                            resolver.close();
                        }
                    }
                }
            }
        });
    }

    /**
     * Handle add or update of a resource
     * @param newScheduledJobs The map to store the jobs
     * @param rsrc The resource containing the job
     */
    private void handleAddOrUpdate(final Map<String, Holder> newScheduledJobs, final Resource rsrc) {
        final String id = ResourceHelper.filterName(rsrc.getName());
        final Holder scheduled = this.scheduledJobs.remove(id);
        boolean read = false;
        if ( scheduled != null ) {
            // check if loading failed and we can retry
            if ( scheduled.info == null || scheduled.read < this.lastBundleActivity.get() ) {
                read = true;
            }
            // check if this is an update
            if ( scheduled.info != null ) {
                final ValueMap vm = ResourceUtil.getValueMap(rsrc);
                final Calendar changed = (Calendar) vm.get(Job.PROPERTY_JOB_CREATED);
                if ( changed != null && scheduled.created.compareTo(changed) < 0 ) {
                    read = true;
                }
            }
            if ( !read ) {
                // nothing changes
                newScheduledJobs.put(id, scheduled);
            }
        } else {
            read = true;
        }
        if ( read ) {
            // read
            final Holder holder = new Holder();
            holder.read = System.currentTimeMillis();

            final Map<String, Object> properties = this.readScheduledJob(rsrc);
            if ( properties != null ) {
                holder.created = (Calendar) properties.get(Job.PROPERTY_JOB_CREATED);
                holder.info = this.addOrUpdateScheduledJob(properties, scheduled != null ? scheduled.info : null);
            }
            newScheduledJobs.put(id, holder);

            if ( holder.info == null && scheduled != null && scheduled.info != null ) {
                this.jobScheduler.unscheduleJob(scheduled.info);
            }
            if ( holder.info != null ) {
                this.jobScheduler.scheduleJob(holder.info);
            }
        }
    }

    /**
     * Remove a scheduled job
     * @param info The schedule info
     */
    public void remove(final ScheduledJobInfoImpl info) {
        final String scheduleKey = ResourceHelper.filterName(info.getName());

        final ResourceResolver resolver = configuration.createResourceResolver();
        try {
            final StringBuilder sb = new StringBuilder(configuration.getScheduledJobsPath(true));
            sb.append(scheduleKey);
            final String path = sb.toString();

            final Resource eventResource = resolver.getResource(path);
            if ( eventResource != null ) {
                resolver.delete(eventResource);
                resolver.commit();
            }
        } catch (final PersistenceException pe) {
            // we ignore the exception if removing fails
            ignoreException(pe);
        } finally {
            resolver.close();
        }

        synchronized ( this.scheduledJobs ) {
            final Holder h = scheduledJobs.remove(scheduleKey);
            if ( h != null && h.info != null ) {
                jobScheduler.unscheduleJob(h.info);
            }
        }
    }

    public void updateSchedule(final String scheduleName, final Collection<ScheduleInfo> scheduleInfo) {

        final ResourceResolver resolver = configuration.createResourceResolver();
        try {
            final String scheduleKey = ResourceHelper.filterName(scheduleName);

            final StringBuilder sb = new StringBuilder(configuration.getScheduledJobsPath(true));
            sb.append(scheduleKey);
            final String path = sb.toString();

            final Resource rsrc = resolver.getResource(path);
            // This is an update, if we can't find the resource we ignore it
            if ( rsrc != null ) {
                final Calendar now = Calendar.getInstance();

                // update holder first
                synchronized ( scheduledJobs ) {
                    final Holder h = scheduledJobs.get(scheduleKey);
                    if ( h != null ) {
                        h.created = now;
                    }
                }

                final ModifiableValueMap mvm = rsrc.adaptTo(ModifiableValueMap.class);
                mvm.put(Job.PROPERTY_JOB_CREATED, now);
                final String[] infoArray = new String[scheduleInfo.size()];
                int index = 0;
                for(final ScheduleInfo si : scheduleInfo) {
                    infoArray[index] = ((ScheduleInfoImpl)si).getSerializedString();
                    index++;
                }
                mvm.put(ResourceHelper.PROPERTY_SCHEDULE_INFO, infoArray);

                try {
                    resolver.commit();
                } catch ( final PersistenceException pe) {
                    logger.warn("Unable to update scheduled job " + scheduleName, pe);
                }
            }
        } finally {
            resolver.close();
        }
    }

    /**
     * Helper method which just logs the exception in debug mode.
     * @param e The exception
     */
    private void ignoreException(final Exception e) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Ignored exception " + e.getMessage(), e);
        }
    }

    public void maintenance() {
        this.addFullScan();
    }
}
