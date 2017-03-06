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

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.annotation.Nonnull;

import org.apache.sling.validation.ValidationFailure;

/**
 * Wraps a message key (being looked up in a {@link ResourceBundle}), messageArguments (being used with {@link MessageFormat#format(String, Object...)}
 * and the location where the validation failure occurred.
 */
public class DefaultValidationFailure implements ValidationFailure, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -1748031688917555982L;
    private final @Nonnull String location;
    private final @Nonnull String messageKey;
    private final Object[] messageArguments;
    private final transient @Nonnull ResourceBundle defaultResourceBundle;
    private final int severity;

    /**
    * Constructor of a validation failure.
    * @param validationContext the context from which to extract location, severity and default resource bundle
    * @param messageKey the key to look up in the resource bundle
    * @param messageArguments the arguments to be used with the looked up value from the resource bundle (given in {@link #getMessage(ResourceBundle)}
    */
   public DefaultValidationFailure(@Nonnull ValidationContext validationContext, @Nonnull String messageKey, Object... messageArguments) {
       this.location = validationContext.getLocation();
       this.severity = validationContext.getSeverity();
       this.defaultResourceBundle = validationContext.getDefaultResourceBundle();
       this.messageKey = messageKey;
       this.messageArguments = messageArguments;
   }

    /**
     * Constructor of a validation failure.
     * @param location the location where the validation error occured
     * @param severity the severity of this failure (may be {@code null}), which leads to setting it to the {@link #DEFAULT_SEVERITY}
     * @param defaultResourceBundle the default resourceBundle which is used to resolve the {@link messageKey} in {@link #getMessage(ResourceBundle)}
     *  if {@code null} is provided as parameter.
     * @param messageKey the key to look up in the resource bundle
     * @param messageArguments the arguments to be used with the looked up value from the resource bundle (given in {@link #getMessage(ResourceBundle)}
     */
    public DefaultValidationFailure(@Nonnull String location, int severity, @Nonnull ResourceBundle defaultResourceBundle, @Nonnull String messageKey, Object... messageArguments) {
        this.location = location;
        this.severity = severity;
        this.messageKey = messageKey;
        this.messageArguments = messageArguments;
        this.defaultResourceBundle = defaultResourceBundle;
    }

    @SuppressWarnings({ "null", "unused" })
    @Override
    public @Nonnull String getMessage(ResourceBundle resourceBundle) {
        if (resourceBundle == null) {
            resourceBundle = defaultResourceBundle;
        }
        if (resourceBundle == null) {
            // this should only happen if this class was deserialized because there the default resource bundle is missing
            return "No defaultResourceBundle found to resolve, messageKey = " + messageKey + ", messageArguments: " + Arrays.toString(messageArguments);
        }
        return MessageFormat.format(resourceBundle.getString(messageKey), messageArguments);
    }

    @Override
    public @Nonnull String getLocation() {
        return location;
    }

    @Override
    public int getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return "DefaultValidationFailure [location=" + location + ", messageKey=" + messageKey + ", messageArguments="
                + Arrays.toString(messageArguments) + ", severity=" + severity + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + Arrays.hashCode(messageArguments);
        result = prime * result + ((messageKey == null) ? 0 : messageKey.hashCode());
        result = prime * result + severity;
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
        if (!location.equals(other.location))
            return false;
        if (!Arrays.equals(messageArguments, other.messageArguments))
            return false;
        if (!messageKey.equals(other.messageKey))
            return false;
        if (severity != other.severity)
            return false;
        return true;
    }

}
