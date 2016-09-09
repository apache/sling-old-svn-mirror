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
package org.apache.sling.contextaware.config.impl.metadata;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;

class ConfigurationMapping {

    private final Class<?> configClass;
    private final String configName;
    private AtomicReference<ConfigurationMetadata> configMetadataRef = new AtomicReference<>(null);
    
    public ConfigurationMapping(Class<?> configClass) {
        this.configClass = configClass;
        this.configName = AnnotationClassParser.getConfigurationName(configClass);
    }
    
    public Class<?> getConfigClass() {
        return configClass;
    }
    
    public String getConfigName() {
        return configName;
    }

    public ConfigurationMetadata getConfigMetadata() {
        // thread-safe lazy initialization via atomic reference
        ConfigurationMetadata configMetadata = configMetadataRef.get();
        if (configMetadata == null) {
            configMetadata = AnnotationClassParser.buildConfigurationMetadata(configClass);
            if (configMetadataRef.compareAndSet(null, configMetadata)) {
                return configMetadata;
            }
            else { 
                return configMetadataRef.get();
            }
        }
        else {
            return configMetadata;
        }
    }
    
}
