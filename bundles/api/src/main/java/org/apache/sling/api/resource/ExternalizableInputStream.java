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

import java.net.URI;

/**
 * This interface is normally used to extend an InputStream to indicate that it has a URI form that could
 * be used in place of the InputStream if desired. It is used in situations where the internal of a ResourceProvider
 * wants to offload IO to channels that do not pass through the JVM. The URI that is returned may have restrictions
 * imposed on it requiring it to be used immediately. Do not store the URI for later usage as it will, in most cases,
 * have expired.
 *
 * @since 2.11.0
 */
public interface ExternalizableInputStream {

    /**
     * Get a URI that is specific to the current session, and may be used anywhere. May return null if this
     * type of URI is not available.
     * @return a URI intended for any network context.
     */
    URI getURI();

    /**
     * Get a URI that is specific to the current session and may only be used in a private context. A private network context means
     * that the URI may only be resolvable inside a private network. Usign this URL in any context will not always work, and
     * may leak information about the private network.
     * @return a URI intended for a private network context.
     */
    URI getPrivateURI();
}
