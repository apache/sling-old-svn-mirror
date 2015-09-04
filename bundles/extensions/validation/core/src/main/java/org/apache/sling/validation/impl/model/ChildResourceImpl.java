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
package org.apache.sling.validation.impl.model;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nonnull;

import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ResourceProperty;

/**
 * Implements a {@link ChildResource}
 */
public class ChildResourceImpl implements ChildResource {

    private final String name;
    private final Pattern namePattern;
    private final @Nonnull List<ResourceProperty> properties;
    private final @Nonnull List<ChildResource> children;
    private final boolean isRequired;

    public ChildResourceImpl(@Nonnull String name, String nameRegex, boolean isRequired, @Nonnull List<ResourceProperty> properties, @Nonnull List<ChildResource> children) {
        if (nameRegex != null) {
            try {
                this.namePattern = Pattern.compile(nameRegex);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid regex given", e);
            }
        } else {
            this.namePattern = null;
        }
        this.name = name;
        this.isRequired = isRequired;
        this.properties = properties;
        this.children = children;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @Nonnull Collection<ResourceProperty> getProperties() {
        return properties;
    }

    @Override
    public Pattern getNamePattern() {
        return namePattern;
    }
    
    public @Nonnull Collection<ChildResource> getChildren() {
        return children;
    }

    @Override
    public boolean isRequired() {
        return isRequired;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((children == null) ? 0 : children.hashCode());
        result = prime * result + (isRequired ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((namePattern == null) ? 0 : namePattern.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ChildResourceImpl other = (ChildResourceImpl) obj;
        if (!children.equals(other.children))
            return false;
        if (isRequired != other.isRequired)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (namePattern == null) {
            if (other.namePattern != null)
                return false;
        } else if (!namePattern.pattern().equals(other.namePattern.pattern()))
            return false;
        if (!properties.equals(other.properties))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ChildResourceImpl [name=" + name + ", namePattern=" + namePattern + ", properties=" + properties
                + ", children=" + children + ", isRequired=" + isRequired + "]";
    }
}
