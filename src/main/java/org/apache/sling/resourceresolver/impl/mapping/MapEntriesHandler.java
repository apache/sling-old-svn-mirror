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
package org.apache.sling.resourceresolver.impl.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Interface for MapEntries to only expose the public methods
 * used during resource resolving
 */
public interface MapEntriesHandler {

    public MapEntriesHandler EMPTY = new MapEntriesHandler() {

        @Override
        public Iterator<MapEntry> getResolveMapsIterator(String requestPath) {
            return Collections.emptyIterator();
        }

        @Override
        public List<MapEntry> getResolveMaps() {
            return Collections.emptyList();
        }

        @Override
        public Collection<MapEntry> getMapMaps() {
            return Collections.emptyList();
        }

        @Override
        public Map<String, String> getAliasMap(String parentPath) {
            return Collections.emptyMap();
        }
    };

    Map<String, String> getAliasMap(String parentPath);

    /**
     * Calculate the resolve maps. As the entries have to be sorted by pattern
     * length, we have to create a new list containing all relevant entries.
     */
    Iterator<MapEntry> getResolveMapsIterator(String requestPath);

    Collection<MapEntry> getMapMaps();

    /**
     * This is for the web console plugin
     */
    List<MapEntry> getResolveMaps();
}
