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
package org.apache.sling.distribution.queue;

import java.util.Calendar;

/**
 * the current status of a certain item in a {@link DistributionQueue}
 */
public class DistributionQueueItemStatus {

    private final int attempts;

    private final DistributionQueueItemState state;

    private final Calendar entered;

    private final String queueName;

    public DistributionQueueItemStatus(Calendar entered, DistributionQueueItemState state, int attempts, String queueName) {
        this.entered = entered;
        this.state = state;
        this.attempts = attempts;
        this.queueName = queueName;
    }

    public DistributionQueueItemStatus(DistributionQueueItemState state, String queueName) {
        this(Calendar.getInstance(), state, 0, queueName);
    }

    public int getAttempts() {
        return attempts;
    }

    public DistributionQueueItemState getItemState() {
        return state;
    }

    public String getQueueName() {
        return queueName;
    }

    @Override
    public String toString() {
        return "{\"attempts\":\"" + attempts + "\",\"" + "\",\"" + "state\":\"" + state +
                "\",\"" + "queueName\":\"" + queueName + "\"}";
    }

    public Calendar getEntered() {
        return entered;
    }
}
