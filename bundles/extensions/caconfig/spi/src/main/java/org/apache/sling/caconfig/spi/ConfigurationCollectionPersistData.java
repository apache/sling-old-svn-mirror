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
package org.apache.sling.caconfig.spi;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Data for persisting configuration collections.
 */
@ProviderType
public final class ConfigurationCollectionPersistData {
    
    private static final Pattern ITEM_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private final Collection<ConfigurationPersistData> items;
    private Map<String,Object> properties;
    
    /**
     * @param items Collection of configuration collection items
     */
    public ConfigurationCollectionPersistData(@Nonnull Collection<ConfigurationPersistData> items) {
        validateItems(items);
        this.items = items;
    }
    
    private void validateItems(Collection<ConfigurationPersistData> items) {
        // ensure unique and valid key names        
        Set<String> keyNames = new HashSet<>();
        for (ConfigurationPersistData item : items) {
            if (StringUtils.isBlank(item.getCollectionItemName())) {
                throw new ConfigurationPersistenceException("Collection item name is missing.");
            }
            if (!ITEM_NAME_PATTERN.matcher(item.getCollectionItemName()).matches()) {
                throw new ConfigurationPersistenceException("Invalid collection item name: " + item.getCollectionItemName());
            }
            if (keyNames.contains(item.getCollectionItemName())) {
                throw new ConfigurationPersistenceException("Duplicate collection item name: " + item.getCollectionItemName());
            }
            keyNames.add(item.getCollectionItemName());
        }
    }

    /**
     * @return Collection of configuration collection items
     */
    public @Nonnull Collection<ConfigurationPersistData> getItems() {
        return items;
    }
    
    /**
     * @return Properties for the configuration collection itself. Does not contain configuration data, but control data e.g. for enabling collection inheritance.
     */
    public @CheckForNull Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * @param value Properties for the configuration collection itself. Does not contain configuration data, but control data e.g. for enabling collection inheritance.
     * @return this
     */
    public ConfigurationCollectionPersistData properties(Map<String, Object> value) {
        this.properties = value;
        return this;
    }

}
