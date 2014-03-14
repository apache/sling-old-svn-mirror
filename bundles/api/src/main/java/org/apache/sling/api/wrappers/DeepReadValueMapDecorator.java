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
package org.apache.sling.api.wrappers;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

/**
 * A value map wrapper which implements deep reading of properties
 * based on the resource tree.
 * @since 2.5
 */
public class DeepReadValueMapDecorator extends ValueMapDecorator {

    private final String pathPrefix;

    private final ResourceResolver resolver;

    private final ValueMap base;

    public DeepReadValueMapDecorator(final Resource resource, final ValueMap base) {
        super(base);
        this.pathPrefix = resource.getPath() + "/";
        this.resolver = resource.getResourceResolver();
        this.base = base;
    }

    private ValueMap getValueMap(final String name) {
        final int pos = name.lastIndexOf("/");
        if ( pos == -1 ) {
            return this.base;
        }
        final Resource rsrc = this.resolver.getResource(pathPrefix + name.substring(0, pos));
        if ( rsrc != null ) {
            final ValueMap vm = rsrc.adaptTo(ValueMap.class);
            if ( vm != null ) {
                return vm;
            }
        }
        return ValueMap.EMPTY; // fall back
    }

    private String getPropertyName(final String name) {
        final int pos = name.lastIndexOf("/");
        if ( pos == -1 ) {
            return name;
        }
        return name.substring(pos + 1);
    }

    /**
     * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Class)
     */
    @Override
    public <T> T get(final String name, final Class<T> type) {
        return this.getValueMap(name).get(this.getPropertyName(name), type);
    }

    /**
     * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Object)
     */
    @Override
    public <T> T get(final String name, T defaultValue) {
        return this.getValueMap(name).get(this.getPropertyName(name), defaultValue);
    }

    /**
     * @see org.apache.sling.api.wrappers.ValueMapDecorator#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey(final Object key) {
        if ( key == null ) {
            return false;
        }
        final String name = key.toString();
        return this.getValueMap(name).containsKey(this.getPropertyName(name));
    }

    /**
     * @see org.apache.sling.api.wrappers.ValueMapDecorator#get(java.lang.Object)
     */
    @Override
    public Object get(final Object key) {
        if ( key == null ) {
            return null;
        }
        final String name = key.toString();
        return this.getValueMap(name).get(this.getPropertyName(name));
    }
}
