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
package org.apache.sling.validation.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.validation.api.ChildResource;
import org.apache.sling.validation.api.ResourceProperty;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.impl.util.ResourceValidationBuilder;

/**
 * Implements a {@link ChildResource}
 */
public class ChildResourceImpl implements ChildResource {

    private final String name;
    private final Pattern namePattern;
    private final @Nonnull Set<ResourceProperty> properties;
    private final @Nonnull List<ChildResource> children;
    private final boolean isRequired;

    public ChildResourceImpl(@Nonnull Resource modelResource, @Nonnull Resource childResource, @Nonnull Map<String, Validator<?>> validatorsMap, @Nonnull List<ChildResource> children) {
        String root = modelResource.getPath();
        if (!childResource.getPath().startsWith(root)) {
            throw new IllegalArgumentException("Expected resource " + childResource.getPath() + " to be under root path " + root);
        }
        // if pattern is set, always use that
        ValueMap childrenProperties = childResource.adaptTo(ValueMap.class);
        if (childrenProperties == null) {
            throw new IllegalStateException("Could not adapt resource " + childResource.getPath() + " to ValueMap");
        }
        if (childrenProperties.containsKey(Constants.NAME_REGEX)) {
            name = null;
            String regex = childrenProperties.get(Constants.NAME_REGEX, String.class);
            try {
                namePattern = Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Given regular expression is invalid: " + regex);
            }
        } else {
            // otherwise fall back to the name
            name = childResource.getName();
            namePattern = null;
        }
        isRequired = !PropertiesUtil.toBoolean(childrenProperties.get(Constants.OPTIONAL), false);
        properties = ResourceValidationBuilder.buildProperties(validatorsMap, childResource.getChild(Constants.PROPERTIES));
        this.children = children;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @Nonnull Set<ResourceProperty> getProperties() {
        return properties;
    }

    @Override
    public Pattern getNamePattern() {
        return namePattern;
    }
    
    public @Nonnull List<ChildResource> getChildren() {
        return children;
    }

    @Override
    public boolean isRequired() {
        return isRequired;
    }
}
