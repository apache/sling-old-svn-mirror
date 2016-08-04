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

package org.apache.sling.jobs.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jobs.*;
import org.apache.sling.jobs.impl.spi.JobStorage;
import org.apache.sling.jobs.impl.storage.InMemoryJobStorage;
import org.apache.sling.mom.QueueManager;
import org.apache.sling.mom.TopicManager;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ieb on 11/04/2016.
 * NB, this does *not* register as a JobConsumer service. it implements a JobConsumer so that it can consume Jobs from JobQueueConsumers.
 */
@Component(immediate = true)
@Service(value =  JobManager.class)
public class JobSubsystem  implements JobManager, JobConsumer {


    private static final Logger LOGGER = LoggerFactory.getLogger(JobSubsystem.class);
    private JobManagerImpl manager;
    private JobStorage jobStorage;
    private OutboundJobUpdateListener messageSender;

    /**
     * Contains a map of JobConsumers wrapped by JobConsumerHolders keyed by ServiceReference.
     */
    @Reference(referenceInterface = JobConsumer.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            bind="addConsumer",
            unbind="removeConsumer")
    private final Map<ServiceReference<JobConsumer>, JobConsumerHolder> registrations =
            new ConcurrentHashMap<ServiceReference<JobConsumer>, JobConsumerHolder>();

    @Reference
    private TopicManager topicManager;
    @Reference
    private QueueManager queueManager;

    @Activate
    public synchronized void activate(@SuppressWarnings("UnusedParameters") Map<String, Object> properties) {
        jobStorage = new InMemoryJobStorage();
        messageSender = new OutboundJobUpdateListener(topicManager, queueManager);
        manager = new JobManagerImpl(jobStorage, messageSender);
    }

    @Deactivate
    public synchronized void deactivate(@SuppressWarnings("UnusedParameters") Map<String, Object> properties) {
        for (Map.Entry<ServiceReference<JobConsumer>, JobConsumerHolder> e : registrations.entrySet()) {
            e.getValue().close();
        }
        registrations.clear();
        manager.dispose();
        messageSender.dispose();
        jobStorage.dispose();
    }

    // --- Job Manager.
    @Nonnull
    @Override
    public JobBuilder newJobBuilder(@Nonnull Types.JobQueue queue, @Nonnull Types.JobType jobType) {
        return manager.newJobBuilder(queue, jobType);
    }

    @Nullable
    @Override
    public Job getJobById(@Nonnull String jobId) {
        return manager.getJobById(jobId);
    }

    @Nullable
    @Override
    public Job getJob(@Nonnull Types.JobQueue queue, @Nonnull Map<String, Object> template) {
        return manager.getJob(queue, template);
    }

    @Nonnull
    @Override
    public Collection<Job> findJobs(@Nonnull QueryType type, @Nonnull Types.JobQueue queue, long limit, @Nullable Map<String, Object>... templates) {
        return manager.findJobs(type, queue, limit, templates);
    }

    @Override
    public void stopJobById(@Nonnull String jobId) {
        manager.stopJobById(jobId);
    }

    @Override
    public boolean abortJob(@Nonnull String jobId) {
        return manager.abortJob(jobId);
    }

    @Nullable
    @Override
    public Job retryJobById(@Nonnull String jobId) {
        return manager.retryJobById(jobId);
    }


    // ---- JobConsumer Registration
    // Register Consumers using
    public synchronized  void addConsumer(ServiceReference<JobConsumer> serviceRef) {
        if (registrations.containsKey(serviceRef)) {
            LOGGER.error("Registration for service reference is already present {}",serviceRef);
            return;
        }
        JobConsumerHolder jobConsumerHolder = new JobConsumerHolder(serviceRef.getBundle().getBundleContext().getService(serviceRef), getServiceProperties(serviceRef));
        registrations.put(serviceRef, jobConsumerHolder);
    }

    private Map<Object, Object> getServiceProperties(ServiceReference<JobConsumer> serviceRef) {
        ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder();
        for ( String k : serviceRef.getPropertyKeys()) {
            builder.put(k, serviceRef.getProperty(k));
        }
        return builder.build();
    }

    public synchronized void removeConsumer(ServiceReference<JobConsumer> serviceRef) {
        JobConsumerHolder jobConsumerHolder = registrations.remove(serviceRef);
        if ( jobConsumerHolder != null) {
            jobConsumerHolder.close();
        }
    }


    // ------- job execution, invoked by JobQueueConsumerFactory.
    @Nonnull
    @Override
    public void execute(@Nonnull Job initialState, @Nonnull JobUpdateListener listener, @Nonnull JobCallback callback) {
        // iterate over the entries. This should cause the entries to come out in natural key order
        // which should respect any priority applied to the Services via ServiceReference. (TODO: check that is the case)
        // TODO: add a Job controller to the job before executing.
        for (Map.Entry<ServiceReference<JobConsumer>,JobConsumerHolder> e : registrations.entrySet()) {
            JobConsumerHolder jobConsumerHolder = e.getValue();
            if (jobConsumerHolder.accept(initialState.getJobType())) {
                jobConsumerHolder.consumer.execute(initialState, listener, callback);
                return;
            }
        }
        throw new IllegalArgumentException("No JobConsumer able to process a job of type "+initialState.getJobType()+" can be found in this instance.");
    }


    /**
     * Holds job consumers and configures a JobTypeValve delegating to the JobConsumer implementation if it implements that interface.
     */
    private class JobConsumerHolder implements JobTypeValve, Closeable {
        private final JobConsumer consumer;
        private final Set<Types.JobType> jobTypes;

        public JobConsumerHolder(JobConsumer consumer, Map<Object, Object> properties) {
            this.consumer = consumer;
            if ( consumer instanceof JobTypeValve) {
                jobTypes = ImmutableSet.of();
            } else {
                jobTypes = Types.jobType((String[]) properties.get(JobConsumer.JOB_TYPES));
            }
        }

        @Override
        public boolean accept(@Nonnull Types.JobType jobType) {
            if ( consumer instanceof JobTypeValve) {
                return ((JobTypeValve) consumer).accept(jobType);
            }
            return jobTypes.contains(jobType);
        }

        public void close() {
            // nothing to do at the moment.
        }
    }
}
