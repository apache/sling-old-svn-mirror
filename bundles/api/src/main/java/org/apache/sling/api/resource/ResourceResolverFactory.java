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

/**
 * The <code>ResourceResolverFactory</code> defines the service API to
 * get and create <code>ResourceResolver</code>s.
 * <p>
 * As soon as the resource resolver is not used anymore, {@link ResourceResolver#close()}
 * should be called.
 *
 * WORK IN PROGRESS - see SLING-1262
 * @since 2.1
 */
public interface ResourceResolverFactory {

    /** If this property is set in the authentication information which is passed
     * into {@link #getResourceResolver(Map)} or {@link #getAdministrativeResourceResolver(Map)}
     * then after a successful authentication attempt, a sudo to the provided
     * user id is tried.
     */
    String SUDO_USER_ID = "sudo.user.id";

    ResourceResolver getResourceResolver(Map<String, Object> authenticationInfo);

    ResourceResolver getAdministrativeResourceResolver(Map<String, Object> authenticationInfo);
}
