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

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.SharedDistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Package related utility methods
 */
public class DistributionPackageUtils {

    static Logger log = LoggerFactory.getLogger(DistributionPackageUtils.class);

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
        return new DistributionPackageInfo(queueItem);
    }

}
