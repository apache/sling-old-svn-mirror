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
import org.apache.sling.api.resource.ResourceResolverFactory;

/**
 * The <code>JcrResourceResolverFactory</code> interface defines the service
 * interface to have JCR-based <code>ResourceResolver</code> instances created
 * for JCR sessions.
 * <p>
 * This interface is not intended to be implemented by client applications. It
 * is implemented by this bundle and the implementation registered as a service
 * for use by client applications.
 *
 * This interface is deprecated. You should use
 * {@link org.apache.sling.api.resource.ResourceResolverFactory}
 * instead. If you need a resource resolver based on an existing session
 * you can create an authentication map just containing this session
 * (using the key {@link JcrResourceConstants#AUTHENTICATION_INFO_SESSION})
 * and then call {@link org.apache.sling.api.resource.ResourceResolverFactory#getResourceResolver(java.util.Map)}
 * with exactly this map.
 *
 * @deprecated Since 2.1. Use the
 *             {@link org.apache.sling.api.resource.ResourceResolverFactory}
 */
@Deprecated
public interface JcrResourceResolverFactory extends ResourceResolverFactory {

    /**
     * Returns a <code>ResourceResolver</code> for the given session. Calling
     * this method repeatedly returns a new instance on each call.
     * <p>
     * This method is equivalent to:
     *
     * <pre>
     * Map&lt;String, Object&gt; authInfo = new HashMap&lt;String, Object&gt;();
     * authInfo.put(SESSION, session);
     * return getResourceResolver(authInfo);
     * </pre>
     * <p>
     * <b>Note:</b> Closing the <code>ResourceResolver</code> returned by this
     * method will <b>not</b> close the provided <code>Session</code> ! Likewise
     * the provided <code>Session</code> should not be logged out before closing
     * the returned <code>ResourceResolver</code>.
     *
     * @param session The JCR <code>Session</code> used by the created resource
     *            manager to access the repository.
     * @return the resource resolver
     */
    ResourceResolver getResourceResolver(Session session);

}
