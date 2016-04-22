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
package org.apache.sling.testing.util.config.impl;

import org.apache.sling.testing.util.config.InstanceConfig;
import org.apache.sling.testing.util.config.InstanceConfigCache;
import org.apache.sling.testing.util.config.InstanceConfigException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class InstanceConfigCacheImpl implements InstanceConfigCache {
    List<InstanceConfig> configs;

    public InstanceConfigCacheImpl(List<InstanceConfig> configs) {
        this.configs = configs;
    }

    public InstanceConfigCacheImpl() {
        this.configs = new ArrayList<InstanceConfig>();
    }

    @Override
    public int size() {
        return configs.size();
    }

    @Override
    public boolean isEmpty() {
        return configs.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return configs.contains(o);
    }

    @Override
    public Iterator<InstanceConfig> iterator() {
        return configs.iterator();
    }

    @Override
    public Object[] toArray() {
        return configs.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return configs.toArray(a);
    }

    @Override
    public boolean add(InstanceConfig instanceConfig) {
        return configs.add(instanceConfig);
    }

    @Override
    public boolean remove(Object o) {
        return configs.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return configs.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends InstanceConfig> c) {
        return configs.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return configs.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return configs.retainAll(c);
    }

    @Override
    public void clear() {
       configs.clear();
    }


    @Override
    public InstanceConfig save() throws InstanceConfigException {
        for (InstanceConfig ic : configs) {
            ic.save();
        }
        return this;
    }

    @Override
    public InstanceConfig restore() throws InstanceConfigException {
        for (InstanceConfig ic : configs) {
            ic.restore();
        }
        return this;
    }
}
