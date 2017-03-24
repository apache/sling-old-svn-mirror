/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.esx.plugins;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

public class SimpleResource {

    private final Resource resource;
    private final ValueMap valueMap;

    public SimpleResource(Resource resource) {
        this.resource = resource;
        this.valueMap = resource.getValueMap();
    }

    public String getPath() {
        return resource.getPath();
    }

    public String getResourceType() {
        return resource.getResourceType();
    }

    public String getResourceSuperType() {
        return resource.getResourceSuperType();
    }

    public SimpleResource getParent() {
        return resource.getParent().adaptTo(SimpleResource.class);
    }

    public boolean hasChildren() {
        return resource.hasChildren();
    }

    public boolean isResourceType(String resourceType) {
        return resource.isResourceType(resourceType);
    }

    public ValueMap getValueMap() {
        return valueMap;
    }
    
    public ValueMap getProperties() {
        return getValueMap();
    }
    
    public String getStringProperty(String key) {
        return valueMap.get(key, String.class);
    }
    
    public long getDateTimeProperty(String key) {
        return ((Calendar) valueMap.get(key, Calendar.class)).getTime().getTime();
    }
    
    public Object[] getArray(String key) {
        return (Object[]) valueMap.get(key, Object[].class);
    }

    public SimpleResource getChild(String childName) {
        return resource.getChild(childName).adaptTo(SimpleResource.class);
    }
    
    public ResourceResolver getResourceResolver() {
        return resource.getResourceResolver();
    }
    public List<SimpleResource> getChildren() {
        Iterator<Resource> resChildren = resource.listChildren();
        ArrayList<SimpleResource> children = new ArrayList<SimpleResource>();
        resChildren.forEachRemaining(resource -> children.add(resource.adaptTo(SimpleResource.class)));
        return children;
    }

}
