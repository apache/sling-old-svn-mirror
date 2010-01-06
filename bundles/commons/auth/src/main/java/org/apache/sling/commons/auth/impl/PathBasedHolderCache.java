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
package org.apache.sling.commons.auth.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class PathBasedHolderCache<Type extends PathBasedHolder> {

    private final Map<String, Map<String, ArrayList<Type>>> cache = new HashMap<String, Map<String, ArrayList<Type>>>();

    public synchronized void clear() {
        cache.clear();
    }

    public synchronized void addHolder(final Type holder) {

        Map<String, ArrayList<Type>> byHostMap = cache.get(holder.protocol);
        if (byHostMap == null) {
            byHostMap = new HashMap<String, ArrayList<Type>>();
            cache.put(holder.protocol, byHostMap);
        }

        final ArrayList<Type> byPathList = new ArrayList<Type>();

        // preset with current list
        final ArrayList<Type> currentPathList = byHostMap.get(holder.host);
        if (currentPathList != null) {
            byPathList.addAll(currentPathList);
        }

        // add the new holder
        byPathList.add(holder);

        // sort the list according to the path length (longest path first)
        Collections.sort(byPathList);

        // replace old list with new list
        byHostMap.put(holder.host, byPathList);
    }

    public synchronized void removeHolder(final Type holder) {
        final Map<String, ArrayList<Type>> byHostMap = cache.get(holder.protocol);
        if (byHostMap != null) {
            final ArrayList<Type> byPathList = byHostMap.get(holder.host);
            if (byPathList != null) {

                // create a new list without the removed holder
                final ArrayList<Type> list = new ArrayList<Type>();
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
    }

    public synchronized ArrayList<Type> findApplicableHolder(
            HttpServletRequest request) {

        Map<String, ArrayList<Type>> byHostMap = cache.get(request.getScheme());
        if (byHostMap == null) {
            byHostMap = cache.get("");
        }

        String hostname = request.getServerName()
            + (request.getServerPort() != 80 && request.getServerPort() != 443
                    ? ":" + request.getServerPort()
                    : "");

        ArrayList<Type> infos = null;
        if (byHostMap != null) {
            infos = byHostMap.get(hostname);
            if (infos == null) {
                infos = byHostMap.get("");
            }
            if (infos != null) {
                return infos;
            }
        }

        return null;
    }

    public synchronized ArrayList<Type> getHolders() {
        final ArrayList<Type> result = new ArrayList<Type>();
        for (Map<String, ArrayList<Type>> byHostEntry : cache.values()) {
            for (ArrayList<Type> holderList : byHostEntry.values()) {
                result.addAll(holderList);
            }
        }
        return result;
    }
}
