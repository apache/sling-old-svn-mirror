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

/**
 * Class representing criteria for queue items selection.
 */
public class DistributionQueueItemSelector {
    private final int skip;
    private final int limit;

    /**
     *
     * @param skip the number of items to skip
     * @param limit the maximum number of items to return. use -1 to return all items.
     */
    public DistributionQueueItemSelector(int skip, int limit) {
        this.skip = skip;
        this.limit = limit;
    }

    /**
     * @return the number of items to skip from the queue.
     */
    public int getSkip() {
        return skip;
    }

    /**
     *
     * @return return the maximum number of items to be selected.
     */
    public int getLimit() {
        return limit;
    }
}
