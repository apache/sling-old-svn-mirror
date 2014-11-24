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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.distribution.communication.DistributionRequestType;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.event.jobs.Job;

public class JobHandlingUtils {

    private static final String PATHS = "distribution.package.paths";

    public static final String ID = "distribution.package.id";

    private static final String TYPE = "distribution.package.type";

    protected static final String REQUEST_TYPE = "distribution.package.request.type";

    protected static final String ORIGIN = "distribution.package.origin";

    public static DistributionQueueItem getItem(final Job job) {
        DistributionPackageInfo packageInfo = new DistributionPackageInfo();
        packageInfo.setOrigin((URI) job.getProperty(ORIGIN));
        packageInfo.setPaths((String[]) job.getProperty(PATHS));
        packageInfo.setRequestType((DistributionRequestType) job.getProperty(REQUEST_TYPE));

        return new DistributionQueueItem((String) job.getProperty(ID),
                String.valueOf(job.getProperty(TYPE)), packageInfo);
    }

    public static Map<String, Object> createFullProperties(
            DistributionQueueItem distributionQueueItem) {
        Map<String, Object> properties = new HashMap<String, Object>();

        properties.put(ID, distributionQueueItem.getId());
        properties.put(TYPE, distributionQueueItem.getType());

        DistributionPackageInfo info = distributionQueueItem.getPackageInfo();
        if (info.getPaths() != null) {
            properties.put(PATHS, info.getPaths());
        }
        if (info.getRequestType() != null) {
            properties.put(REQUEST_TYPE, info.getRequestType());
        }
        if (info.getOrigin() != null) {
            properties.put(ORIGIN, info.getOrigin());
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
        if (topic == null || !topic.startsWith(JobHandlingDistributionQueue.DISTRIBUTION_QUEUE_TOPIC)) return null;

        String queue = topic.substring(JobHandlingDistributionQueue.DISTRIBUTION_QUEUE_TOPIC.length() + 1);
        int idx = queue.indexOf("/");

        if (idx < 0) return "";

        return queue.substring(idx + 1);
    }

}
