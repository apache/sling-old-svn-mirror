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
package org.apache.sling.contextaware.config.management.impl;

import java.util.Collection;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.contextaware.config.spi.ConfigurationPersistenceStrategy;

public class CustomConfigurationPersistenceStrategy implements ConfigurationPersistenceStrategy {
 
    @Override
    public Resource getResource(Resource resource) {
        return resource.getChild("jcr:content");
    }

    @Override
    public boolean persist(ResourceResolver resourceResolver, String configResourcePath, Map<String,Object> properties) {
        // TODO: implement persistence
        return false;
    }

    @Override
    public boolean persistCollection(ResourceResolver resourceResolver, String configResourceCollectionParentPath,
            Collection<Map<String,Object>> propertiesCollection) {
        // TODO: implement persistence
        return false;
    }

}
