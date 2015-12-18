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
package org.apache.sling.validation.impl;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.spi.ValidationContext;

public class DefaultValidationContext implements ValidationContext {

    private final @Nonnull String location;
    private final @Nonnull ValueMap valueMap;
    private final Resource resource;

    public DefaultValidationContext(@Nonnull String location, @Nonnull ValueMap valueMap, Resource resource) {
        super();
        this.location = location;
        this.valueMap = valueMap;
        this.resource = resource;
    }

    @Override
    @Nonnull
    public String getLocation() {
        return location;
    }

    @Override
    @Nonnull
    public ValueMap getValueMap() {
        return valueMap;
    }

    @Override
    @CheckForNull
    public Resource getResource() {
        return resource;
    }

}
