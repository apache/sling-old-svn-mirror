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
package org.apache.sling.jcr.resource.internal.helper;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.internal.JcrResourceResolver;

/**
 * API for providers of resources. Used by the {@link JcrResourceResolver} and
 * {@link org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl}
 * classes to transparently access resources from different locations such as a
 * JCR repository (the default) or OSGi bundles.
 * <p>
 * This is an internal interface not available outside this bundle. It refers to
 * the internal {@link JcrResourceResolver} class, which is also not visible from
 * outside of this bundle.
 */
public interface ResourceProvider {

    /**
     * A list of absolute path prefixes of resources available through this
     * provider.
     */
    String[] getRoots();

    /**
     * Returns a resource from this resource provider or <code>null</code> if
     * the resource provider cannot find it. The path should have one of the
     * {@link #getRoots()} strings as its prefix.
     *
     * @throws Exception may be thrown in case of any problem creating the
     *             <code>Resource</code> instance.
     */
    Resource getResource(JcrResourceResolver jcrResourceResolver, String path)
            throws Exception;

}
