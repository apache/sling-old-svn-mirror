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
package org.apache.sling.distribution.communication;

/**
 * The different states a {@link org.apache.sling.distribution.communication.DistributionRequest} can have during its lifecycle.
 * Allowed transitions of {@link org.apache.sling.distribution.communication.DistributionRequestState} for a certain
 * {@link org.apache.sling.distribution.communication.DistributionRequest} are:
 * {@code #SUCCEEDED} -> ø
 * {@code #FAILED} -> ø
 * {@code #ACCEPTED} -> {@code #FAILED}
 * {@code #ACCEPTED} -> {@code #SUCCEEDED}
 * <p/>
 * {@link org.apache.sling.distribution.communication.DistributionRequest}s against synchronous {@link org.apache.sling.distribution.agent.DistributionAgent}s
 * will only results in {@code #SUCCEEDED} or {@code #FAILED} {@link org.apache.sling.distribution.communication.DistributionRequestState}s
 * while requests against asynchronous agents can result in any of {@code #SUCCEEDED}, {@code #FAILED} or {@code #ACCEPTED} states.
 */
public enum DistributionRequestState {

    /**
     * A {@link org.apache.sling.distribution.communication.DistributionRequest} has succeeded when the content has been
     * successfully distributed (created, transported and persisted) from the source instance to the target instance.
     */
    SUCCEEDED,

    /**
     * A {@link org.apache.sling.distribution.communication.DistributionRequest} has failed when the content cannot be
     * successfully distributed from the source instance to target instance, this means the request execution failed during
     * one of: creation, transport, persistence.
     */
    FAILED,

    /**
     * A {@link org.apache.sling.distribution.communication.DistributionRequest} has been accepted when the content to be
     * distributed has been successfully created, but not yet either transported or persisted correctly to the target instance.
     */
    ACCEPTED

}
