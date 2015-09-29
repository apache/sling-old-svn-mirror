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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.event.jobs.Job;

public class JobHandlingUtils {

    private static final String DISTRIBUTION_PACKAGE_PREFIX = "distribution.";
    private static final String ID = DISTRIBUTION_PACKAGE_PREFIX + "item.id";

    public static DistributionQueueItem getItem(final Job job) {

        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : job.getPropertyNames()) {
            if (key.startsWith(DISTRIBUTION_PACKAGE_PREFIX)) {
                String infoKey = key.substring(DISTRIBUTION_PACKAGE_PREFIX.length());
                properties.put(infoKey, job.getProperty(key));
            }
        }

        String id = (String) job.getProperty(ID);

        return new DistributionQueueItem(id, properties);
    }

    public static Map<String, Object> createFullProperties(DistributionQueueItem queueItem) {
        Map<String, Object> properties = new HashMap<String, Object>();

        for (String key : queueItem.keySet()) {
            Object value = queueItem.get(key);
            if (value != null) {
                properties.put(DISTRIBUTION_PACKAGE_PREFIX + key, queueItem.get(key));
            }
        }

        properties.put(ID, queueItem.getId());

        return properties;
    }

    public static Map<String, Object> createIdProperties(String itemId) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ID, itemId);
        return properties;
    }

    public static String getQueueName(Job job) {

        String topic = job.getTopic();
        if (topic == null || !topic.startsWith(JobHandlingDistributionQueue.DISTRIBUTION_QUEUE_TOPIC)) return null;

        String queue = topic.substring(JobHandlingDistributionQueue.DISTRIBUTION_QUEUE_TOPIC.length() + 1);
        int idx = queue.indexOf("/");

        if (idx < 0) return "";

        return queue.substring(idx + 1);
    }

    public static DistributionQueueItemStatus getStatus(final Job job) {
        String queueName = getQueueName(job);
        int attempts = job.getRetryCount();

        DistributionQueueItemStatus status = new DistributionQueueItemStatus(job.getCreated(),
                attempts > 0 ? DistributionQueueItemState.ERROR : DistributionQueueItemState.QUEUED,
                attempts, queueName);

        return status;
    }

    public static DistributionQueueEntry getEntry(final Job job) {
        DistributionQueueItem item = getItem(job);
        DistributionQueueItemStatus itemStatus = getStatus(job);

        if (item != null && itemStatus != null) {
            return new DistributionQueueEntry(item, itemStatus);
        }

        return null;
    }


}
