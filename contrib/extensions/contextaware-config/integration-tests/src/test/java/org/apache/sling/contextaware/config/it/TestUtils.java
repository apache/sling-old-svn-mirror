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
package org.apache.sling.contextaware.config.it;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TestUtils {
    
    /**
     * Root path for test content
     */
    public static final String CONTENT_ROOT_PATH = "/content/test";
    
    /**
     * Root path for config content
     */
    public static final String CONFIG_ROOT_PATH = "/conf/test";
    
    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);
    
    private TestUtils() {
        // static methods only
    }
    
    public static void cleanUp(ResourceResolver resourceResolver) {
        deletePath(resourceResolver, CONTENT_ROOT_PATH);
        deletePath(resourceResolver, CONFIG_ROOT_PATH);
        try {
            resourceResolver.commit();
        }
        catch (PersistenceException ex) {
            log.error("Unable clean up resources.", ex);
        }
    }

    public static void deletePath(ResourceResolver resourceResolver, String path) {
        Resource resource = resourceResolver.getResource(path);
        if (resource != null) {
            try {
                resourceResolver.delete(resource);
            }
            catch (PersistenceException ex) {
                log.error("Unable to delete resource " + path, ex);
            }
        }
    }
}
