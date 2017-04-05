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
package org.apache.sling.caconfig.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.management.multiplexer.ConfigurationResourceResolvingStrategyMultiplexer;
import org.apache.sling.caconfig.management.multiplexer.ContextPathStrategyMultiplexer;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.caconfig.resource.impl.util.ConfigNameUtil;
import org.apache.sling.caconfig.resource.spi.ContextResource;
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
        ConfigNameUtil.ensureValidConfigName(configName);
        return configurationResourceResolvingStrategy.getResource(resource, Collections.singleton(bucketName), configName);
    }

    @Override
    public Collection<Resource> getResourceCollection(Resource resource, String bucketName, String configName) {
        ConfigNameUtil.ensureValidConfigName(configName);
        return configurationResourceResolvingStrategy.getResourceCollection(resource, Collections.singleton(bucketName), configName);
    }

    @Override
    public String getContextPath(Resource resource) {
        Iterator<ContextResource> it = contextPathStrategy.findContextResources(resource);
        if (it.hasNext()) {
            return it.next().getResource().getPath();
        }
        else {
            return null;
        }
    }

    @Override
    public Collection<String> getAllContextPaths(Resource resource) {
        final List<String> contextPaths = new ArrayList<>();
        Iterator<ContextResource> contextResources = contextPathStrategy.findContextResources(resource);
        while (contextResources.hasNext()) {
            contextPaths.add(contextResources.next().getResource().getPath());
        }
        return contextPaths;
    }
    
}
