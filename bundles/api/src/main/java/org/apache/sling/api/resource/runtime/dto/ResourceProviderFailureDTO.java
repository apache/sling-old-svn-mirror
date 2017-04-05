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
 * Represents a failed {@code org.apache.sling.spi.resource.provider.ResourceProvider}.
 * If the provider is failed, some of the properties of the {@link ResourceProviderDTO}
 * might be missing or invalid, e.g. {@link ResourceProviderDTO#path} might contain
 * the empty path.
 *
 * @since 1.0.0 (Sling API Bundle 2.11.0)
 */
public class ResourceProviderFailureDTO extends ResourceProviderDTO {

    /**
     * The reason for the failure.
     */
	public FailureReason reason;
}
