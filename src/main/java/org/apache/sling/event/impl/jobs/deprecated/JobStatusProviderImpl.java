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
package org.apache.sling.event.impl.jobs.deprecated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.JobStatusProvider;
import org.apache.sling.event.JobsIterator;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobManager.QueryType;
import org.apache.sling.event.jobs.Queue;
import org.osgi.service.event.Event;


/**
 * Implementation of the deprecated job status provider
 *
 * @deprecated
 */
@Deprecated
@Component
@Service(value=JobStatusProvider.class)
public class JobStatusProviderImpl
    implements JobStatusProvider {

    @Reference
    private JobManager jobManager;

    /**
     * @see org.apache.sling.event.JobStatusProvider#removeJob(java.lang.String, java.lang.String)
     */
    @Override
    public boolean removeJob(final String topic, final String jobId) {
        if ( jobId != null && topic != null ) {
            final Event job = this.jobManager.findJob(topic,
                    Collections.singletonMap(ResourceHelper.PROPERTY_JOB_NAME, (Object)jobId));
            if ( job != null ) {
                return this.removeJob((String)job.getProperty(ResourceHelper.PROPERTY_JOB_ID));
            }
        }
        return true;
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#removeJob(java.lang.String)
     */
    @Override
    public boolean removeJob(String jobId) {
        return this.jobManager.removeJob(jobId);
    }


    /**
     * @see org.apache.sling.event.JobStatusProvider#forceRemoveJob(java.lang.String, java.lang.String)
     */
    @Override
    public void forceRemoveJob(final String topic, final String jobId) {
        if ( jobId != null && topic != null ) {
            final Event job = this.jobManager.findJob(topic,
                    Collections.singletonMap(ResourceHelper.PROPERTY_JOB_NAME, (Object)jobId));
            if ( job != null ) {
                this.forceRemoveJob((String)job.getProperty(ResourceHelper.PROPERTY_JOB_ID));
            }
        }
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#forceRemoveJob(java.lang.String)
     */
    @Override
    public void forceRemoveJob(final String jobId) {
        this.jobManager.forceRemoveJob(jobId);
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#wakeUpJobQueue(java.lang.String)
     */
    @Override
    public void wakeUpJobQueue(final String jobQueueName) {
        final Queue q = this.jobManager.getQueue(jobQueueName);
        if ( q != null ) {
            q.resume();
        }
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#queryAllJobs(String, Map...)
     */
    @Override
    public JobsIterator queryAllJobs(final String topic, final Map<String, Object>... filterProps) {
        return new JobsIteratorImpl(this.jobManager.queryJobs(QueryType.ALL, topic, filterProps));
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#queryCurrentJobs(String, Map...)
     */
    @Override
    public JobsIterator queryCurrentJobs(final String topic, final Map<String, Object>... filterProps) {
        return new JobsIteratorImpl(this.jobManager.queryJobs(QueryType.ACTIVE, topic, filterProps));
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#queryScheduledJobs(String, Map...)
     */
    @Override
    public JobsIterator queryScheduledJobs(final String topic, final Map<String, Object>... filterProps) {
        return new JobsIteratorImpl(this.jobManager.queryJobs(QueryType.QUEUED, topic, filterProps));
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#getCurrentJobs(java.lang.String)
     */
    @Override
    @Deprecated
    public Collection<Event> getCurrentJobs(String topic) {
        return this.getCurrentJobs(topic, (Map<String, Object>[])null);
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#getScheduledJobs(java.lang.String)
     */
    @Override
    @Deprecated
    public Collection<Event> getScheduledJobs(String topic) {
        return this.getScheduledJobs(topic, (Map<String, Object>[])null);
    }

    private Collection<Event> asList(final JobsIterator ji)  {
        final List<Event> jobs = new ArrayList<Event>();
        while ( ji.hasNext() ) {
            jobs.add(ji.next());
        }
        return jobs;
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#getCurrentJobs(java.lang.String, java.util.Map...)
     */
    @Override
    @Deprecated
    public Collection<Event> getCurrentJobs(String topic, Map<String, Object>... filterProps) {
        return this.asList(this.queryCurrentJobs(topic, filterProps));
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#getScheduledJobs(java.lang.String, java.util.Map...)
     */
    @Override
    @Deprecated
    public Collection<Event> getScheduledJobs(String topic, Map<String, Object>... filterProps) {
        return this.asList(this.queryScheduledJobs(topic, filterProps));
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#getAllJobs(java.lang.String, java.util.Map...)
     */
    @Override
    @Deprecated
    public Collection<Event> getAllJobs(String topic, Map<String, Object>... filterProps) {
        return this.asList(this.queryAllJobs(topic, filterProps));
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#cancelJob(java.lang.String, java.lang.String)
     */
    @Override
    @Deprecated
    public void cancelJob(String topic, String jobId) {
        this.removeJob(topic, jobId);
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#cancelJob(java.lang.String)
     */
    @Override
    @Deprecated
    public void cancelJob(String jobId) {
        this.removeJob(jobId);
    }
}
