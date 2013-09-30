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

public class ResourceProxy {

    private final String path;
    private final Map<String, Object> properties;
    private final List<ResourceProxy> children = new ArrayList<ResourceProxy>();
    private final Map<Class<?>, Object> adapted = new HashMap<Class<?>, Object>(1);

    public ResourceProxy(String path) {
        this(path, new HashMap<String, Object>());
    }

    public ResourceProxy(String path, Map<String, Object> properties) {
        this.path = path;
        this.properties = properties;
    }

    public void addChild(ResourceProxy child) {

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

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append('[').append(getClass().getSimpleName()).append(": path=").append(path).append(", properties=")
                .append(properties);
        for (ResourceProxy child : children) {
            out.append("\n- ").append(child);
        }
        out.append("\n]");

        return out.toString();
    }
}
