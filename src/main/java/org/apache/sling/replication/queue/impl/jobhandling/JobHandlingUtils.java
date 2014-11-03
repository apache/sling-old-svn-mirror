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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.replication.packaging.ReplicationPackageInfo;
import org.apache.sling.replication.queue.ReplicationQueueItem;

public class JobHandlingUtils {

    private static final String PATHS = "replication.package.paths";

    public static final String ID = "replication.package.id";

    private static final String TYPE = "replication.package.type";

    protected static final String ACTION = "replication.package.action";

    protected static final String ORIGIN = "replication.package.origin";

    public static ReplicationQueueItem getPackage(final Job job) {
        ReplicationPackageInfo packageInfo = new ReplicationPackageInfo();
        packageInfo.setOrigin((String) job.getProperty(ORIGIN));

        return new ReplicationQueueItem((String) job.getProperty(ID),
                (String[]) job.getProperty(PATHS),
                String.valueOf(job.getProperty(ACTION)),
                String.valueOf(job.getProperty(TYPE)), packageInfo);
    }

    public static Map<String, Object> createFullProperties(
            ReplicationQueueItem replicationQueueItem) {
        Map<String, Object> properties = new HashMap<String, Object>();

        properties.put(ID, replicationQueueItem.getId());
        properties.put(PATHS, replicationQueueItem.getPaths());
        properties.put(ACTION, replicationQueueItem.getAction());
        properties.put(TYPE, replicationQueueItem.getType());

        ReplicationPackageInfo packageInfo = replicationQueueItem.getPackageInfo();
        if (packageInfo != null && packageInfo.getOrigin() != null) {
            properties.put(ORIGIN, packageInfo.getOrigin());
        }

        return properties;
    }

    public static Map<String, Object> createIdProperties(String itemId) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ID, itemId);
        return properties;
    }

    public static String getQueueName(Job job) {

        String topic = job.getTopic();
        if (topic == null || !topic.startsWith(JobHandlingReplicationQueue.REPLICATION_QUEUE_TOPIC)) return null;

        String queue = topic.substring(JobHandlingReplicationQueue.REPLICATION_QUEUE_TOPIC.length() + 1);
        int idx = queue.indexOf("/");

        if (idx < 0) return "";

        return queue.substring(idx + 1);
    }

}
