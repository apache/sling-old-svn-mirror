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

import org.osgi.annotation.versioning.ProviderType;

import java.net.URI;

/**
 * Provides a URI in exchange for a Resource.
 * Typically the Resource will represent something where is a URI is valiable and usefull.
 * Implementations of this interface must ensure that the any underlying security model is delegated
 * securely and not circumvented. Typically resource provider bundles should implement this provider as in most cases
 * internal implementation details of the resource will be required to achieve the implementation. Ideally
 * implementations should be carefully reviewed by peers.
 *
 * @since 2.11.0
 */
@ProviderType
public interface URIProvider {

    /**
     * Return a URI appicable to the defined scope.
     * @param scope the required scope.
     * @param resource the resource to convert from.
     * @return a URI if the resoruce has a URI suitable for the requested scope.
     */
    URI toURI(URIProvider.Scope scope, Resource resource);

    enum Scope {
        PUBLIC,
        INTERNAL
    }
}