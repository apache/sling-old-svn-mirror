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
package org.apache.sling.api.security;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>ResourceAccessSecurity</code> defines a service API which is
 * used in two different context: for securing resource providers which
 * have no own access control and on the application level to further
 * restrict the access to resources in general.
 *
 * A resource access security service is registered with the service
 * property {@link #CONTEXT}. Allowed values are {@link #APPLICATION_CONTEXT}
 * and {@link #PROVIDER_CONTEXT}. If the value is missing it defaults
 * to {@link #PROVIDER_CONTEXT}. Services registered with an invalid
 * value, will be ignored.
 *
 * In the context of resource providers, this service might be used
 * for implementations of resource providers where the underlying persistence
 * layer does not implement access control. The goal is to make it easy to implement
 * a lightweight access control for such providers. For example, a JCR resource
 * providers should *not* use the provider context resource access security - in a
 * JCR context, security is fully delegated to the underlying repository, and
 * mixing security models would be a bad idea.
 *
 * In the context of the application, this service might be used to add
 * additional or temporary constraints across the whole resource tree.
 *
 * It is expected to only have a single service per context in the
 * framework/application (much like the OSGi LogService or ConfigurationAdmin Service).
 * In the case of multiple services per context, the one with the highest
 * service ranking is used.
 */
@ProviderType
public interface ResourceAccessSecurity {

    /**
     * The name of the service registration property containing the context
     * of this service. Allowed values are {@link #APPLICATION_CONTEXT} and
     * {@link #PROVIDER_CONTEXT}.
     * The default for this value is <code>{@link #PROVIDER_CONTEXT}</code>.
     * (value is "access.context")
     */
    String CONTEXT = "access.context";

    /**
     * Allowed value for the {@link #CONTEXT} service registration property.
     * Services marked with this context are applied to all resources.
     */
    String APPLICATION_CONTEXT = "application";

    /**
     * Allowed value for the {@link #CONTEXT} service registration property.
     * Services marked with this context are only applied to resource
     * providers which indicate the additional checks with the
     * {@link org.apache.sling.api.resource.ResourceProvider#USE_RESOURCE_ACCESS_SECURITY}
     * property.
     */
    String PROVIDER_CONTEXT = "provider";

    /** If supplied Resource can be read, return it (or a wrapped
     *  variant of it). The returned Resource should then be used
     *  instead of the one that was passed into the method.
     *  @return null if {@link Resource} cannot be read
     */
    Resource getReadableResource(Resource resource);

    /** @return true if a {@link Resource} can be created at the supplied
     *  absolute path. */
    boolean canCreate(String absPathName, ResourceResolver resourceResolver);

    /** @return true if supplied {@link Resource} can be updated */
    boolean canUpdate(Resource resource);

    /** @return true if supplied {@link Resource} can be deleted */
    boolean canDelete(Resource resource);

    /** @return true if supplied {@link Resource} can be executed as a script */
    boolean canExecute(Resource resource);

    /** @return true if the "valueName" value of supplied {@link Resource} can be read */
    boolean canReadValue(Resource resource, String valueName);

    /** @return true if the "valueName" value of supplied {@link Resource} can be set */
    boolean canSetValue(Resource resource, String valueName);

    /** @return true if the "valueName" value of supplied {@link Resource} can be deleted */
    boolean canDeleteValue(Resource resource, String valueName);

    /**
     * Optionally transform a query based on the current
     * user's credentials. Can be used to narrow down queries to omit results
     * that the current user is not allowed to see anyway, to speed up
     * downstream access control.
     *
     * Query transformations are not critical with respect to access control as results
     * are filtered downstream using the canRead.. methods.
     *
     * @param query the query
     * @param language the language in which the query is expressed
     * @param resourceResolver the resource resolver which resolves the query
     * @return the transformed query
     * @throws AccessSecurityException
     */
    String transformQuery(String query, String language, ResourceResolver resourceResolver)
    throws AccessSecurityException;

}