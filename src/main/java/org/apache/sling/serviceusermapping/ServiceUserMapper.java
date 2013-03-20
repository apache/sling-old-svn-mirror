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
package org.apache.sling.serviceusermapping;

import org.osgi.framework.Bundle;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface ServiceUserMapper {

    /**
     * Returns the name of a user to the be used to access the Sling Resource
     * tree or the JCR Repository.
     *
     * @param bundle The bundle implementing the service request access to
     *            resources.
     * @param serviceInfo Additional information about the concrete service
     *            requesting access. This parameter is optional and may be
     *            {@code null}.
     * @return The name of the user to use to provide access to the resources
     *         for the service. This may be {@code null} to only grant guest
     *         level (or anonymous level) access to the resources.
     */
    String getUserForService(Bundle bundle, String serviceInfo);

}
