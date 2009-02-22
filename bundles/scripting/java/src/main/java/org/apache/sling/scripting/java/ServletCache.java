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

package org.apache.sling.scripting.java;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a simple in memory cache for compiled servlets.
 */
public final class ServletCache {

    /**
     * Maps servlet source urls to servlet wrappers.
     */
    private Map<String, ServletWrapper> servlets = new ConcurrentHashMap<String, ServletWrapper>();

    /**
     * Add a new ServletWrapper.
     *
     * @param servletUri Servlet URI
     * @param sw Servlet wrapper
     */
    public void addWrapper(String servletUri, ServletWrapper sw) {
        servlets.put(servletUri, sw);
    }

    /**
     * Get an already existing ServletWrapper.
     *
     * @param servletUri Servlet URI
     * @return ServletWrapper
     */
    public ServletWrapper getWrapper(String servletUri) {
        return servlets.get(servletUri);
    }

    /**
     * Remove a  ServletWrapper.
     *
     * @param servletUri Servlet URI
     */
    public void removeWrapper(String servletUri) {
        servlets.remove(servletUri);
    }

    /**
     * Process a "destory" event for this web application context.
     */
    public void destroy() {
        Iterator<ServletWrapper> i = this.servlets.values().iterator();
        while (i.hasNext()) {
            i.next().destroy();
        }
    }
}
