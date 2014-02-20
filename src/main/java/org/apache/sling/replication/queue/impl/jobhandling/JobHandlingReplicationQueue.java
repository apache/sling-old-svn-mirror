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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
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

    public boolean add(ReplicationQueueItem replicationPackage) {
        boolean result = true;
        try {
            Map<String, Object> properties = JobHandlingUtils
                    .createFullPropertiesFromPackage(replicationPackage);

            Job job = jobManager.createJob(topic).properties(properties).add();
            if (log.isInfoEnabled()) {
                log.info("job {} added", job.getId());
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("could not add an item to the queue", e);
            }
            result = false;
        }
        return result;
    }

    public ReplicationQueueItemState getStatus(ReplicationQueueItem replicationPackage)
            throws ReplicationQueueException {
        ReplicationQueueItemState itemStatus = new ReplicationQueueItemState();
        try {
            Map<String, Object> properties = JobHandlingUtils
                    .createIdPropertiesFromPackage(replicationPackage);
            Job job = jobManager.getJob(topic, properties);
            if (job != null) {
                itemStatus.setAttempts(job.getRetryCount());
                itemStatus.setItemState(ItemState.valueOf(job.getJobState().toString()));
                itemStatus.setEntered(job.getCreated());
                if (log.isInfoEnabled()) {
                    log.info("status of job {} is {}", job.getId(), job.getJobState());
                }
            } else {
                itemStatus.setItemState(ItemState.DROPPED);
            }
        } catch (Exception e) {
            throw new ReplicationQueueException("unable to retrieve the queue status", e);
        }
        return itemStatus;
    }

    public ReplicationQueueItem getHead() {
        Job firstItem = getFirstItem();
        if (firstItem != null) {
            return JobHandlingUtils.getPackage(firstItem);
        } else {
            return null;
        }
    }

    public void removeHead() {
        Job firstItem = getFirstItem();
        if (firstItem != null) {
            jobManager.removeJobById(firstItem.getId());
        }
    }

    @SuppressWarnings("unchecked")
    private Job getFirstItem() {
        if (log.isInfoEnabled()) {
            log.info("getting first item in the queue");
        }

        HashMap<String, Object> props = new HashMap<String, Object>();
        Collection<Job> jobs = jobManager.findJobs(QueryType.QUEUED, topic, -1, props);
        jobs.addAll(jobManager.findJobs(QueryType.ACTIVE, topic, -1, props));
        if (jobs.size() > 0) {
            ArrayList<Job> list = new ArrayList<Job>(jobs);
            Collections.sort(list, new Comparator<Job>() {
                public int compare(Job o1, Job o2) {
                    return o2.getRetryCount() - o1.getRetryCount();
                }
            });
            Job firstItem = list.get(0);
            if (log.isInfoEnabled()) {
                log.info("first item in the queue is {}, retried {} times", firstItem.getId(),
                        firstItem.getRetryCount());
            }
            return firstItem;
        }
        return null;
    }

    public boolean isEmpty() {
        return getItems().isEmpty();
    }

    public Collection<ReplicationQueueItem> getItems() {
        Collection<ReplicationQueueItem> items = new LinkedList<ReplicationQueueItem>();
        Collection<Job> jobs = jobManager.findJobs(QueryType.ALL, topic, -1);
        for (Job job : jobs) {
            items.add(JobHandlingUtils.getPackage(job));
        }
        return Collections.unmodifiableCollection(items);
    }

    public void remove(String id) {
        boolean removed = jobManager.removeJobById(id);
        if (log.isInfoEnabled()) {
            log.info("item with id {} removed from the queue: {}", id, removed);
        }
    }

}
