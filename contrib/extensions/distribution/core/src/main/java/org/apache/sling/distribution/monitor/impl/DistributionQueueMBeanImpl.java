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
package org.apache.sling.distribution.monitor.impl;

import java.util.Calendar;

import org.apache.sling.distribution.queue.DistributionQueue;

/**
 * Default implementation of {@link DistributionQueueMBean}
 */
public final class DistributionQueueMBeanImpl implements DistributionQueueMBean {

    private final DistributionQueue distributionQueue;

    public DistributionQueueMBeanImpl(DistributionQueue distributionQueue) {
        this.distributionQueue = distributionQueue;
    }

    @Override
    public String getName() {
        return distributionQueue.getName();
    }

    @Override
    public String getType() {
        return distributionQueue.getType().name().toLowerCase();
    }

    @Override
    public int getSize() {
        return distributionQueue.getStatus().getItemsCount();
    }

    @Override
    public boolean isEmpty() {
        return distributionQueue.getStatus().isEmpty();
    }

    @Override
    public String getState() {
        return distributionQueue.getStatus().getState().name().toLowerCase();
    }

    @Override
    public String getHeadId() {
        if (distributionQueue.getHead() != null) {
            return distributionQueue.getHead().getId();
        }
        return null;
    }

    @Override
    public int getHeadDequeuingAttempts() {
        if (distributionQueue.getHead() != null) {
            return distributionQueue.getHead().getStatus().getAttempts();
        }
        return -1;
    }

    @Override
    public String getHeadStatus() {
        if (distributionQueue.getHead() != null) {
            return distributionQueue.getHead().getStatus().getItemState().name().toLowerCase();
        }
        return null;
    }

    @Override
    public Calendar getHeadEnqueuingDate() {
        if (distributionQueue.getHead() != null) {
            return distributionQueue.getHead().getStatus().getEntered();
        }
        return null;
    }

}
