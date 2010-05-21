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
     * ensured to be mapped to the Sling namespace prefix <em>sling</em> for
     * any session used by the JCR Resource bundle through the
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
}
