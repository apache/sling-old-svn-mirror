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

import aQute.bnd.annotation.ConsumerType;



/**
 * A resource provider might return the state when it was created and not
 * update to the latest state.
 * If the provider supports updating to the latest state, it should
 * implement this method.
 *
 * This interface is only supported if the provider has been create through
 * a {@link ResourceProviderFactory}.
 *
 * @see ResourceProviderFactory#getResourceProvider(java.util.Map)
 * @see ResourceProviderFactory#getAdministrativeResourceProvider(java.util.Map)
 *
 * @since 2.3.0
 */
@ConsumerType
public interface RefreshableResourceProvider extends ResourceProvider {

    /**
     * The provider is updated to reflect the latest state.
     * Resources which have changes pending are not discarded.
     */
    void refresh();
}
