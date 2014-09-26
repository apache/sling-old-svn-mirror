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
package org.apache.sling.replication.queue.impl.jobhandling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobManager.QueryType;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueItemState.ItemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a {@link ReplicationQueue} based on Sling Job Handling facilities
 */
public class JobHandlingReplicationQueue implements ReplicationQueue {

    public final static String REPLICATION_QUEUE_TOPIC = "org/apache/sling/replication/queue";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;

    private final String topic;

    private final JobManager jobManager;

    protected JobHandlingReplicationQueue(String name, String topic, JobManager jobManager) {
        this.name = name;
        this.topic = topic;
        this.jobManager = jobManager;
    }

    public String getName() {
        return name;
    }

    public boolean add(ReplicationQueueItem item) {
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

    public ReplicationQueueItemState getStatus(ReplicationQueueItem replicationPackage)
            throws ReplicationQueueException {
        ReplicationQueueItemState itemStatus = new ReplicationQueueItemState();
        try {
            Map<String, Object> properties = JobHandlingUtils.createIdProperties(replicationPackage.getId());
            Job job = jobManager.getJob(topic, properties);
            if (job != null) {
                itemStatus.setAttempts(job.getRetryCount());
                itemStatus.setItemState(ItemState.valueOf(job.getJobState().toString()));
                itemStatus.setEntered(job.getCreated());
                log.info("status of job {} is {}", job.getId(), job.getJobState());
            } else {
                itemStatus.setItemState(ItemState.DROPPED);
            }
        } catch (Exception e) {
            throw new ReplicationQueueException("unable to retrieve the queue status", e);
        }
        return itemStatus;
    }

    public ReplicationQueueItem getHead() {
        Job firstItem = getFirstJob();
        if (firstItem != null) {
            return JobHandlingUtils.getPackage(firstItem);
        } else {
            return null;
        }
    }

    private Job getFirstJob() {
        log.info("getting first item in the queue");

        Collection<Job> jobs = getJobs(1);
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

    private Collection<Job> getJobs(int limit) {
        return jobManager.findJobs(QueryType.ALL, topic, limit);
    }

    public boolean isEmpty() {
        return getItems().isEmpty();
    }

    public List<ReplicationQueueItem> getItems() {
        List<ReplicationQueueItem> items = new ArrayList<ReplicationQueueItem>();
        Collection<Job> jobs = getJobs(-1);
        for (Job job : jobs) {
            items.add(JobHandlingUtils.getPackage(job));
        }

        return items;
    }

    public void remove(String id) {
        Job job = getJob(id);

        if (job != null) {
            boolean removed = jobManager.removeJobById(job.getId());
            log.info("item with id {} removed from the queue: {}", id, removed);
        }
    }

}
