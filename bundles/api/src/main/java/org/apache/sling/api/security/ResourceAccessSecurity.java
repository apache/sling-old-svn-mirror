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

/**
 * The <code>ResourceAccessSecurity</code> defines a service API which might be
 * used in implementations of resource providers where the underlying
 * persistence layer does not implement access control. The goal is to make it 
 * easy to implement a lightweight access control in such providers.
 * 
 * Expected to only be implemented once in the framework/application (much
 * like the OSGi LogService or ConfigurationAdmin Service) - ResourceProvider
 * implementations are encouraged to use this service for access control unless
 * the underlying storage already provides it.
 * 
 * JCR resource providers should *not* use this - in a JCR context, security is
 * fully delegated to the underlying repository, and mixing security models would
 * be a bad idea.
 */

public interface ResourceAccessSecurity {

    /** If supplied Resource can be read, return it (or a wrapped
     *  variant of it). The returned Resource should then be used
     *  instead of the one that was passed into the method.
     *  @return null if {@link Resource} cannot be read
     */
    public Resource getReadableResource(Resource resource);

    /** @return true if a {@link Resource} can be created at the supplied 
     *  absolute path. */
    public boolean canCreate(String absPathName, ResourceResolver resourceResolver);

    /** @return true if supplied {@link Resource} can be updated */ 
    public boolean canUpdate(Resource resource);

    /** @return true if supplied {@link Resource} can be deleted */ 
    public boolean canDelete(Resource resource);

    /** @return true if supplied {@link Resource} can be executed as a script */ 
    public boolean canExecute(Resource resource);

    /** @return true if the "valueName" value of supplied {@link Resource} can be read */ 
    public boolean canReadValue(Resource resource, String valueName);

    /** @return true if the "valueName" value of supplied {@link Resource} can be set */ 
    public boolean canSetValue(Resource resource, String valueName);

    /** @return true if the "valueName" value of supplied {@link Resource} can be deleted */ 
    public boolean canDeleteValue(Resource resource, String valueName);

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
    public String transformQuery(String query, String language, ResourceResolver resourceResolver)
            throws AccessSecurityException;

}