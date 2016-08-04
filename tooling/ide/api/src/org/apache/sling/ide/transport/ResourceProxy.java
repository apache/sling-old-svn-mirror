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
package org.apache.sling.ide.transport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The <tt>ResourceProxy</tt> is a representation of a resource as found in the local workspace
 * 
 * <p>
 * The resource usually has properties, as returned from <tt>{@link #getProperties()}</tt>. If no properties are found,
 * it means that the resource is only a reference and not that it is an empty resource.
 * </p>
 * 
 * <p>
 * The resource only has information about the first row of children, as returned by {@link #getChildren()}. It does not
 * necessarily know about second-level children.
 * </p>
 * 
 * <p>
 * By exception, if the child is <tt>covered</tt>, then all information about that child and its descendants is known.
 * </p>
 *
 */
public class ResourceProxy {

    private final String path;
    private final Map<String, Object> properties;
    private final List<ResourceProxy> children = new ArrayList<>();
    private final Map<Class<?>, Object> adapted = new HashMap<>(1);

    public ResourceProxy(String path) {
        this(path, new HashMap<String, Object>());
    }

    public ResourceProxy(String path, Map<String, Object> properties) {
        this.path = path;
        this.properties = properties;
    }

    public void addChild(ResourceProxy child) {

        // TODO - should validate for direct parent
        if (!isParent(path, child.getPath())) {
            throw new IllegalArgumentException("Resource at path " + child.getPath() + " is not a direct child of "
                    + path);
        }

        this.children.add(child);
    }

    public void addProperty(String name, Object value) {

        this.properties.put(name, value);
    }

    public String getPath() {
        return path;
    }

    public List<ResourceProxy> getChildren() {
        return children;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public <T> void addAdapted(Class<T> klazz, T adaptedInstance) {

        adapted.put(klazz, adaptedInstance);
    }

    public <T> T adaptTo(Class<T> klazz) {

        // OK to suppress warnings since type safety is insured by addAdapted
        @SuppressWarnings("unchecked")
        T res = (T) adapted.get(klazz);

        return res;
    }

    public List<ResourceProxy> getCoveredChildren() {

        List<ResourceProxy> coveredChildren = new ArrayList<>();
        for (ResourceProxy child : getChildren()) {
            if (child.getProperties().isEmpty()) {
                continue;
            }

            coveredChildren.add(child);
        }

        return coveredChildren;
    }

    public boolean covers(String path) {
        for (ResourceProxy child : getCoveredChildren()) {
            if (child.getPath().equals(path)) {
                return true;
            } else if (isDescendent(child.getPath(), path)) {
                return child.covers(path);
            }
        }

        return false;
    }

    private boolean isParent(String parentPath, String childPath) {

        if (!isDescendent(parentPath, childPath)) {
            return false;
        }

        for (int i = parentPath.length() + 1; i < childPath.length(); i++) {
            if (childPath.charAt(i) == '/') {
                return false;
            }
        }

        return true;
    }

    private boolean isDescendent(String parentPath, String childPath) {
        if (parentPath.equals("/")) {
            return childPath.length() > 1;
        }

        return parentPath.length() < childPath.length() && childPath.charAt(parentPath.length()) == '/'
                    && childPath.startsWith(parentPath);
    }

    public ResourceProxy getChild(String path) {
        for (ResourceProxy child : getChildren()) {
            if (child.getPath().equals(path)) {
                return child;
            } else if (isDescendent(child.getPath(), path)) {
                return child.getChild(path);
            }
        }

        return null;
    }


    @Override
    public String toString() {
        return toString0(1);
    }

    private String toString0(int padding) {
        StringBuilder out = new StringBuilder();
        out.append(getClass().getSimpleName()).append(": path=").append(path).append(", properties=")
                .append(properties);
        for (ResourceProxy child : children) {
            out.append("\n");
            for (int i = 0; i < padding * 2; i++) {
                out.append(' ');
            }
            out.append(child.toString0(padding + 1));
        }

        return out.toString();
    }

}
