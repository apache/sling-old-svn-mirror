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

package org.apache.sling.servlets.post.impl.operations;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;

/**
 * MockResource is synthetic, which doesnt work here.
 */
public class MockRealResource implements Resource {
    private final ResourceResolver resourceResolver;
    private final String path;
    private final String resourceType;
    private final String name;
    private final String parentPath;
    private MockValueMap properties = new MockValueMap();

    public MockRealResource(ResourceResolver resourceResolver, String path, String resourceType) {
        this.resourceResolver = resourceResolver;
        this.path = path;
        this.resourceType = resourceType;
        this.name = path.substring(path.lastIndexOf('/'));
        this.parentPath = path.substring(0,path.lastIndexOf('/'));
    }

    public MockRealResource(ResourceResolver resourceResolver, String path, String resourceType, Map<String, Object> properties) {
        this.resourceResolver = resourceResolver;
        this.path = path;
        this.name = path.substring(path.lastIndexOf('/')+1);
        this.parentPath = path.substring(0,path.lastIndexOf('/'));
        this.resourceType = resourceType;
        this.properties.putAll(properties);
    }


    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Resource getParent() {
        return resourceResolver.getResource(parentPath);
    }

    @Override
    public Iterator<Resource> listChildren() {
        return resourceResolver.listChildren(this);
    }

    @Override    public Iterable<Resource> getChildren() {
        return resourceResolver.getChildren(this);
    }

    @Override
    public Resource getChild(String s) {
        return resourceResolver.getResource(path +"/"+ s);
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }

    @Override
    public String getResourceSuperType() {
        return null;
    }

    @Override
    public boolean isResourceType(String s) {
        return s.equals(resourceType);
    }

    @Override
    public ResourceMetadata getResourceMetadata() {
        return null;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> aClass) {
        if (ValueMap.class.isAssignableFrom(aClass)) {
            return (AdapterType) properties;
        }
        if (InputStream.class.isAssignableFrom(aClass) && properties.containsKey("jcr:data")) {
            Object o = properties.get("jcr:data");
            if (o instanceof InputStream) {
                return (AdapterType) o;
            } else {
                try {
                    return (AdapterType) new ByteArrayInputStream(String.valueOf(properties.get("jcr:data")).getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Cant convert UTF-8 to byte[]");
                }
            }
        }
        return null;
    }
}
