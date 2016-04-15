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
package org.apache.sling.spi.resource.provider;

import java.util.Map;

import javax.annotation.CheckForNull;

import aQute.bnd.annotation.ProviderType;

/**
 * The resource context provides additional information for resource resolving.
 *
 * @since 1.0.0 (Sling API Bundle 2.11.0)
 */
@ProviderType
public interface ResourceContext {

    /**
     * Return optional parameters for resolving the resource.
     * For example if the resource is resolved through an http request, this
     * map could contain the path parameters of the url.
     * @return A non empty map with parameters or {@code null}.
     */
    @CheckForNull Map<String, String> getResolveParameters();

    /**
     * "Empty" instance, not providing any additional information.
     */
    ResourceContext EMPTY_CONTEXT = new ResourceContext() {

        @Override
        public Map<String, String> getResolveParameters() {
            return null;
        }
    };
}
