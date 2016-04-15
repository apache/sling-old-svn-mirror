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
package org.apache.sling.distribution.queue.impl.jobhandling;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueState;
import org.apache.sling.distribution.queue.DistributionQueueStatus;
import org.apache.sling.distribution.queue.impl.DistributionQueueUtils;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobManager.QueryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a {@link org.apache.sling.distribution.queue.DistributionQueue} based on Sling Job Handling facilities
 */
public class JobHandlingDistributionQueue implements DistributionQueue {

    public final static String DISTRIBUTION_QUEUE_TOPIC = "org/apache/sling/distribution/queue";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;

    private final String topic;

    private final JobManager jobManager;

    private final boolean isActive;

    JobHandlingDistributionQueue(String name, String topic, JobManager jobManager, boolean isActive) {
        this.name = name;
        this.topic = topic;
        this.jobManager = jobManager;
        this.isActive = isActive;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public DistributionQueueEntry add(@Nonnull DistributionQueueItem item) {
        try {
            Map<String, Object> properties = JobHandlingUtils.createFullProperties(item);

            Job job = jobManager.createJob(topic).properties(properties).add();
            log.debug("job {} added for item {}", job.getId(), item.getPackageId());

            return JobHandlingUtils.getEntry(job);
        } catch (Exception e) {
            log.error("could not add an item to the queue", e);
            return null;
        }
    }


    public DistributionQueueEntry getHead() {
        Job firstJob = getFirstJob();
        if (firstJob != null) {
            DistributionQueueEntry entry = JobHandlingUtils.getEntry(firstJob);
            return entry;
        } else {
            return null;
        }
    }

    private Job getFirstJob() {
        log.debug("getting first item in the queue");

        List<Job> jobs = getJobs(0, 1);
        if (jobs.size() > 0) {
            Job firstItem = jobs.get(0);
            log.debug("first item in the queue is {}, retried {} times", firstItem.getId(), firstItem.getRetryCount());
            return firstItem;
        }
        return null;
    }

    private Job getJob(String itemId) {
        String jobId = JobHandlingUtils.unescapeId(itemId);
        Job job = jobManager.getJobById(jobId);

        if (job == null) {
            log.warn("item with id {} cannot be found", itemId);
        }

        return job;
    }

    private List<Job> getJobs(int skip, int limit) {
        int actualSkip = skip < 0 ? 0 : skip;
        int actualLimit = limit < 0 ? -1 : actualSkip + limit;


        Collection<Job> jobs = jobManager.findJobs(QueryType.ALL, topic, actualLimit);
        List<Job> result = new ArrayList<Job>();

        int i = 0;
        for (Job job : jobs) {
            if (i >= actualSkip) {
                result.add(job);
            }
            i++;
        }

        return result;
    }


    @Nonnull
    public List<DistributionQueueEntry> getItems(int skip, int limit) {

        List<DistributionQueueEntry> items = new ArrayList<DistributionQueueEntry>();
        Collection<Job> jobs = getJobs(skip, limit);
        for (Job job : jobs) {
            items.add(JobHandlingUtils.getEntry(job));
        }

        return items;
    }

    public DistributionQueueEntry getItem(@Nonnull String id) {
        Job job = getJob(id);

        if (job != null) {
            DistributionQueueEntry entry = JobHandlingUtils.getEntry(job);
            return entry;
        }

        return null;
    }

    public DistributionQueueEntry remove(@Nonnull String id) {
        boolean removed = false;
        Job job = getJob(id);

        DistributionQueueEntry entry = null;

        if (job != null) {
            entry = JobHandlingUtils.getEntry(job);
            removed = jobManager.removeJobById(job.getId());
        }

        log.debug("item with id {} removed from the queue: {}", id, removed);
        return entry;
    }


    @Override
    @Nonnull
    public DistributionQueueStatus getStatus() {
        List<Job> jobs = getJobs(0, -1);
        Job firstJob = jobs.size() > 0 ? jobs.get(0) : null;

        DistributionQueueItem firstItem = firstJob != null ? JobHandlingUtils.getItem(firstJob) : null;
        DistributionQueueItemStatus firstItemStatus = firstJob != null ? JobHandlingUtils.getStatus(firstJob) : null;

        DistributionQueueState state = DistributionQueueUtils.calculateState(firstItem, firstItemStatus);
        if (!isActive) {
            state = DistributionQueueState.PASSIVE;
        }

        int itemsCount = jobs.size();

        return new DistributionQueueStatus(itemsCount, state);
    }

}
