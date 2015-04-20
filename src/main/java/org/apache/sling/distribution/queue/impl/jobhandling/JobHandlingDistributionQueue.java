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
import org.apache.sling.distribution.queue.DistributionQueueException;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus.ItemState;
import org.apache.sling.distribution.queue.DistributionQueueState;
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

    public boolean add(@Nonnull DistributionQueueItem item) {
        boolean result = true;
        try {
            Map<String, Object> properties = JobHandlingUtils.createFullProperties(item);

            Job job = jobManager.createJob(topic).properties(properties).add();
            log.info("job {} added", job.getId());
        } catch (Exception e) {
            log.error("could not add an item to the queue", e);
            result = false;
        }
        return result;
    }

    @Nonnull
    public DistributionQueueItemStatus getStatus(@Nonnull DistributionQueueItem distributionPackage)
            throws DistributionQueueException {
        try {
            Map<String, Object> properties = JobHandlingUtils.createIdProperties(distributionPackage.getId());
            Job job = jobManager.getJob(topic, properties);
            if (job != null) {

                DistributionQueueItemStatus itemState = new DistributionQueueItemStatus(job.getCreated(),
                        ItemState.valueOf(job.getJobState().toString()),
                        job.getRetryCount(), name);

                log.info("status of job {} is {}", job.getId(), job.getJobState());

                return itemState;
            } else {
                DistributionQueueItemStatus itemState = new DistributionQueueItemStatus(ItemState.DROPPED, name);
                return itemState;
            }
        } catch (Exception e) {
            throw new DistributionQueueException("unable to retrieve the queue status", e);
        }
    }

    public DistributionQueueItem getHead() {
        Job firstJob = getFirstJob();
        if (firstJob != null) {
            return JobHandlingUtils.getItem(firstJob);
        } else {
            return null;
        }
    }

    private Job getFirstJob() {
        log.debug("getting first item in the queue");

        List<Job> jobs = getJobs(0, 1);
        if (jobs.size() > 0) {
            Job firstItem = jobs.toArray(new Job[jobs.size()])[0];
            log.info("first item in the queue is {}, retried {} times", firstItem.getId(), firstItem.getRetryCount());
            return firstItem;
        }
        return null;
    }

    private Job getJob(String itemId) {
        Map<String, Object> properties = JobHandlingUtils.createIdProperties(itemId);
        Job job = jobManager.getJob(topic, properties);

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

    public boolean isEmpty() {
        return getJobs(0, -1).isEmpty();
    }

    public int getItemsCount() {
        List<Job> jobs = getJobs(0, -1);
        return jobs.size();
    }

    @Nonnull
    public List<DistributionQueueItem> getItems(int skip, int limit) {


        List<DistributionQueueItem> items = new ArrayList<DistributionQueueItem>();
        Collection<Job> jobs = getJobs(skip, limit);
        for (Job job : jobs) {
            items.add(JobHandlingUtils.getItem(job));
        }

        return items;
    }

    public DistributionQueueItem getItem(@Nonnull String id) {
        Job job = getJob(id);

        DistributionQueueItem item = null;

        if (job != null) {
            item = JobHandlingUtils.getItem(job);

            return item;
        }

        return null;
    }

    public DistributionQueueItem remove(@Nonnull String id) {
        boolean removed = false;
        Job job = getJob(id);

        DistributionQueueItem item = null;

        if (job != null) {
            item = JobHandlingUtils.getItem(job);
            removed = jobManager.removeJobById(job.getId());
        }

        log.info("item with id {} removed from the queue: {}", id, removed);
        return item;
    }

    public DistributionQueueState getState() {
        if (!isActive) {
            return DistributionQueueState.PAUSED;
        } else {
            return DistributionQueueUtils.calculateState(this);
        }
    }

}
