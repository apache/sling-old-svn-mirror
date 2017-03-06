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
package org.apache.sling.validation.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.validation.spi.Validator;

/**
 * This encapsulates a {@link Validator} as well as its default severity.
 *
 * @param <T> the type param of the encapsulated {@link Validator}
 */
public final class ValidatorAndSeverity<T> {
    private final @Nonnull Validator<T> validator;
    private final Integer severity;

    public ValidatorAndSeverity(@Nonnull Validator<T> validator, Integer severity) {
        this.validator = validator;
        this.severity = severity;
    }
    
    public @CheckForNull Integer getSeverity() {
        return severity;
    }

    public @Nonnull Validator<T> getValidator() {
        return validator;
    }

    @Override
    public String toString() {
        return "ValidatorWithSeverity [validator=" + validator + ", severity=" + severity + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((severity == null) ? 0 : severity.hashCode());
        result = prime * result + ((validator == null) ? 0 : validator.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ValidatorAndSeverity<?> other = (ValidatorAndSeverity<?>) obj;
        if (severity == null) {
            if (other.severity != null)
                return false;
        } else if (!severity.equals(other.severity))
            return false;
        if (!validator.equals(other.validator))
            return false;
        return true;
    }
    
}
