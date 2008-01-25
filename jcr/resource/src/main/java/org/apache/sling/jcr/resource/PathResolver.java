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
package org.apache.sling.jcr.resource;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;

/**
 * The <code>PathResolver</code> interface defines additional methods to the
 * Sling <code>ResourceResolver</code> which allow resolving url strings to
 * <code>Resource</code> objects using the same algorithms as used by the
 * <code>SlingHttpServletRequest</code> based resolver. In addition methods
 * are provided to apply a reverse mapping from (resource) path to url string.
 * This reverse mapping may be used for example to create links for existing
 * paths.
 */
public interface PathResolver {

    /**
     * Resolves the given url string to a <code>Resource</code> applying the
     * same resolution semantics as the
     * <code>ResourceResolver.resolve(SlingHttpServletRequest request)</code>
     * method. Unlike that other method, which never returns <code>null</code>,
     * this method may return <code>null</code> if the url does not resolve.
     *
     * @param url The URL string to resolve to a resource.
     * @return The <code>Resource</code> to which the url string resolves or
     *         <code>null</code> if the url string cannot be resolved to a
     *         resource.
     * @throws SlingException May be thrown if an error occurrs trying to
     *             resolve the url string to a resource.
     */
    Resource resolve(String url) throws SlingException;

    /**
     * Maps a resource path to an url string, which when fed to the
     * {@link #resolve(String)} method returns a resource of the given path.
     * This method may be used to get the external (URI) representation of a
     * resource, which is guaranteed to map back to the same resource, when used
     * in a request.
     *
     * @param path The resource path to map to an url string
     * @return The external (URI) representation of the resource path.
     */
    String pathToURL(Resource resource);

}