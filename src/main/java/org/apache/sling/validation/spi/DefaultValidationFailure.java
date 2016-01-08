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
import java.util.ResourceBundle;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.validation.ValidationFailure;

/**
 * Wraps a message key (being looked up in a {@link ResourceBundle}), messageArguments (being used with {@link MessageFormat#format(String, Object...)}
 * and the location where the validation failure occurred.
 */
public class DefaultValidationFailure implements ValidationFailure {

    private final @Nonnull String location;
    private final @Nonnull String messageKey;
    private final Object[] messageArguments;

    /**
     * Constructor of a validation failure. The message is constructed by looking up the given messageKey from a resourceBundle.
     * and formatting it using the given messageArguments via {@link MessageFormat#format(String, Object...)}.
     * @param location the location
     * @param messageKey the key to look up in the resource bundle
     * @param messageArguments the arguments to be used with the looked up value from the resource bundle (given in {@link #getMessage(ResourceBundle)}
     */
    public DefaultValidationFailure(@Nonnull String location, @Nonnull String messageKey, Object... messageArguments) {
        this.location = location;
        this.messageKey = messageKey;
        this.messageArguments = messageArguments;
    }

    @SuppressWarnings("null")
    @Override
    public @Nonnull String getMessage(@Nonnull ResourceBundle resourceBundle) {
        return MessageFormat.format(resourceBundle.getString(messageKey), messageArguments);
    }

    @Override
    public @Nonnull String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "DefaultValidationFailure [location=" + location + ", messageKey=" + messageKey + ", messageArguments=" + StringUtils.join(messageArguments) + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + Arrays.hashCode(messageArguments);
        result = prime * result + ((messageKey == null) ? 0 : messageKey.hashCode());
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
        DefaultValidationFailure other = (DefaultValidationFailure) obj;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        if (!Arrays.equals(messageArguments, other.messageArguments))
            return false;
        if (messageKey == null) {
            if (other.messageKey != null)
                return false;
        } else if (!messageKey.equals(other.messageKey))
            return false;
        return true;
    }

}
