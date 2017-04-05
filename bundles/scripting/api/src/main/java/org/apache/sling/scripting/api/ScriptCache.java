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

package org.apache.sling.scripting.api;

/**
 * The {@code ScriptCache} service interface defines a cache for compiled scripts. Implementations of this interface should be thread-safe.
 */
public interface ScriptCache {

    /**
     * Retrieves the {@link CachedScript} corresponding to the script accessible from the {@code scriptPath}.
     *
     * @param scriptPath the path from where the script can be accessed
     * @return the {@link CachedScript} if one exists, {@code null} otherwise
     */
    CachedScript getScript(String scriptPath);

    /**
     * Stores a {@link CachedScript} in the cache. If a previous version of it exist in the cache it is overridden.
     *
     * @param script the {@link CachedScript} that should be stored in the cache
     */
    void putScript(CachedScript script);

    /**
     * Empties the cache.
     */
    void clear();

    /**
     * Removes the script identified by {@code scriptPath} from the cache.
     *
     * @param scriptPath the path from where the script can be accessed
     * @return {@code true} if a script was cached from that path and was removed, {@code false} otherwise
     */
    boolean removeScript(String scriptPath);

}
