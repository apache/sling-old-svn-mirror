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
package org.apache.sling.jcr.resourcesecurity.impl;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;

@Component(configurationFactory=true, policy=ConfigurationPolicy.REQUIRE, metatype=true,
           label="Apache Sling JCR Resource Access Gate",
           description="This access gate can be used to handle the access to resources" +
                       " not backed by a JCR repository by providing ACLs in the " +
                       "reposiory")
@Service(value=ResourceAccessGate.class)
@Properties({
    @Property(name=ResourceAccessGate.PATH, label="Path",
              description="The path is a regular expression for which resources the service should be called"),
    @Property(name=ResourceAccessGateFactory.PROP_PREFIX,
              label="Deep Check Prefix",
              description="If this value is configured with a prefix and the resource path starts with this" +
                          " prefix, the prefix is removed from the path and the remaining part is appended " +
                          " to the JCR path to check. For example if /foo/a/b/c is required, this prefix is " +
                          " configured with /foo and the JCR node to check is /check, the permissions at " +
                          " /check/a/b/c are checked."),
    @Property(name=ResourceAccessGateFactory.PROP_JCR_PATH,
              label="JCR Node",
              description="This node is checked for permissions to the resources."),
    @Property(name=ResourceAccessGate.OPERATIONS, value= {"read", "create", "update", "delete"}, propertyPrivate=true),
    @Property(name=ResourceAccessGate.CONTEXT, value=ResourceAccessGate.PROVIDER_CONTEXT, propertyPrivate=true)
})
public class ResourceAccessGateFactory
    extends AllowingResourceAccessGate
    implements ResourceAccessGate {

    static final String PROP_JCR_PATH = "jcrPath";

    static final String PROP_PREFIX = "checkpath.prefix";

    private String jcrPath;

    @Activate
    protected void activate(final Map<String, Object> props) {
        this.jcrPath = PropertiesUtil.toString(props.get(PROP_JCR_PATH), null);
    }

    /**
     * Skip the check if the resource is backed by a JCR resource.
     * This is a sanity check which should usually not be required if the system
     * is configured correctly.
     */
    private boolean skipCheck(final Resource resource) {
        // if resource is backed by a JCR node, skip check
        return resource.adaptTo(Node.class) != null;
    }

    /**
     * Check the permission
     */
    private GateResult checkPermission(final Resource resource, final String permission) {
        if ( this.skipCheck(resource) ) {
            return GateResult.GRANTED;
        }
        boolean granted = false;
        final Session session = resource.getResourceResolver().adaptTo(Session.class);
        if ( session != null ) {
            try {
                granted = session.hasPermission(jcrPath, permission);
            } catch (final RepositoryException re) {
                // ignore
            }
        }
        return granted ? GateResult.GRANTED : GateResult.DENIED;
    }

    /**
     * @see org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate#hasReadRestrictions(org.apache.sling.api.resource.ResourceResolver)
     */
    @Override
    public boolean hasReadRestrictions(final ResourceResolver resourceResolver) {
        return true;
    }

    /**
     * @see org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate#hasCreateRestrictions(org.apache.sling.api.resource.ResourceResolver)
     */
    @Override
    public boolean hasCreateRestrictions(final ResourceResolver resourceResolver) {
        return true;
    }

    /**
     * @see org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate#hasUpdateRestrictions(org.apache.sling.api.resource.ResourceResolver)
     */
    @Override
    public boolean hasUpdateRestrictions(final ResourceResolver resourceResolver) {
        return true;
    }

    /**
     * @see org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate#hasDeleteRestrictions(org.apache.sling.api.resource.ResourceResolver)
     */
    @Override
    public boolean hasDeleteRestrictions(final ResourceResolver resourceResolver) {
        return true;
    }

    /**
     * @see org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate#canRead(org.apache.sling.api.resource.Resource)
     */
    @Override
    public GateResult canRead(final Resource resource) {
        return this.checkPermission(resource, Session.ACTION_READ);
    }

    /**
     * @see org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate#canDelete(org.apache.sling.api.resource.Resource)
     */
    @Override
    public GateResult canDelete(Resource resource) {
        return this.checkPermission(resource, Session.ACTION_REMOVE);
    }

    /**
     * @see org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate#canUpdate(org.apache.sling.api.resource.Resource)
     */
    @Override
    public GateResult canUpdate(Resource resource) {
        return this.checkPermission(resource, Session.ACTION_SET_PROPERTY);
    }

    /**
     * @see org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate#canCreate(java.lang.String, org.apache.sling.api.resource.ResourceResolver)
     */
    @Override
    public GateResult canCreate(String absPathName, ResourceResolver resourceResolver) {
        boolean canCreate = false;

        final Session session = resourceResolver.adaptTo(Session.class);
        if ( session != null ) {
            try {
                canCreate = session.hasPermission(jcrPath, Session.ACTION_ADD_NODE);
            } catch (final RepositoryException re) {
                // ignore
            }
        }

        return canCreate ? GateResult.GRANTED : GateResult.DENIED;
    }
}
