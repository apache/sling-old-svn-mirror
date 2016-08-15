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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpServletRequest;

public class PathBasedHolderCache<Type extends PathBasedHolder> {

    private final Map<String, Map<String, SortedSet<Type>>> cache = new HashMap<String, Map<String, SortedSet<Type>>>();

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

            Map<String, SortedSet<Type>> byHostMap = cache.get(holder.protocol);
            if (byHostMap == null) {
                byHostMap = new HashMap<String, SortedSet<Type>>();
                cache.put(holder.protocol, byHostMap);
            }

            final SortedSet<Type> byPathSet = new TreeSet<Type>();

            // preset with current list
            final SortedSet<Type> currentPathSet = byHostMap.get(holder.host);
            if (currentPathSet != null) {
                byPathSet.addAll(currentPathSet);
            }

            // add the new holder
            byPathSet.add(holder);

            // replace old set with new set
            byHostMap.put(holder.host, byPathSet);
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    public void removeHolder(final Type holder) {
        this.rwLock.writeLock().lock();
        try {
            final Map<String, SortedSet<Type>> byHostMap = cache.get(holder.protocol);
            if (byHostMap != null) {
                final SortedSet<Type> byPathSet = byHostMap.get(holder.host);
                if (byPathSet != null) {

                    // create a new set without the removed holder
                    final SortedSet<Type> set = new TreeSet<Type>();
                    set.addAll(byPathSet);
                    set.remove(holder);

                    // replace the old set with the new one (or remove if empty)
                    if (set.isEmpty()) {
                        byHostMap.remove(holder.host);
                    } else {
                        byHostMap.put(holder.host, set);
                    }
                }
            }
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /**
     * Remove all holders which "equal" the provided holder
     * @param holder Template holder
     */
    public void removeAllMatchingHolders(final Type holder) {
        this.rwLock.writeLock().lock();
        try {
            for(final Map.Entry<String,  Map<String, SortedSet<Type>>> entry : this.cache.entrySet()) {
                final Iterator<Map.Entry<String, SortedSet<Type>>> innerIter = entry.getValue().entrySet().iterator();
                while ( innerIter.hasNext() ) {
                    final Map.Entry<String, SortedSet<Type>> innerEntry = innerIter.next();
                    final Iterator<Type> iter = innerEntry.getValue().iterator();
                    while ( iter.hasNext() ) {
                        final Type current = iter.next();
                        if ( holder.equals(current) ) {
                            iter.remove();
                        }
                    }
                    if ( innerEntry.getValue().isEmpty() ) {
                        innerIter.remove();
                    }
                }
            }
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    public Collection<Type>[] findApplicableHolders(final HttpServletRequest request) {
        this.rwLock.readLock().lock();
        try {
            final String hostname = request.getServerName()
                  + (request.getServerPort() != 80 && request.getServerPort() != 443
                    ? ":" + request.getServerPort()
                    : "");

            @SuppressWarnings("unchecked")
            final SortedSet<Type>[] result = new SortedSet[4];

            final Map<String, SortedSet<Type>> byHostMap = cache.get(request.getScheme());
            if ( byHostMap != null ) {
                result[0] = byHostMap.get(hostname);
                result[1] = byHostMap.get("");
            }
            final Map<String, SortedSet<Type>> defaultByHostMap = cache.get("");
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
            for (Map<String, SortedSet<Type>> byHostEntry : cache.values()) {
                for (SortedSet<Type> holderSet : byHostEntry.values()) {
                    result.addAll(holderSet);
                }
            }
            return result;
        } finally {
            this.rwLock.readLock().unlock();
        }
    }
}
