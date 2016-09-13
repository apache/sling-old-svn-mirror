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
package org.apache.sling.contextaware.config.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service=ConfigurationResourceResolver.class, immediate=true)
public class ConfigurationResourceResolverImpl implements ConfigurationResourceResolver {

    @Reference
    private ContextPathStrategyMultiplexer contextPathStrategy;
    
    @Reference
    private ConfigurationResourceResolvingStrategyMultiplexer configurationResourceResolvingStrategy;

    @Override
    public Resource getResource(Resource resource, String bucketName, String configName) {
        return configurationResourceResolvingStrategy.getResource(resource, bucketName, configName);
    }

    @Override
    public Collection<Resource> getResourceCollection(Resource resource, String bucketName, String configName) {
        return configurationResourceResolvingStrategy.getResourceCollection(resource, bucketName, configName);
    }

    @Override
    public String getContextPath(Resource resource) {
        Iterator<Resource> it = contextPathStrategy.findContextResources(resource).iterator();
        if (it.hasNext()) {
            return it.next().getPath();
        }
        else {
            return null;
        }
    }

    @Override
    public Collection<String> getAllContextPaths(Resource resource) {
        final List<String> contextPaths = new ArrayList<>();
        Collection<Resource> contextResources = contextPathStrategy.findContextResources(resource);
        for (Resource contextResource : contextResources) {
            contextPaths.add(contextResource.getPath());
        }
        return contextPaths;
    }

}
