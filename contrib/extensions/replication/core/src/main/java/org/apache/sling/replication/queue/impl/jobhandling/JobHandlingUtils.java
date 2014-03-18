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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobHandlingUtils {

    private static final Logger log = LoggerFactory.getLogger(JobHandlingUtils.class.getName());

    private static final String PATHS = "replication.package.paths";

    public static final String ID = "replication.package.id";

    private static final String TYPE = "replication.package.type";

    protected static final String ACTION = "replication.package.action";

    protected static final String BYTES = "replication.package.bytes";


    public static ReplicationQueueItem getPackage(final Job job) {
        String id = (String) job.getProperty(ID);
        if (id != null) {
            return new ReplicationQueueItem((String) job.getProperty(ID),
                    (String[]) job.getProperty(PATHS),
                    String.valueOf(job.getProperty(ACTION)),
                    String.valueOf(job.getProperty(TYPE)));
        } else {
            return new ReplicationQueueItem((String[]) job.getProperty(PATHS),
                    String.valueOf(job.getProperty(ACTION)),
                    String.valueOf(job.getProperty(TYPE)),
                    unBox((Byte[]) job.getProperty(BYTES)));
        }
    }

    public static Map<String, Object> createFullPropertiesFromPackage(
            ReplicationQueueItem replicationPackage) throws IOException {
        Map<String, Object> properties = new HashMap<String, Object>();

        if (replicationPackage.getId() != null)
            properties.put(ID, replicationPackage.getId());
        properties.put(PATHS, replicationPackage.getPaths());
        properties.put(ACTION, replicationPackage.getAction());
        properties.put(TYPE, replicationPackage.getType());

        if (replicationPackage.getBytes() != null)
            properties.put(BYTES, box(replicationPackage.getBytes()));

        return properties;
    }

    public static Map<String, Object> createIdPropertiesFromPackage(
            ReplicationQueueItem replicationPackage) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ID, replicationPackage.getId());
        return properties;
    }

    public static Byte[] box(byte[] bytes) {
        if (bytes == null) return null;
        Byte[] result = new Byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i];
        }
        return result;
    }

    public static byte[] unBox(Byte[] bytes) {
        if (bytes == null) return null;
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i];
        }
        return result;
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
