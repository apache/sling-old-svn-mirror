/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.jcr.jackrabbit.server.impl.security;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.DefaultAccessManager;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.sling.jcr.jackrabbit.server.impl.AccessManagerFactoryTracker;
import org.apache.sling.jcr.jackrabbit.server.impl.Activator;
import org.apache.sling.jcr.jackrabbit.server.security.accessmanager.AccessManagerPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.accessmanager.AccessManagerPluginFactory;
import org.apache.sling.jcr.jackrabbit.server.security.accessmanager.WorkspaceAccessManagerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows to plugin a custom <code>AccessManager</code> as an OSGi bundle:
 * <ol>
 *   <li>Set this class as <code>AccessManager</code> in your <code>repository.xml</code></li>
 *   <li>Implement <code>o.a.s.j.j.s.s.a.AccessManagerPluginFactory</code></li>
 * </ol>
 *
 * <p>If <code>PluggableDefaultAccessManager</code> is specified in <code>repository.xml</code>, and no
 * implementation of <code>AccessManagerPluginFactory</code> exists, all calls will fall back
 * to <code>DefaultAccessManager</code>.</p>
 *
 * <p>See also <a href="https://issues.apache.org/jira/browse/SLING-880">SLING-880</a></p>
 * @see AccessManagerPluginFactory
 */
public class PluggableDefaultAccessManager extends DefaultAccessManager {

    private AccessManagerPlugin accessManagerPlugin;
    private NamePathResolver namePathResolver;
    private static final Logger log = LoggerFactory.getLogger(PluggableDefaultAccessManager.class);
    protected AccessManagerPluginFactory accessManagerFactory;
    protected AccessManagerFactoryTracker accessManagerFactoryTracker;
    private Session session;
    private Subject subject;
    // only warn once, then only warn on debug level.
    private static int pluginWarning = 0;

    /**
     * the hierarchy manager used to resolve path from itemId
     */
    private HierarchyManager hierMgr;

    public PluggableDefaultAccessManager() {
    }

    protected AccessManagerPluginFactory getAccessManagerFactory() {
        return accessManagerFactoryTracker.getFactory(this);
    }

    public void init(AMContext context) throws AccessDeniedException, Exception {
        this.init(context, null, null);
    }

    public void init(AMContext context, AccessControlProvider acProvider, WorkspaceAccessManager wspAccessMgr) throws AccessDeniedException, Exception {
        accessManagerFactoryTracker = Activator.getAccessManagerFactoryTracker();
        accessManagerFactory = getAccessManagerFactory();
        if (accessManagerFactory != null) {
            this.accessManagerPlugin = accessManagerFactory.getAccessManager();
        }
        this.sanityCheck();
        super.init(context, acProvider, wspAccessMgr);
        this.namePathResolver = context.getNamePathResolver();
        if (this.accessManagerPlugin != null) {
            this.accessManagerPlugin.init(context.getSubject(), context.getSession());
        }
        this.session = context.getSession();
        this.subject = context.getSubject();

        hierMgr = context.getHierarchyManager();
    }

    public void close() throws Exception {
        this.accessManagerFactoryTracker.unregister(this);
        super.close();
        if (this.accessManagerPlugin != null) {
            this.accessManagerPlugin.close();
        }
    }

    public void endSession() {
        if (this.session != null && this.session.isLive()) {
            this.session.logout();
        }
    }

    public void checkPermission(ItemId id, int permissions) throws AccessDeniedException, ItemNotFoundException, RepositoryException {
        this.sanityCheck();
        super.checkPermission(id, permissions);
    }

    public boolean isGranted(ItemId id, int permissions) throws ItemNotFoundException, RepositoryException {
        return super.isGranted(id, permissions);
    }

    public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
        if (this.sanityCheck()) {
            return this.accessManagerPlugin.isGranted(namePathResolver.getJCRPath(absPath), permissions);
        }
        return super.isGranted(absPath, permissions);
    }

    public boolean isGranted(Path parentPath, Name childName, int permissions) throws RepositoryException {
        return super.isGranted(parentPath, childName, permissions);
    }

    public boolean canRead(Path itemPath, ItemId itemId) throws RepositoryException {
        if (this.sanityCheck()) {
        	String resolvedPath = null;
        	if (itemPath != null) {
        		resolvedPath = namePathResolver.getJCRPath(itemPath);
        	} else if (itemId != null) {
        		Path path = hierMgr.getPath(itemId);
        		resolvedPath = namePathResolver.getJCRPath(path);
        	}
            return this.accessManagerPlugin.canRead(resolvedPath);
        }
        return super.canRead(itemPath, itemId);
    }

    public boolean canAccess(String workspaceName) throws RepositoryException {
        WorkspaceAccessManagerPlugin plugin = null;
        if (this.sanityCheck()) {
            plugin = this.accessManagerPlugin.getWorkspaceAccessManager();
        }
        if (plugin != null) {
            return plugin.canAccess(workspaceName);
        }
        return super.canAccess(workspaceName);
    }

    private boolean sanityCheck() throws RepositoryException {
        if (this.accessManagerPlugin == null) {
            AccessManagerPluginFactory factory = this.accessManagerFactoryTracker.getFactory(this);
            if (factory == null) {
                if ( pluginWarning  == 0 ) {
                    pluginWarning++;
                    log.warn("No pluggable AccessManager available, falling back to DefaultAccessManager");
                } else {
                    log.debug("No pluggable AccessManager available, falling back to DefaultAccessManager");
                }
                return false;

            }
            this.accessManagerPlugin = factory.getAccessManager();
            try {
                this.accessManagerPlugin.init(this.subject, this.session);
            } catch (Exception e) {
                throw new RepositoryException(e);
            }
        }
        return true;
    }
}
