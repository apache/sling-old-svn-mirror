/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.provisioning.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class KeyValueMap<T>
    extends Commentable
    implements Iterable<Map.Entry<String, T>> {

    private final Map<String, T> properties = new HashMap<String, T>();

    public T get(final String key) {
        return this.properties.get(key);
    }

    public void put(final String key, final T value) {
        this.properties.put(key, value);
    }

    public void putAll(final KeyValueMap<T> map) {
        this.properties.putAll(map.properties);
    }

    @Override
    public Iterator<Entry<String, T>> iterator() {
        return this.properties.entrySet().iterator();
    }

    public boolean isEmpty() {
        return this.properties.isEmpty();
    }

    @Override
    public String toString() {
        return properties.toString();
    }
}
