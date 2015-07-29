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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ResourceProperty;

public class ChildResourceBuilder {

    public boolean optional;
    public boolean multiple;
    String nameRegex;
    private final List<ResourceProperty> resourceProperties;
    private final List<ChildResource> children;

    public ChildResourceBuilder() {
        this.nameRegex = null;
        this.optional = false;
        resourceProperties = new ArrayList<ResourceProperty>();
        children = new ArrayList<ChildResource>();
    }

    public @Nonnull ChildResourceBuilder nameRegex(String nameRegex) {
        this.nameRegex = nameRegex;
        return this;
    }

    public @Nonnull ChildResourceBuilder optional() {
        this.optional = true;
        return this;
    }

    public @Nonnull ChildResource build(@Nonnull String name) {
        return new ChildResourceImpl(name, nameRegex, !optional, resourceProperties, children);
    }
}
