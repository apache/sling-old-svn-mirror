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
 * Result returned by a {@link UseProvider}.
 */
public final class ProviderOutcome {

    // a generic failure without a cause returned by #failure()
    private static final ProviderOutcome GENERIC_FAILURE = new ProviderOutcome(false, null, null);

    // whether this is a success or failure
    private final boolean success;

    // the result value in case of success (may be null)
    private final Object result;

    // the reason for failure in case of failure (may be null)
    private final Throwable cause;

    /**
     * Creates an outcome instance
     *
     * @param success {@code true} to indicate success or {@code false} to indicate failure
     * @param result  optional result value in case of success, may be {@code null}
     * @param cause   optional cause in case of failure, may be {@code null}
     */
    private ProviderOutcome(boolean success, Object result, Throwable cause) {
        this.success = success;
        this.result = result;
        this.cause = cause;
    }

    /**
     * Create a successful outcome
     *
     * @param result the result
     * @return a successful result
     */
    public static ProviderOutcome success(Object result) {
        return new ProviderOutcome(true, result, null);
    }

    /**
     * Create a failed outcome without a specific {@link #getCause() cause}. This method must be used for creating outcomes that don't
     * signal an error but rather the fact that the {@link UseProvider} is not capable of fulfilling the request.
     *
     * @return a failed outcome
     */
    public static ProviderOutcome failure() {
        return GENERIC_FAILURE;
    }

    /**
     * Create a failed outcome with the given {@link #getCause() cause}. This method must be used when the {@link UseProvider} is
     * capable of fulfilling the request but an error condition prevents the provider from doing so.
     *
     * @param cause The reason for this failure, which may be {@code null}
     * @return a failed outcome
     */
    public static ProviderOutcome failure(Throwable cause) {
        return new ProviderOutcome(false, null, cause);
    }

    /**
     * If the given obj is not {@code null} return a {@link #success(Object) successful outcome}, with the given result. Otherwise, return
     * {@link #failure()}.
     *
     * @param obj the result
     * @return an outcome based on whether the parameter is null or not
     */
    public static ProviderOutcome notNullOrFailure(Object obj) {
        return (obj == null) ? failure() : success(obj);
    }

    /**
     * Check if the outcome has been successful
     *
     * @return the outcome success status
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Check whether the outcome is a failure
     *
     * @return the outcome failure status
     */
    public boolean isFailure() {
        return !isSuccess();
    }

    /**
     * Get the result in this outcome.
     *
     * @return the result of the container
     * @throws IllegalStateException if the outcome is a failure
     */
    public Object getResult() {
        if (!success) {
            throw new IllegalStateException("Outcome has not been successful");
        }
        return result;
    }

    /**
     * Returns the cause for this failure outcome or {@code null} if this outcome is a success or no cause has been defined with the
     * {@link #failure(Throwable)} method.
     *
     * @return the cause for this failure outcome.
     */
    public Throwable getCause() {
        return cause;
    }
}
