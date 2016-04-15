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
package org.apache.sling.validation.spi;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;

public class DefaultValidationResult implements ValidationResult {

    private final boolean isValid;
    private final @Nonnull List<ValidationFailure> failures;

    private DefaultValidationResult(boolean isValid) {
        this.isValid = isValid;
        this.failures = Collections.emptyList();
    }

    /** 
     * Constructs a result with one failure message. The message is constructed by looking up the given messageKey from a resourceBundle.
     * and formatting it using the given messageArguments via {@link MessageFormat#format(String, Object...)}.
     * @param location the location.
     * @param severity the severity of the embedded failure (may be {@code null}), which leads to setting it to the {@link #DEFAULT_SEVERITY}.
     * @param messageKey the message key used for looking up a value in the resource bundle given in {@link ValidationFailure#getMessage(java.util.ResourceBundle)}.
     * @param messageArguments optional number of arguments being used in {@link MessageFormat#format(String, Object...)}
     */
    public DefaultValidationResult(@Nonnull String location, Integer severity, @Nonnull String messageKey, Object... messageArguments) {
        this.isValid = false;
        this.failures = Collections.<ValidationFailure>singletonList(new DefaultValidationFailure(location, severity, messageKey, messageArguments));
    }

    public DefaultValidationResult(ValidationFailure... failures) {
        this.isValid = false;
        this.failures = Arrays.asList(failures);
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    @Nonnull
    public List<ValidationFailure> getFailures() {
        return failures;
    }

    public static final @Nonnull DefaultValidationResult VALID = new DefaultValidationResult(true);
}
