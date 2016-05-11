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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.spi.DefaultValidationResult;

/**
 * Aggregates multiple {@link ValidationResult}s.
 */
public class CompositeValidationResult implements ValidationResult {

    private final @Nonnull List<ValidationResult> results;

    public CompositeValidationResult() {
        results = new ArrayList<ValidationResult>();
    }

    public void addValidationResult(@Nonnull ValidationResult result) {
        results.add(result);
    }

    public void addFailure(@Nonnull String location, Integer severity, @Nonnull String message, Object... messageArguments) {
        results.add(new DefaultValidationResult(location, severity, message, messageArguments));
    }

    @Override
    public boolean isValid() {
        // this is only valid if all aggregated results are valid
        for (ValidationResult result : results) {
            if (!result.isValid()) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Nonnull
    public List<ValidationFailure> getFailures() {
        List<ValidationFailure> failures = new LinkedList<ValidationFailure>();
        for (ValidationResult result : results) {
            for (ValidationFailure failure : result.getFailures()) {
                failures.add(failure);
            }
        }
        return failures;
    }
}
