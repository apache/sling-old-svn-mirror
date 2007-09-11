/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.cache.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.cache.CacheService;
import org.apache.sling.component.ComponentRequest;


/**
 * The <code>AbstractCacheFilter</code> TODO
 *
 * @scr.reference name="CacheService"
 *                interface="org.apache.sling.cache.CacheService"
 *                cardinality="0..n" policy="dynamic"
 */
abstract class AbstractCacheFilter {

    List cacheServices;

    protected CacheService getBurstCacheService(ComponentRequest request) {
        // return a cache service willing to handle burst caching for the request
        return null;
    }

    protected CacheService getCacheService(ComponentRequest request) {
        // return a cache service willing to handle this component rendering
        return null;
    }

    //---------- SCR integration ----------------------------------------------

    protected void bindCacheService(CacheService cacheService) {
        if (this.cacheServices == null) {
            this.cacheServices = new ArrayList();
        }
        this.cacheServices.add(cacheService);
    }

    protected void unbindCacheService(CacheService cacheService) {
        if (this.cacheServices != null) {
            this.cacheServices.remove(cacheService);
        }
    }
}
