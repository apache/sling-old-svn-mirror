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
package org.apache.sling.contextaware.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.resource.ConfigurationResourceResolver;
import org.osgi.service.component.annotations.Component;

@Component(service=ConfigurationResourceResolver.class)
public class ConfigurationResourceResolverImpl implements ConfigurationResourceResolver {

    @Override
    public Resource getResource(Resource resource, String configName) {
        // TODO: this is only a dummy implementation
        String configPath = "/conf" + getContextPath(resource) + "/" + configName;
        return resource.getResourceResolver().getResource(configPath);
    }

    @Override
    public Collection<Resource> getResourceList(Resource resource, String configName) {
        // TODO: this is only a dummy implementation
        String configPath = "/conf" + getContextPath(resource) + "/" + configName;
        Resource configResource = resource.getResourceResolver().getResource(configPath);
        if (configResource != null) {
            return StreamSupport.stream(configResource.getChildren().spliterator(), false)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public String getContextPath(Resource resource) {
        // TODO: this is only a dummy implementation
        String[] pathParts = resource.getPath().split("/");
        return "/" + pathParts[1] + "/" + pathParts[2];
    }

    @Override
    public List<String> getAllContextPaths(Resource resource) {
        // TODO: this is only a dummy implementation
        List<String> items = new ArrayList<String>();
        items.add(getContextPath(resource));
        return items;
    }

}
