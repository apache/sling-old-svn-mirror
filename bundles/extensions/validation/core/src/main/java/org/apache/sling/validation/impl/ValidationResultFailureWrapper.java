/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;

/**
 * Wrapper around {@link ValidationResult} which will wrap all enclosed {@link ValidationFailure}s with the help of the given {@link ValidationFailureWrapperFactory}.
 *
 * @param <T> the wrapper class of ValidationFailure which is created by the given ValidationFailureWrapperFactory.
 */
public class ValidationResultFailureWrapper<T extends ValidationFailure> implements ValidationResult {

    private final ValidationResult delegate;
    private final String parameter;
    private final ValidationFailureWrapperFactory<T> failureWrapperFactory;

    public ValidationResultFailureWrapper(ValidationResult delegate, String parameter, ValidationFailureWrapperFactory<T> failureWrapperFactory) {
        this.delegate = delegate;
        this.parameter = parameter;
        this.failureWrapperFactory = failureWrapperFactory;
    }

    @Override
    public boolean isValid() {
        return delegate.isValid();
    }

    @Override
    @Nonnull
    public List<ValidationFailure> getFailures() {
        // go through all failures
        List<ValidationFailure> failures = new LinkedList<ValidationFailure>();
        for (ValidationFailure failure : delegate.getFailures()) {
           failures.add(failureWrapperFactory.createWrapper(failure, parameter));
        }
        return failures;
    }

    @Override
    public String toString() {
        return "ValidationResultFailureWrapper [delegate=" + delegate + ", parameter=" + parameter
                + ", failureWrapperFactory=" + failureWrapperFactory + "]";
    }

}
