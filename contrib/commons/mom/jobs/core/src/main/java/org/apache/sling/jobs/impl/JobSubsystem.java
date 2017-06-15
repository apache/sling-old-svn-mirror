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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.jobs.Job;
import org.apache.sling.jobs.JobBuilder;
import org.apache.sling.jobs.JobCallback;
import org.apache.sling.jobs.JobConsumer;
import org.apache.sling.jobs.JobManager;
import org.apache.sling.jobs.JobTypeValve;
import org.apache.sling.jobs.JobUpdateListener;
import org.apache.sling.jobs.Types;
import org.apache.sling.jobs.impl.spi.JobStorage;
import org.apache.sling.jobs.impl.storage.InMemoryJobStorage;
import org.apache.sling.mom.QueueManager;
import org.apache.sling.mom.TopicManager;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NB, this does *not* register as a JobConsumer service. it implements a JobConsumer so that it can consume Jobs from JobQueueConsumers.
 */
@Component(service = JobManager.class, immediate=true)
public class JobSubsystem  implements JobManager, JobConsumer {


    private static final Logger LOGGER = LoggerFactory.getLogger(JobSubsystem.class);
    private JobManagerImpl manager;
    private JobStorage jobStorage;
    private OutboundJobUpdateListener messageSender;

    /**
     * Contains a map of JobConsumers wrapped by JobConsumerHolders keyed by ServiceReference.
     */
    private final Map<ServiceReference<JobConsumer>, JobConsumerHolder> registrations =
            new ConcurrentHashMap<ServiceReference<JobConsumer>, JobConsumerHolder>();

    @Reference
    private TopicManager topicManager;
    @Reference
    private QueueManager queueManager;

    @Activate
    public synchronized void activate() {
        jobStorage = new InMemoryJobStorage();
        messageSender = new OutboundJobUpdateListener(topicManager, queueManager);
        manager = new JobManagerImpl(jobStorage, messageSender);
    }

    @Deactivate
    public synchronized void deactivate() {
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
    @Reference(service = JobConsumer.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind="removeConsumer")
    public synchronized  void addConsumer(ServiceReference<JobConsumer> serviceRef) {
        if (registrations.containsKey(serviceRef)) {
            LOGGER.error("Registration for service reference is already present {}",serviceRef);
            return;
        }
        JobConsumerHolder jobConsumerHolder = new JobConsumerHolder(serviceRef.getBundle().getBundleContext().getService(serviceRef), serviceRef);
        registrations.put(serviceRef, jobConsumerHolder);
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

        public JobConsumerHolder(JobConsumer consumer, ServiceReference<JobConsumer> ref) {
            this.consumer = consumer;
            if ( consumer instanceof JobTypeValve) {
                jobTypes = Collections.emptySet();
            } else {
                jobTypes = getJobTypes(ref);
            }
        }

        public Set<Types.JobType> getJobTypes(ServiceReference<JobConsumer> ref) {
            Object types = ref.getProperty(JobConsumer.JOB_TYPES);
            if (types instanceof String) {
                return Types.jobType(new String[]{(String) types});

            } else if (types instanceof String[]) {
                return Types.jobType((String[]) types);

            } else if (types instanceof Iterable) {
                List<String> l = new ArrayList<String>();
                for (Object o : (Iterable<?>) types) {
                    l.add(String.valueOf(o));
                }
                return Types.jobType(l.toArray(new String[l.size()]));
            }
            throw new IllegalArgumentException("For the JobConsumer to work, the job consumer must either " +
                    "implement a JobTypeValve or define a list of JobTypes, neither were specified. " +
                    "Please check the implementation or OSGi configuration, was expecting " +
                    JobConsumer.JOB_TYPES + " property to be set to a String[]");
        }




        @Override
        public boolean accept(@Nonnull Types.JobType jobType) {
            if ( consumer instanceof JobTypeValve) {
                return ((JobTypeValve) consumer).accept(jobType);
            }
            return jobTypes.contains(jobType);
        }

        @Override
        public void close() {
            // nothing to do at the moment.
        }
    }
}
