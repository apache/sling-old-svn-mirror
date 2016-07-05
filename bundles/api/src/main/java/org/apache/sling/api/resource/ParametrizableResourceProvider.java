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
package org.apache.sling.api.resource;

import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import aQute.bnd.annotation.ConsumerType;

/**
 * This extension allows resource provider implementations to support
 * semicolon-separated parameters added to the URI, eg.: {@code /content/test;v='1.0'}.
 *
 * If a {@code ResourceProvider} implements this interface, the {@link #getResource(ResourceResolver, String, Map)}
 * method is called instead of {@link ResourceProvider#getResource(ResourceResolver, String)}
 * if such parameters are available. If no map (or an empty map) is available,
 * {@link ResourceProvider#getResource(ResourceResolver, String)} is called.
 *
 * @since 2.8.0 (Sling API Bundle 2.9.0)
 * @deprecated Use the {@link org.apache.sling.spi.resource.provider.ResourceProvider}
 */
@Deprecated
@ConsumerType
public interface ParametrizableResourceProvider {

    /**
     * Returns a resource from this resource provider or <code>null</code> if
     * the resource provider cannot find it. The path should have one of the {@link ResourceProvider#ROOTS}
     * strings as its prefix.
     *
     * The resource provider must not return cached instances for a resource as
     * the resource resolver will update the resource metadata of the resource
     * at the end of the resolution process and this metadata might be different
     * depending on the full path of resource resolution passed into the
     * resource resolver.
     *
     * @param resourceResolver
     *            The {@link ResourceResolver} to which the returned {@link Resource} is attached.
     * @param path The full path of the resource.
     * @param parameters A map of additional parameters, the map contains at least one parameter.
     * @return <code>null</code> If this provider does not have a resource for
     *         the path.
     * @throws org.apache.sling.api.SlingException
     *             may be thrown in case of any problem creating the <code>Resource</code> instance.
     * @see ResourceProvider#getResource(ResourceResolver, String)
     */
    @CheckForNull Resource getResource(@Nonnull ResourceResolver resourceResolver,
            @Nonnull String path,
            @Nonnull Map<String, String> parameters);
}
