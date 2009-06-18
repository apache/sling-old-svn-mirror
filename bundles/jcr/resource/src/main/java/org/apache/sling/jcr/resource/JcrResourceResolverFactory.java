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
package org.apache.sling.jcr.resource;

import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * The <code>JcrResourceResolverFactory</code> interface defines the service
 * interface to have JCR-based <code>ResourceResolver</code> instances created
 * for JCR sessions.
 * <p>
 * This interface is not intended to be implemented by client applications. It
 * is implemented by this bundle and the implementation registered as a service
 * for use by client applications.
 */
public interface JcrResourceResolverFactory {

    /**
     * Returns a <code>ResourceResolver</code> for the given session. Calling
     * this method repeatedly returns a new instance on each call.
     * 
     * @param session The JCR <code>Session</code> used by the created
     *            resource manager to access the repository.
     */
    ResourceResolver getResourceResolver(Session session);

}
