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

package org.apache.sling.distribution.packaging.impl;

import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.apache.sling.distribution.serialization.impl.SharedDistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Package related utility methods
 */
public class DistributionPackageUtils {

    static Logger log = LoggerFactory.getLogger(DistributionPackageUtils.class);

    /**
     * distribution package origin queue
     */
    public static String PACKAGE_INFO_PROPERTY_ORIGIN_QUEUE = "internal.origin.queue";

    /**
     * distribution request user
     */
    public static String PACKAGE_INFO_PROPERTY_REQUEST_USER = "internal.request.user";


    /**
     * Acquires the package if it's a {@link SharedDistributionPackage}, via {@link SharedDistributionPackage#acquire(String)}
     * @param distributionPackage a distribution package
     * @param queueName the name of the queue in which the package should be acquired
     */
    public static void acquire(DistributionPackage distributionPackage, String queueName) {
        if (distributionPackage instanceof SharedDistributionPackage) {
            ((SharedDistributionPackage) distributionPackage).acquire(queueName);
        }
    }

    /**
     * Releases a distribution package if it's a {@link SharedDistributionPackage}, otherwise deletes it.
     * @param distributionPackage a distribution package
     * @param queueName the name of the queue from which it should be eventually released
     */
    public static void releaseOrDelete(DistributionPackage distributionPackage, String queueName) {
        if (distributionPackage instanceof SharedDistributionPackage) {
            if (queueName != null) {
                ((SharedDistributionPackage) distributionPackage).release(queueName);
                log.debug("package {} released from queue {}", distributionPackage.getId(), queueName);
            } else {
                log.error("package {} cannot be released from null queue", distributionPackage.getId());
            }
        } else {
            deleteSafely(distributionPackage);
            log.debug("package {} deleted", distributionPackage.getId());
        }
    }

    /**
     * Delete a distribution package, if deletion fails, ignore it
     * @param distributionPackage the package to delete
     */
    public static void deleteSafely(DistributionPackage distributionPackage) {
        if (distributionPackage != null) {
            try {
                distributionPackage.delete();
            } catch (Throwable t) {
                log.error("error deleting package", t);
            }
        }
    }

    public static void closeSafely(DistributionPackage distributionPackage) {
        if (distributionPackage != null) {
            try {
                distributionPackage.close();
            } catch (Throwable t) {
                log.error("error closing package", t);
            }
        }
    }

    /**
     * Create a queue item out of a package
     * @param distributionPackage a distribution package
     * @return a distribution queue item
     */
    public static DistributionQueueItem toQueueItem(DistributionPackage distributionPackage) {
        return new DistributionQueueItem(distributionPackage.getId(), distributionPackage.getInfo());
    }

    /**
     * Create a {@link DistributionPackageInfo} from a queue item
     * @param queueItem a distribution queue item
     * @return a {@link DistributionPackageInfo}
     */
    public static DistributionPackageInfo fromQueueItem(DistributionQueueItem queueItem) {
        String type = queueItem.get(DistributionPackageInfo.PROPERTY_PACKAGE_TYPE, String.class);
        return new DistributionPackageInfo(type, queueItem);
    }

    public static String getQueueName(DistributionPackageInfo packageInfo) {
        return packageInfo.get(PACKAGE_INFO_PROPERTY_ORIGIN_QUEUE, String.class);
    }

    public static void mergeQueueEntry(DistributionPackageInfo packageInfo, DistributionQueueEntry entry) {
        packageInfo.putAll(entry.getItem());
        packageInfo.put(PACKAGE_INFO_PROPERTY_ORIGIN_QUEUE, entry.getStatus().getQueueName());
    }


    public static void fillInfo(DistributionPackageInfo info, DistributionRequest request) {
        info.put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, request.getRequestType());
        info.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, request.getPaths());
        info.put(DistributionPackageInfo.PROPERTY_REQUEST_DEEP_PATHS, getDeepPaths(request));
    }

    public static String[] getDeepPaths(DistributionRequest request) {
        List<String> deepPaths = new ArrayList<String>();
        for (String path : request.getPaths()) {
            if (request.isDeep(path)) {
                deepPaths.add(path);
            }
        }

        return deepPaths.toArray(new String[0]);
    }

}
