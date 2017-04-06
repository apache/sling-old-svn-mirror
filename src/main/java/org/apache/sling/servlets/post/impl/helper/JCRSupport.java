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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

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
            logger.warn("Support for JCR operations like checkin, checkout, ordering etc. is currently disabled " +
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

    public Value parse(Session session, String value, ValueFactory factory, boolean weak) throws RepositoryException {
        return ReferenceParser.parse(session, value, factory, weak);
    }

    /**
     * Parses the given source strings and returns the respective reference value
     * instances. If no node matches for any of the sources
     * returns <code>null</code>.
     * <p/>
     *
     * @param values path or UUID strings
     * @param factory the value factory
     * @param weak true to create a WeakReference value
     * @return the values or <code>null</code>
     * @throws RepositoryException
     */
    public Value[] parse(Session session, String[] values, ValueFactory factory, boolean weak) throws RepositoryException {
        return ReferenceParser.parse(session, values, factory, weak);
    }

}
