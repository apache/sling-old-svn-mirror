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
package org.apache.sling.distribution;

import aQute.bnd.annotation.ProviderType;

/**
 * The different states a {@link org.apache.sling.distribution.DistributionRequest} can have during its lifecycle.
 * Allowed transitions of {@link org.apache.sling.distribution.DistributionRequestState} for a certain
 * {@link org.apache.sling.distribution.DistributionRequest} are:
 * {@code #DISTRIBUTED} -> ø
 * {@code #DROPPED} -> ø
 * {@code #ACCEPTED} -> {@code #DROPPED}
 * {@code #ACCEPTED} -> {@code #DISTRIBUTED}
 * <p/>
 * {@link org.apache.sling.distribution.DistributionRequest}s executed synchronously
 * will only results in {@code #DISTRIBUTED} or {@code #DROPPED} {@link org.apache.sling.distribution.DistributionRequestState}s
 * while requests executed asynchronously can result in any of {@code #DISTRIBUTED}, {@code #DROPPED} or {@code #ACCEPTED} states.
 */
@ProviderType
public enum DistributionRequestState {

    /**
     * The request has completed and the content has been successfully distributed
     * (created, transported and persisted) from the source instance to the target instance.
     */
    DISTRIBUTED,

    /**
     * The request has been dropped and the content could not be successfully
     * distributed from the source to target instance because the request
     * execution failed during one of the phases: creation, queueing, transport, persistence.
     */
    DROPPED,

    /**
     * The request was not executed because no distribution agent was found to serve it.
     */
    NOT_EXECUTED,

    /**
     * The request has been accepted, as a consequence the content to be distributed
     * has been created and queued (and it will be eventually processed asynchronously).
     */
    ACCEPTED

}
