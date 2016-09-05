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
package org.apache.sling.contextaware.config.spi.metadata;

import java.util.Map;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Common properties for configuration and properties.
 */
@ProviderType
abstract class AbstractMetadata {

    private final String name;
    private String label;
    private String description;
    private Map<String,String> properties;

    public AbstractMetadata(@Nonnull String name) {
        if (name == null) {
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
     * @return Label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label Label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description Description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return Further properties for documentation and configuration of behavior in configuration editor.
     */
    public Map<String,String> getProperties() {
        return this.properties;
    }
    
    /**
     * @param properties Further properties for documentation and configuration of behavior in configuration editor.
     */
    public void setProperties(Map<String,String> properties) {
        this.properties = properties;
    }
    
    @Override
    public String toString() {
        return this.name;
    }

}
