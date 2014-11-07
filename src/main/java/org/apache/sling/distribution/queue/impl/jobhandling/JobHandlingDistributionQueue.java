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
import org.apache.sling.distribution.queue.DistributionQueueItemSelector;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemState.ItemState;
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

    protected JobHandlingDistributionQueue(String name, String topic, JobManager jobManager) {
        this.name = name;
        this.topic = topic;
        this.jobManager = jobManager;
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
    public DistributionQueueItemState getStatus(@Nonnull DistributionQueueItem distributionPackage)
            throws DistributionQueueException {
        try {
            Map<String, Object> properties = JobHandlingUtils.createIdProperties(distributionPackage.getId());
            Job job = jobManager.getJob(topic, properties);
            if (job != null) {

                DistributionQueueItemState itemState = new DistributionQueueItemState(job.getCreated(),
                        ItemState.valueOf(job.getJobState().toString()),
                        job.getRetryCount());

                log.info("status of job {} is {}", job.getId(), job.getJobState());

                return itemState;
            } else {
                DistributionQueueItemState itemState = new DistributionQueueItemState(ItemState.DROPPED);
                return itemState;
            }
        } catch (Exception e) {
            throw new DistributionQueueException("unable to retrieve the queue status", e);
        }
    }

    public DistributionQueueItem getHead() {
        Job firstItem = getFirstJob();
        if (firstItem != null) {
            return JobHandlingUtils.getPackage(firstItem);
        } else {
            return null;
        }
    }

    private Job getFirstJob() {
        log.info("getting first item in the queue");

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

        int i =0;
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

    @Nonnull
    public List<DistributionQueueItem> getItems(DistributionQueueItemSelector selector) {
        if (selector == null) {
            selector = new DistributionQueueItemSelector(0, -1);
        }

        List<DistributionQueueItem> items = new ArrayList<DistributionQueueItem>();
        Collection<Job> jobs = getJobs(selector.getSkip(), selector.getLimit());
        for (Job job : jobs) {
            items.add(JobHandlingUtils.getPackage(job));
        }

        return items;
    }

    public void remove(@Nonnull String id) {
        Job job = getJob(id);

        if (job != null) {
            boolean removed = jobManager.removeJobById(job.getId());
            log.info("item with id {} removed from the queue: {}", id, removed);
        }
    }

}
