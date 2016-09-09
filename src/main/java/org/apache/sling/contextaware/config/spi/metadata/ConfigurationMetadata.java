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

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Defines a configuration.
 */
@ProviderType
public final class ConfigurationMetadata extends AbstractMetadata {

    private boolean isList;
    private List<PropertyMetadata<?>> propertyMetadata;

    /**
     * @param name Configuration name
     */
    public ConfigurationMetadata(@Nonnull String name) {
        super(name);
    }
    
    /**
     * @return true if configuration is singleton
     */
    public boolean isSingleton() {
        return !isList;
    }
    
    /**
     * @return true if configuration is list
     */
    public boolean isList() {
        return isList;
    }

    /**
     * @param isList true if configuration is list
     */
    public void setList(boolean isList) {
        this.isList = isList;
    }

    /**
     * @return Configuration properties
     */
    public Collection<PropertyMetadata<?>> getPropertyMetadata() {
        return this.propertyMetadata;
    }

    /**
     * @param propertyMetadata Configuration properties
     */
    public void setPropertyMetadata(List<PropertyMetadata<?>> propertyMetadata) {
        this.propertyMetadata = propertyMetadata;
    }

}
