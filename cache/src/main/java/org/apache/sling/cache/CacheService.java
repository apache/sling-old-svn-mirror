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
package org.apache.sling.cache;

import java.util.List;

/**
 * The <code>CacheHandlerService</code> interface is implemented by classes
 * configured in the &lt;cache> element of the <code>DeliveryModule</code>
 * configuration.
 * <p>
 * TODO: This interface has to be redefined into a pure Cache Management
 * interface as cache checking and delivery from cache is no handled by
 * specialized RenderFilters.
 * <p>
 * Thoughts on this Service Interface:
 * <ul>
 * <li>Multiple Services may exist
 * <li>When caching, each service is asked, whether it would like to handle the
 * request
 * <li>If non wants to, caching is disabled
 * <li>Otherwise that service is used for caching
 * <li>This is somewhat comparable to the cache configurations of CQ3
 * <li>Maybe this is to be implemented in terms of a ManagedServiceFactory or
 * ComponentFactory to allow the instantiation of multiple services by multiple
 * configurations ....
 * </ul>
 */
public interface CacheService {

    /**
     * Removes all entries for the page indicated with the <code>handle</code>
     * from the cache or to completely clear the cache if <code>handle</code>
     * is <code>null</code>.
     * <p>
     * This method flushes any cache entry whose dependency list contains this
     * handle. Previous releases only flushed the cache entries whose request
     * URIs mapped to the geiven handle.
     *
     * @param handle The handle of the <code>Page</code> whose cache entries
     *            should be removed or <code>null</code> to remove all entries
     *            from the cache.
     */
    public abstract void flushPageEntries(String handle);

    /**
     * Returns a list of request URI strings leading to cache entries, which
     * depend on the page addressed by the given <code>handle</code>.
     *
     * @param pageHandle The handle of the page whose dependent cache entry
     *            request URIs should be returned.
     * @return The list of cache entry URLs depending on the given handle. If an
     *         error occurrs building that list
     */
    public abstract List getDependencies(String pageHandle);

}
