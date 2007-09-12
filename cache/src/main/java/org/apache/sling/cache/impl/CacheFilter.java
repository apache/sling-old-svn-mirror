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
package org.apache.sling.cache.impl;

import java.io.IOException;

import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentFilterChain;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;

/**
 * The <code>CacheFilter</code> class is a component level filter implementing
 * the caching and caching delivery per component rendering.
 *
 * @scr.component immediate="true" inherit="true"
 *      label="%cache.name" description="%cache.description"
 * @scr.property name="service.description"
 *          value="Component Rendering Cache Filter"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="component" private="true"
 * @scr.property name="filter.order" value="-1000" type="Integer" private="true"
 * @scr.service
 */
public class CacheFilter extends AbstractCacheFilter implements ComponentFilter {

    /**
     * @see ComponentFilter#doFilter(ComponentRequest, ComponentResponse, ComponentFilterChain),
     *      ComponentResponse,
     *      ComponentFilterChain
     */
    public void doFilter(ComponentRequest request, ComponentResponse response,
            ComponentFilterChain filterChain) throws IOException,
            ComponentException {

        // Actually, this needs to be done:
        // -> can we handle this from the cache ?
        // -> if not, is this cacheable ??
        // -> if so, wrap the response and request
        // -> if not, just continue in the chain
        //
        // CacheHandlerData is not required, as we have the response
        // and request wrappers
        //
        // Handle the tasks of the cache handler data in the wrappers, namely
        // the dependency tracking and "cache stop" (actually this is supported
        // by the ComponentResponse.EXPIRATION_CACHE attribute
        //
        // Tasks of the wrappers:
        // - Dependency tracking
        // - Cookie tracking
        // - expiration time setting
        // - stop caching (see ComponentResponse.EXPIRATION_CACHE attribute)
        //
        // Additional tasks required from a caching service:
        // - Explicit cache flushing by JCR node (path)
        // - Explicit cache flushing by URI
        // - Implicit cache flush (dependency tracking, JCR observation)
        // - JMX tasks such as cache flush, cache enumeration, cache
        // configuration

        // // 2.1 Cache Setup --> Cache Handler
        // CacheHandlerData hd = getCacheService().checkCache(request,
        // response);
        // if (hd == CacheService.FROM_CACHE) {
        // return;
        // }
        //
        // // 2.2 create the content data now with the cache handler data from
        // // above
        // contentData = requestData.getContentData();
        // contentData.setCacheHandlerData(hd);

        // currently there is no caching, so just forward
        filterChain.doFilter(request, response);
    }

    public void init(ComponentContext context) {
    }

    public void destroy() {
    }
}
