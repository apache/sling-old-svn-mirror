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
package org.apache.sling.testing.mock.osgi;

import static org.apache.sling.testing.mock.osgi.MapUtil.toDictionary;
import static org.apache.sling.testing.mock.osgi.MapUtil.toMap;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.OsgiMetadata;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Map util merge methods.
 */
final class MapMergeUtil {

    private MapMergeUtil() {
        // static methods only
    }
    
    /**
     * Merge service properties from three sources (with this precedence):
     * 1. Properties defined in calling unit test code
     * 2. Properties from ConfigurationAdmin
     * 3. Properties from OSGi SCR metadata
     * @param target Target service
     * @param configAdmin Configuration admin or null if none is registered
     * @param properties Properties from unit test code or null if none where passed
     * @return Merged properties
     */
    static Dictionary<String, Object> propertiesMergeWithOsgiMetadata(Object target, 
            ConfigurationAdmin configAdmin, 
            Dictionary<String, Object> properties) {
        return toDictionary(propertiesMergeWithOsgiMetadata(target, configAdmin, toMap(properties)));
    }
    
    /**
     * Merge service properties from three sources (with this precedence):
     * 1. Properties defined in calling unit test code
     * 2. Properties from ConfigurationAdmin
     * 3. Properties from OSGi SCR metadata
     * @param target Target service
     * @param configAdmin Configuration admin or null if none is registered
     * @param properties Properties from unit test code or null if none where passed
     * @return Merged properties
     */
    static Map<String, Object> propertiesMergeWithOsgiMetadata(Object target,
            ConfigurationAdmin configAdmin,
            Map<String, Object> properties) {
        Map<String, Object> mergedProperties = new HashMap<String, Object>();
        
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(target.getClass());
        if (metadata != null) {
            Map<String,Object> metadataProperties = metadata.getProperties();
            if (metadataProperties != null) {
                mergedProperties.putAll(metadataProperties);

                // merge with configuration from config admin
                if (configAdmin != null) {
                    Object pid = metadataProperties.get(Constants.SERVICE_PID);
                    if (pid != null) {
                        try {
                            Configuration config = configAdmin.getConfiguration(pid.toString());
                            mergedProperties.putAll(toMap(config.getProperties()));
                        }
                        catch (IOException ex) {
                            throw new RuntimeException("Unable to read config for pid " + pid, ex);
                        }
                    }
                }
            }
        }
        
        // merge with properties from calling unit test code
        if (properties != null) {
            mergedProperties.putAll(properties);
        }
        
        return mergedProperties;
    }
    
}
