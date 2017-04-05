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
package org.apache.sling.models.impl;

import javax.annotation.Nonnull;

/**
 * This class encapsulates a value of a generic class in case of success or the
 * {@link RuntimeException}s in case of an error. It is used because the
 * different instantiation methods for models don't all allow exceptions to be
 * thrown. Also throwing and catching exceptions would decrease the runtime performance.
 * Therefore this class is used to throw the exception only if necessary.
 */
public class Result<SuccessObjectType> {
    private final RuntimeException t;
    private final SuccessObjectType object;

    /**
     * instantiate with one throwable (i.e. failure)
     * 
     * @param throwable
     */
    public Result(RuntimeException throwable) {
        this.t = throwable;
        this.object = null;
    }

    /**
     * instantate with a model (i.e. success)
     * 
     * @param object
     */
    public Result(SuccessObjectType object) {
        this.object = object;
        this.t = null;
    }

    /**
     * 
     * @return the encapsulated exception
     * @throws IllegalStateException
     *             in case this object does not represent a failure
     */
    public @Nonnull RuntimeException getThrowable() {
        if (t == null) {
            throw new IllegalStateException("No throwable available");
        }
        return t;
    }

    /**
     * 
     * @return the encapsulated success value
     * @throws IllegalStateException
     *             in case this object does not represent a success
     */
    public @Nonnull SuccessObjectType getValue() {
        if (object == null) {
            throw new IllegalStateException(
                    "Success object is not set, but rather an exception is encapsulated: " + t.getMessage(), t);
        }
        return object;
    }

    /**
     * 
     * @return {@code true} in case this object represents a success, otherwise
     *         {@code false}
     */
    public boolean wasSuccessful() {
        return object != null;
    }

}
