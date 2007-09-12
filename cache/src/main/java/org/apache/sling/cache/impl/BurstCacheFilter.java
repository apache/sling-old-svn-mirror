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
 * The <code>BurstCacheFilter</code> is a global Request Component API filter,
 * which checks for the complete request, whether caching may quickly be
 * applied.
 *
 * @scr.component immediate="true" inherit="true"
 *      label="%burstcache.name" description="%burstcache.description"
 * @scr.property name="service.description" value="Burst Cache Delivery Filter"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="request" private="true"
 * @scr.property name="filter.order" value="-800" type="Integer" private="true"
 * @scr.service
 */
public class BurstCacheFilter extends AbstractCacheFilter implements
    ComponentFilter {

    public void doFilter(ComponentRequest request, ComponentResponse response,
            ComponentFilterChain filterChain) throws IOException,
            ComponentException {

        // currently there is no caching, so just forward
        filterChain.doFilter(request, response);
    }

    public void init(ComponentContext context) {
    }

    public void destroy() {
    }
}
