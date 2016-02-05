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
package org.apache.sling.api.resource.runtime.dto;

/**
 * Represents a {@code org.apache.sling.spi.resource.provider.ResourceProvider}.
 *
 * @since 1.0.0 (Sling API Bundle 2.11.0)
 */
public class ResourceProviderDTO {

    /**
	 * The name of the resource provider.
	 * Optional might be {@code null}.
	 */
	public String name;

    /**
     * The path of the resource provider.
     * This is never {@code null}.
     */
    public String path;

    /**
     * Whether resource access security should be used.
     */
    public boolean useResourceAccessSecurity;

    /**
     * The auth handling for this provider.
     * This is never {@code null}.
     */
    public AuthType authType;

    /**
     * Whether the resource provider supports modifications.
     */
    public boolean modifiable;

    /**
     * Whether the resource provider supports adaptable.
     */
    public boolean adaptable;

    /**
     * Whether the resource provider supports refreshing.
     */
    public boolean refreshable;

    /**
     * Whether the resource provider supports attributes.
     */
    public boolean attributable;

    /**
     * Whether the resource provider supports query languages.
     */
    public boolean supportsQueryLanguage;

    /**
     * The service id from the service registry.
     */
    public long serviceId;
}
