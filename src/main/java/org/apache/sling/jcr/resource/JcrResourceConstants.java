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

import org.apache.sling.api.SlingConstants;

/**
 * The <code>JcrResourceConstants</code> interface provides constant values.
 */
public class JcrResourceConstants {

    /**
     * The namespace URI used by Sling JCR for items and node types used by
     * Sling (value is "http://sling.apache.org/jcr/sling/1.0"). This URI is
     * ensured to be mapped to the Sling namespace prefix <em>sling</em> for any
     * session used by the JCR Resource bundle through the
     * <code>Sling-Namespaces</code> bundle manifest header.
     */
    public static final String SLING_NAMESPACE_URI = SlingConstants.NAMESPACE_URI_ROOT
        + "jcr/sling/1.0";

    /**
     * The name of the JCR Property that defines the resource type of this node
     * (value is "sling:resourceType"). The resource manager implementation of
     * this bundle uses this property to defined the resource type of a loaded
     * resource. If this property does not exist the primary node type is used
     * as the resource type.
     */
    public static final String SLING_RESOURCE_TYPE_PROPERTY = "sling:resourceType";

    /**
     * The name of the JCR Property that defines the resource super type (value
     * is "sling:resourceSuperType"). The resource manager implementation of
     * this bundle uses this property to defined the resource type of a loaded
     * resource. If this property does not exist any non-mixin base type of the
     * the primary node type is used as the resource super type.
     */
    public static final String SLING_RESOURCE_SUPER_TYPE_PROPERTY = "sling:resourceSuperType";

    /**
     * The name of the property providing the JCR credentials to be used by the
     * resource resolver factory method instead of the <code>user.name</code>
     * and <code>user.password</code> properties. If this propery is provided
     * and set to an object of type <code>javax.jcr.Credentials</code> the
     * <code>user.name</code> property is ignored.
     * <p>
     * This property is ignored by the
     * {@link org.apache.sling.api.resource.ResourceResolverFactory#getAdministrativeResourceResolver(java.util.Map)}
     * method or if the authentication info has a
     * {@link #AUTHENTICATION_INFO_SESSION} property set to a
     * <code>javax.jcr.Session</code> object.
     * <p>
     * The type of this property, if present, is
     * <code>javax.jcr.Credentials</code>.
     *
     * @since 2.1
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getResourceResolver(java.util.Map)
     */
    public static final String AUTHENTICATION_INFO_CREDENTIALS = "user.jcr.credentials";

    /**
     * The name of the authentication info property containing the workspace
     * name to which the JCR based resource resolver should provide access.
     * <p>
     * The type of this property, if present, is <code>String</code>.
     *
     * @since 2.1
     */
    public static final String AUTHENTICATION_INFO_WORKSPACE = "user.jcr.workspace";

    /**
     * The name of the authentication info property containing a JCR Session to
     * which a JCR based resource resolver should provide access. If this
     * property is set in the authentication info map, all other properties are
     * ignored for the creation of the resource resolver with the exception of
     * the <code>user.impersonation</code> which is still respected.
     * <p>
     * The session provided by as this property and used as the basis of newly
     * created resource resolver must not be logged out before the resource
     * resolver is closed. On the other closing the resource resolver not logout
     * this session.
     * <p>
     * This property is ignored by the
     * {@link org.apache.sling.api.resource.ResourceResolverFactory#getAdministrativeResourceResolver(java.util.Map)}
     * method.
     * <p>
     * The type of this property, if present, is <code>javax.jcr.Session</code>.
     *
     * @since 2.1
     */
    public static final String AUTHENTICATION_INFO_SESSION = "user.jcr.session";

    /**
     * Constant for the sling:Folder node type
     * @since 2.2
     */
    public static final String NT_SLING_FOLDER = "sling:Folder";

    /**
     * Constant for the sling:OrderedFolder node type
     * @since 2.2
     */
    public static final String NT_SLING_ORDERED_FOLDER = "sling:OrderedFolder";
}
