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

/**
 * The MBean representation of a {@code DistributionQueue}.
 */
public interface DistributionQueueMBean {

    /**
     * Get this queue name
     *
     * @return the queue name
     */
    String getName();

    /**
     * Get the type of this queue.
     *
     * @return the type
     */
    String getType();

    /**
     * Get the current size of the queue.
     *
     * @return the current size of queue
     */
    int getSize();

    /**
     * Check if the queue is empty
     *
     * @return {@code true} if the queue is empty, {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * Get the state associated with the status of this queue
     *
     * @return the state associated with the status of this queue
     */
    String getState();

    /**
     * Get the first item ID (in a FIFO strategy, the next to be processed) from the queue.
     *
     * @return first item into the queue or {@code null} if the queue is empty
     */
    String getHeadId();

    /**
     * Get the first item (in a FIFO strategy, the next to be processed) dequeuing attempts from the queue.
     *
     * @return first item dequeuing attempts or {@code -1} if the queue is empty
     */
    int getHeadDequeuingAttempts();

    /**
     * Get the first item (in a FIFO strategy, the next to be processed) state.
     *
     * @return the first item status or {@code null} if the queue is empty
     */
    String getHeadStatus();

    /**
     * Get the first item (in a FIFO strategy, the next to be processed) date time when joined the queue.
     *
     * @return the first item date time when joined the queue or {@code null} if the queue is empty
     */
    Calendar getHeadEnqueuingDate();

}
