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

import javax.annotation.Nonnull;

import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.spi.DefaultValidationFailure;

/**
 * Wrapper around {@link ValidationFailure} which will overwrite the delegate's location in case it is {@code null}
 *
 */
public class LocationFallbackValidationFailureWrapper extends DefaultValidationFailure {

    private LocationFallbackValidationFailureWrapper(@Nonnull ValidationFailure delegate, String location) {
        super(delegate.getMessage(), delegate.getLocation() != null ? delegate.getLocation() : location);
    }
    
    

    @Override
    public String toString() {
        return "LocationFallbackValidationFailureWrapper [" + super.toString() + "]";
    }

    public static class Factory
            implements ValidationFailureWrapperFactory<LocationFallbackValidationFailureWrapper> {
        public Factory() {
        }

        @Override
        public LocationFallbackValidationFailureWrapper createWrapper(@Nonnull ValidationFailure delegate,
                String parameter) {
            return new LocationFallbackValidationFailureWrapper(delegate, parameter);
        }
    }
}
