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
package org.apache.sling.servlets.post.impl.helper;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRSupport {

    public static final JCRSupport INSTANCE = new JCRSupport();

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Object supportImpl;

    public JCRSupport() {
        Object impl = null;
        try {
            impl = new JCRSupportImpl();
        } catch ( final Throwable t) {
            logger.warn("Support for JCR operations like checkin, checkout, import, ordering etc. is currently disabled " +
                        "in the servlets post module. Check whether the JCR API is available.");
        }
        this.supportImpl = impl;
    }

    public void orderNode(final SlingHttpServletRequest request,
            final Resource resource,
            final List<Modification> changes) throws PersistenceException {
        if ( supportImpl != null ) {
            ((JCRSupportImpl)supportImpl).orderNode(request, resource, changes);
        }
    }

    public boolean checkin(final Resource rsrc)
    throws PersistenceException {
        if ( rsrc != null && supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).checkin(rsrc);
        }
        return false;
    }

    public void checkoutIfNecessary(final Resource rsrc,
            final List<Modification> changes,
            final VersioningConfiguration versioningConfiguration)
    throws PersistenceException {
        if ( rsrc != null && supportImpl != null ) {
            ((JCRSupportImpl)supportImpl).checkoutIfNecessary(rsrc, changes, versioningConfiguration);
        }
    }

    public boolean isNode(final Resource rsrc) {
        if ( rsrc != null && supportImpl != null ) {
            return  ((JCRSupportImpl)supportImpl).isNode(rsrc);
        }
        return false;
    }

    public boolean isVersionable(final Resource rsrc) throws PersistenceException {
        if ( supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).isVersionable(rsrc);
        }
        return false;
    }

    public boolean isNodeType(final Resource rsrc, final String typeHint) {
        if ( rsrc != null && supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).isNodeType(rsrc, typeHint);
        }
        return false;
    }

    public Boolean isFileNodeType(final ResourceResolver resolver, final String nodeType) {
        if ( supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).isFileNodeType(resolver, nodeType);
        }
        return false;
    }

    public boolean isPropertyProtectedOrNewAutoCreated(final Object node, final String name)
    throws PersistenceException {
        if ( node != null && supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).isPropertyProtectedOrNewAutoCreated(node, name);
        }
        return false;
    }

    public boolean isPropertyMandatory(final Object node, final String name)
    throws PersistenceException {
        if ( node != null && supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).isPropertyMandatory(node, name);
        }
        return false;
    }

    public boolean isPropertyMultiple(final Object node, final String name)
    throws PersistenceException {
        if ( node != null && supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).isPropertyMultiple(node, name);
        }
        return false;
    }

    public boolean isNewNode(final Object node) {
        if ( node != null && supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).isNewNode(node);
        }
        return true;
    }

    public Integer getPropertyType(final Object node, final String name)
    throws PersistenceException {
        if ( node != null && supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).getPropertyType(node, name);
        }
        return null;
    }

    public boolean hasSession(final ResourceResolver resolver) {
        if ( supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).hasSession(resolver);
        }
        return false;
    }

    /**
     * Stores property value(s) as reference(s). Will parse the reference(s) from the string
     * value(s) in the {@link RequestProperty}.
     *
     * @return true only if parsing was successful and the property was actually changed
     */
    public Modification storeAsReference(
            final Resource resource,
            final Object node,
            final String name,
            final String[] values,
            final int type,
            final boolean multiValued)
    throws PersistenceException {
        if ( node != null && supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).storeAsReference(node, name, values, type, multiValued);
        }
        throw new PersistenceException("Resource " + resource.getPath() + " does not support reference properties.", null, resource.getPath(), name);
    }

    public void setTypedProperty(final Object n,
            final String name,
            final String[] values,
            final int type,
            final boolean multiValued)
    throws PersistenceException {
        if ( n != null && supportImpl != null ) {
            ((JCRSupportImpl)supportImpl).setTypedProperty(n, name, values, type, multiValued);
        } else {
            throw new PersistenceException("Property should be stored through JCR but JCR support is not available");
        }
    }

    public Object getNode(final Resource rsrc) {
        if ( supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).getNode(rsrc);
        }
        return null;
    }

    public Object getItem(final Resource rsrc) {
        if ( supportImpl != null ) {
            return ((JCRSupportImpl)supportImpl).getItem(rsrc);
        }
        return null;
    }

    public void setPrimaryNodeType(final Object node, final String type)
    throws PersistenceException {
        if ( node != null && supportImpl != null ) {
            ((JCRSupportImpl)supportImpl).setPrimaryNodeType(node, type);
        } else {
            throw new PersistenceException("Node type should be set but JCR support is not available");
        }
    }

    public String copy(Object src, Object dstParent, String name)
    throws PersistenceException {
        // the caller already got an item and a node, so supportImpl is available
        return ((JCRSupportImpl)supportImpl).copy(src, dstParent, name);
    }

    public void move(Object src, Object dstParent, String name)
    throws PersistenceException {
        // the caller already got an item and a node, so supportImpl is available
        ((JCRSupportImpl)supportImpl).move(src, dstParent, name);
    }

    public boolean jcrEnabled() {
        return this.supportImpl != null;
    }
}
