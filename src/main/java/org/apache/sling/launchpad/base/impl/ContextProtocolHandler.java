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
package org.apache.sling.launchpad.base.impl;

import java.net.URL;
import java.net.URLConnection;

import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.osgi.service.url.AbstractURLStreamHandlerService;

/**
 * The <code>ContextProtocolHandler</code> is a simple extension of the OSGi
 * provided <code>AbstractURLStreamHandlerService</code> which simply returns
 * an instance of the {@link ContextConnection} when trying to open the
 * connection of the URL.
 */
public class ContextProtocolHandler extends AbstractURLStreamHandlerService {

    /**
     * The {@link LaunchpadContentProvider} to which requests for content access are
     * delegated.
     */
    private final LaunchpadContentProvider resourceProvider;

    /**
     * Creates an instance of this protocol handler setting the servlet context
     * which is queried to access content.
     *
     * @param resourceProvider The {@link LaunchpadContentProvider} to which requests
     *            for content access are delegated.
     */
    public ContextProtocolHandler(LaunchpadContentProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    /**
     * Returns an instance of the {@link ContextConnection} class to access the
     * content of the <code>url</code>.
     *
     * @param url The URL whose content is requested.
     */
    public URLConnection openConnection(URL url) {
        return new ContextConnection(url, resourceProvider);
    }
}
