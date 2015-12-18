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

import javax.annotation.Nonnull;

import org.apache.sling.validation.ValidationFailure;

public class DefaultValidationFailure implements ValidationFailure {

    private final @Nonnull String message;
    private final @Nonnull String location;

    public DefaultValidationFailure(@Nonnull String message, @Nonnull String location) {
        this.message = message;
        this.location = location;
    }

    @Override
    public @Nonnull String getMessage() {
        return message;
    }

    @Override
    public @Nonnull String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "DefaultValidationFailure [message=" + message + ", location=" + location + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof DefaultValidationFailure))
            return false;
        DefaultValidationFailure other = (DefaultValidationFailure) obj;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        return true;
    }
}
