/*******************************************************************************
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
 ******************************************************************************/

package org.apache.sling.scripting.sightly.use;

/**
 * Result returned by a use provider
 */
public final class ProviderOutcome {

    private boolean success;
    private Object result;

    private static final ProviderOutcome FAILURE = new ProviderOutcome(false, null);

    /**
     * Create a successful outcome
     * @param result the result
     * @return a successful result
     */
    public static ProviderOutcome success(Object result) {
        return new ProviderOutcome(true, result);
    }

    /**
     * Create a failed outcome
     * @return a failed outcome
     */
    public static ProviderOutcome failure() {
        return FAILURE;
    }

    /**
     * If the given obj is not null return a successful outcome, with the given result.
     * Otherwise, return failure
     * @param obj the result
     * @return an outcome based on whether the parameter is null or not
     */
    public static ProviderOutcome notNullOrFailure(Object obj) {
        return (obj == null) ? failure() : success(obj);
    }

    private ProviderOutcome(boolean success, Object result) {
        this.success = success;
        this.result = result;
    }

    /**
     * Check if the outcome has been successful
     * @return the outcome success status
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Check whether the outcome is a failure
     * @return the outcome failure status
     */
    public boolean isFailure() {
        return !isSuccess();
    }

    /**
     * Get the result in this outcome
     *
     * @return the result of the container
     * @throws java.lang.UnsupportedOperationException if the outcome is a failure
     */
    public Object getResult() {
        if (!success) {
            throw new UnsupportedOperationException("Outcome has not been successful");
        }
        return result;
    }
}
