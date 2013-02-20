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
package org.apache.sling.auth.core.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpServletRequest;

public class PathBasedHolderCache<Type extends PathBasedHolder> {

    private final Map<String, Map<String, List<Type>>> cache = new HashMap<String, Map<String, List<Type>>>();

    /** Read/write lock to synchronize the cache access. */
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void clear() {
        this.rwLock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    public void addHolder(final Type holder) {
        this.rwLock.writeLock().lock();
        try {

            Map<String, List<Type>> byHostMap = cache.get(holder.protocol);
            if (byHostMap == null) {
                byHostMap = new HashMap<String, List<Type>>();
                cache.put(holder.protocol, byHostMap);
            }

            final List<Type> byPathList = new ArrayList<Type>();

            // preset with current list
            final List<Type> currentPathList = byHostMap.get(holder.host);
            if (currentPathList != null) {
                byPathList.addAll(currentPathList);
            }

            // add the new holder
            byPathList.add(holder);

            // sort the list according to the path length (longest path first)
            Collections.sort(byPathList);

            // replace old list with new list
            byHostMap.put(holder.host, byPathList);
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    public void removeHolder(final Type holder) {
        this.rwLock.writeLock().lock();
        try {
            final Map<String, List<Type>> byHostMap = cache.get(holder.protocol);
            if (byHostMap != null) {
                final List<Type> byPathList = byHostMap.get(holder.host);
                if (byPathList != null) {

                    // create a new list without the removed holder
                    final List<Type> list = new ArrayList<Type>();
                    list.addAll(byPathList);
                    list.remove(holder);

                    // replace the old list with the new one (or remove if empty)
                    if (list.isEmpty()) {
                        byHostMap.remove(holder.host);
                    } else {
                        byHostMap.put(holder.host, list);
                    }
                }
            }
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    public List<Type>[] findApplicableHolder(final HttpServletRequest request) {
        this.rwLock.readLock().lock();
        try {
            final String hostname = request.getServerName()
                  + (request.getServerPort() != 80 && request.getServerPort() != 443
                    ? ":" + request.getServerPort()
                    : "");

            @SuppressWarnings("unchecked")
            final List<Type>[] result = new ArrayList[4];

            final Map<String, List<Type>> byHostMap = cache.get(request.getScheme());
            if ( byHostMap != null ) {
                result[0] = byHostMap.get(hostname);
                result[1] = byHostMap.get("");
            }
            final Map<String, List<Type>> defaultByHostMap = cache.get("");
            if ( defaultByHostMap != null ) {
                result[2] = defaultByHostMap.get(hostname);
                result[3] = defaultByHostMap.get("");
            }
            return result;
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    public List<Type> getHolders() {
        this.rwLock.readLock().lock();
        try {
            final List<Type> result = new ArrayList<Type>();
            for (Map<String, List<Type>> byHostEntry : cache.values()) {
                for (List<Type> holderList : byHostEntry.values()) {
                    result.addAll(holderList);
                }
            }
            return result;
        } finally {
            this.rwLock.readLock().unlock();
        }
    }
}
