/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/

package org.apache.sling.scripting.core.impl.helper;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * The {@code CachingMap} class offers an implementation of a {@link LinkedHashMap} that can be used for caches. The maps' values are
 * {@link SoftReference}s, such that garbage collection can be performed on the cache, if needed.
 * </p>
 * <p>
 * Read / write operations are <i>NOT</i> synchronised.
 * </p>
 *
 * @param <T> the type to which {@link SoftReference}s will be kept
 */
public class CachingMap<T> extends LinkedHashMap<String, SoftReference<T>> {

    private int capacity;

    /**
     * Creates a caching map with a maximum capacity equal to the {@code capacity} parameter.
     *
     * @param capacity the maximum capacity; if {@code capacity < 1} then this map will always remove the latest added element
     */
    public CachingMap(int capacity) {
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, SoftReference<T>> eldest) {
        return size() > capacity;
    }
}
