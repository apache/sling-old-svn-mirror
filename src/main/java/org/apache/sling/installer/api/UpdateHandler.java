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
package org.apache.sling.installer.api;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Map;


/**
 * An update handler is a service handling updates of resources through other ways
 * than the installer, e.g. handling a configuration change through the web console
 * or directly through the configuration admin API.
 *
 * @since 3.1
 */
public interface UpdateHandler {

    /**
     * Required configuration property defining the schemes, this handler is handling.
     * String or string array
     */
    String PROPERTY_SCHEMES = "handler.schemes";

    /**
     * Handle the remove of a resource
     * @param resourceType The resource type
     * @param id The resource id, e.g. symbolic name etc.
     * @param url The url where an earlier version of this resource came from
     * @return If the handler could handle/perist the resource an update result is returned
     *         otherwise the handler should return <code>null</code>
     */
    UpdateResult handleRemoval(final String resourceType,
            final String id,
            final String url);

    /**
     * Handle the update of a resource
     * @param resourceType The resource type
     * @param id The resource id, e.g. symbolic name etc.
     * @param url The url where an earlier version of this resource came from (optional)
     * @param dict Dictionary
     * @param attributes Optional additional attributes.
     * @return If the handler could handle/perist the resource an update result is returned
     *         otherwise the handler should return <code>null</code>
     */
    UpdateResult handleUpdate(final String resourceType,
            final String id,
            final String url,
            final Dictionary<String, Object> dict,
            final Map<String, Object> attributes);

    /**
     * Handle the update of a resource
     * @param resourceType The resource type
     * @param id The resource id, e.g. symbolic name etc.
     * @param url The url where an earlier version of this resource came from (optional)
     * @param is Input stream to the contents of the resource
     * @param attributes Optional additional attributes.
     * @return If the handler could handle/perist the resource an update result is returned
     *         otherwise the handler should return <code>null</code>
     */
    UpdateResult handleUpdate(final String resourceType,
            final String id,
            final String url,
            final InputStream is,
            final Map<String, Object> attributes);
}