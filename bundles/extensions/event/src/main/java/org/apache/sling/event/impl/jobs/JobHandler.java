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


import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.impl.jobs.config.TopologyCapabilities;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.consumer.JobExecutor;


/**
 * This object adds actions to a {@link JobImpl}.
 */
public class JobHandler {

    private final JobImpl job;

    public volatile long started = -1;

    private volatile boolean isStopped = false;

    private final JobManagerConfiguration configuration;

    private final JobExecutor consumer;

    public JobHandler(final JobImpl job,
            final JobExecutor consumer,
            final JobManagerConfiguration configuration) {
        this.job = job;
        this.consumer = consumer;
        this.configuration = configuration;
    }

    public JobImpl getJob() {
        return this.job;
    }

    public JobExecutor getConsumer() {
        return this.consumer;
    }

    public boolean startProcessing(final Queue queue) {
        this.isStopped = false;
        return this.persistJobProperties(this.job.prepare(queue));
    }

    /**
     * Reschedule the job
     * Update the retry count and remove the started time.
     * @return <code>true</code> if rescheduling was successful, <code>false</code> otherwise.
     */
    public boolean reschedule() {
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final Resource jobResource = resolver.getResource(job.getResourcePath());
            if ( jobResource != null ) {
                final ModifiableValueMap mvm = jobResource.adaptTo(ModifiableValueMap.class);
                mvm.put(Job.PROPERTY_JOB_RETRY_COUNT, job.getProperty(Job.PROPERTY_JOB_RETRY_COUNT, Integer.class));
                if ( job.getProperty(Job.PROPERTY_RESULT_MESSAGE) != null ) {
                    mvm.put(Job.PROPERTY_RESULT_MESSAGE, job.getProperty(Job.PROPERTY_RESULT_MESSAGE));
                }
                mvm.remove(Job.PROPERTY_JOB_STARTED_TIME);
                mvm.put(JobImpl.PROPERTY_JOB_QUEUED, Calendar.getInstance());
                try {
                    resolver.commit();
                    return true;
                } catch ( final PersistenceException pe ) {
                    this.configuration.getMainLogger().debug("Unable to update reschedule properties for job " + job.getId(), pe);
                }
            }
        } finally {
            resolver.close();
        }

        return false;
    }

    /**
     * Finish a job.
     * @param state The state of the processing
     * @param keepJobInHistory whether to keep the job in the job history.
     * @param duration the duration of the processing.
     */
    public void finished(final Job.JobState state,
                          final boolean keepJobInHistory,
                          final Long duration) {
        final boolean isSuccess = (state == Job.JobState.SUCCEEDED);
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final Resource jobResource = resolver.getResource(job.getResourcePath());
            if ( jobResource != null ) {
                try {
                    String newPath = null;
                    if ( keepJobInHistory ) {
                        final ValueMap vm = ResourceHelper.getValueMap(jobResource);
                        newPath = this.configuration.getStoragePath(job.getTopic(), job.getId(), isSuccess);
                        final Map<String, Object> props = new HashMap<String, Object>(vm);
                        props.put(JobImpl.PROPERTY_FINISHED_STATE, state.name());
                        if ( isSuccess ) {
                            // we set the finish date to start date + duration
                            final Date finishDate = new Date();
                            finishDate.setTime(job.getProcessingStarted().getTime().getTime() + duration);
                            final Calendar finishCal = Calendar.getInstance();
                            finishCal.setTime(finishDate);
                            props.put(JobImpl.PROPERTY_FINISHED_DATE, finishCal);
                        } else {
                            // current time is good enough
                            props.put(JobImpl.PROPERTY_FINISHED_DATE, Calendar.getInstance());
                        }
                        if ( job.getProperty(Job.PROPERTY_RESULT_MESSAGE) != null ) {
                            props.put(Job.PROPERTY_RESULT_MESSAGE, job.getProperty(Job.PROPERTY_RESULT_MESSAGE));
                        }
                        ResourceHelper.getOrCreateResource(resolver, newPath, props);
                    }
                    resolver.delete(jobResource);
                    resolver.commit();

                    if ( keepJobInHistory && configuration.getMainLogger().isDebugEnabled() ) {
                        if ( isSuccess ) {
                            configuration.getMainLogger().debug("Kept successful job {} at {}", Utility.toString(job), newPath);
                        } else {
                            configuration.getMainLogger().debug("Moved cancelled job {} to {}", Utility.toString(job), newPath);
                        }
                    }
                } catch ( final PersistenceException pe ) {
                    this.configuration.getMainLogger().warn("Unable to finish job " + job.getId(), pe);
                } catch (final InstantiationException ie) {
                    // something happened with the resource in the meantime
                    this.configuration.getMainLogger().debug("Unable to instantiate job", ie);
                }
            }
        } finally {
            resolver.close();
        }
    }

    /**
     * Reassign to a new instance.
     */
    public void reassign() {
        final QueueInfo queueInfo = this.configuration.getQueueConfigurationManager().getQueueInfo(job.getTopic());
        // Sanity check if queue configuration has changed
        final TopologyCapabilities caps = this.configuration.getTopologyCapabilities();
        final String targetId = (caps == null ? null : caps.detectTarget(job.getTopic(), job.getProperties(), queueInfo));

        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final Resource jobResource = resolver.getResource(job.getResourcePath());
            if ( jobResource != null ) {
                try {
                    final ValueMap vm = ResourceHelper.getValueMap(jobResource);
                    final String newPath = this.configuration.getUniquePath(targetId, job.getTopic(), job.getId(), job.getProperties());

                    final Map<String, Object> props = new HashMap<String, Object>(vm);
                    props.remove(Job.PROPERTY_JOB_QUEUE_NAME);
                    if ( targetId == null ) {
                        props.remove(Job.PROPERTY_JOB_TARGET_INSTANCE);
                    } else {
                        props.put(Job.PROPERTY_JOB_TARGET_INSTANCE, targetId);
                    }
                    props.remove(Job.PROPERTY_JOB_STARTED_TIME);

                    try {
                        ResourceHelper.getOrCreateResource(resolver, newPath, props);
                        resolver.delete(jobResource);
                        resolver.commit();
                    } catch ( final PersistenceException pe ) {
                        this.configuration.getMainLogger().warn("Unable to reassign job " + job.getId(), pe);
                    }
                } catch (final InstantiationException ie) {
                    // something happened with the resource in the meantime
                    this.configuration.getMainLogger().debug("Unable to instantiate job", ie);
                }
            }
        } finally {
            resolver.close();
        }
    }

    /**
     * Update the property of a job in the resource tree
     * @param propNames the property names to update
     * @return {@code true} if the update was successful.
     */
    public boolean persistJobProperties(final String... propNames) {
        if ( propNames != null ) {
            final ResourceResolver resolver = this.configuration.createResourceResolver();
            try {
                final Resource jobResource = resolver.getResource(job.getResourcePath());
                if ( jobResource != null ) {
                    final ModifiableValueMap mvm = jobResource.adaptTo(ModifiableValueMap.class);
                    for(final String propName : propNames) {
                        final Object val = job.getProperty(propName);
                        if ( val != null ) {
                            if ( val.getClass().isEnum() ) {
                                mvm.put(propName, val.toString());
                            } else {
                                mvm.put(propName, val);
                            }
                        } else {
                            mvm.remove(propName);
                        }
                    }
                    resolver.commit();

                    return true;
                } else {
                    this.configuration.getMainLogger().debug("No job resource found at {}", job.getResourcePath());
                }
            } catch ( final PersistenceException ignore ) {
                this.configuration.getMainLogger().debug("Unable to persist properties", ignore);
            } finally {
                resolver.close();
            }
            return false;
        }
        return true;
    }

    public boolean isStopped() {
        return this.isStopped;
    }

    public void stop() {
        this.isStopped = true;
    }

    public void addToRetryList() {
        this.configuration.addJobToRetryList(this.job);

    }

    public boolean removeFromRetryList() {
        return this.configuration.removeJobFromRetryList(this.job);
    }

    @Override
    public int hashCode() {
        return this.job.getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if ( ! (obj instanceof JobHandler) ) {
            return false;
        }
        return this.job.getId().equals(((JobHandler)obj).job.getId());
    }

    @Override
    public String toString() {
        return "JobHandler(" + this.job.getId() + ")";
    }
}