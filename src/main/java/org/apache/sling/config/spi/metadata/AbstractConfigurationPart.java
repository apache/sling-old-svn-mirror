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
package org.apache.sling.config.spi.metadata;

import java.util.HashMap;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Defines a configuration set.
 */
@ProviderType
abstract class AbstractConfigurationPart implements ConfigurationPart {

    private final String name;
    private final ValueMap properties = new ValueMapDecorator(new HashMap<String, Object>());

    public AbstractConfigurationPart(@Nonnull String name) {
        if (name == null || !ConfigurationPart.NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
        this.name = name;
    }
    
    /**
     * @return Parameter name
     */
    public @Nonnull String getName() {
        return this.name;
    }

    /**
     * @return Further properties for documentation and configuration of
     *         behavior in configuration editor.
     */
    public @Nonnull ValueMap getProperties() {
        return this.properties;
    }
    
    @Override
    public String toString() {
        return this.name;
    }

}
