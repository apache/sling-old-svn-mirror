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
package org.apache.sling.jcr.contentloader.internal;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.engine.SlingSettingsService;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ContentLoaderService</code> is the service
 * providing the following functionality:
 * <ul>
 * <li>Bundle listener to load initial content.
 * <li>Fires OSGi EventAdmin events on behalf of internal helper objects
 * </ul>
 *
 */
@Component(metatype=false)
@Properties({
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="service.description", value="Apache Sling Content Loader Implementation")
})
public class ContentLoaderService implements SynchronousBundleListener, JcrContentHelper {

    public static final String PROPERTY_CONTENT_LOADED = "content-loaded";
    public static final String PROPERTY_CONTENT_LOADED_AT = "content-load-time";
    private static final String PROPERTY_CONTENT_LOADED_BY = "content-loaded-by";
    private static final String PROPERTY_CONTENT_UNLOADED_AT = "content-unload-time";
    private static final String PROPERTY_CONTENT_UNLOADED_BY = "content-unloaded-by";
    public static final String PROPERTY_UNINSTALL_PATHS = "uninstall-paths";

    public static final String BUNDLE_CONTENT_NODE = "/var/sling/bundle-content";

    /** default log */
    final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The JCR Repository we access to resolve resources
     */
    @Reference
    private SlingRepository repository;

    /**
     * The MimeTypeService used by the initial content initialContentLoader to
     * resolve MIME types for files to be installed.
     */
    @Reference
    private MimeTypeService mimeTypeService;

    /**
     * The initial content loader which is called to load initial content up
     * into the repository when the providing bundle is installed.
     */
    private Loader initialContentLoader;

    /**
     * The id of the current instance
     */
    private String slingId;

    /**
     * List of currently updated bundles.
     */
    private final Set<String> updatedBundles = new HashSet<String>();

    /** Sling settings service. */
    @Reference
    protected SlingSettingsService settingsService;

    // ---------- BundleListener -----------------------------------------------

    /**
     * Loads and unloads any content provided by the bundle whose state
     * changed. If the bundle has been started, the content is loaded. If
     * the bundle is about to stop, the content are unloaded.
     *
     * @param event The <code>BundleEvent</code> representing the bundle state
     *            change.
     */
    public void bundleChanged(BundleEvent event) {

        //
        // NOTE:
        // This is synchronous - take care to not block the system !!
        //

        Session session = null;
        final Bundle bundle = event.getBundle();
        switch (event.getType()) {
            case BundleEvent.STARTING:
                // register content when the bundle content is available
                // as node types are registered when the bundle is installed
                // we can safely add the content at this point.
                try {
                    session = this.getSession();
                    final boolean isUpdate;
                    synchronized ( this.updatedBundles ) {
                        isUpdate = this.updatedBundles.remove(bundle.getSymbolicName());
                    }
                    initialContentLoader.registerBundle(session, bundle, isUpdate);
                } catch (Throwable t) {
                    log.error(
                        "bundleChanged: Problem loading initial content of bundle "
                            + bundle.getSymbolicName() + " ("
                            + bundle.getBundleId() + ")", t);
                } finally {
                    this.ungetSession(session);
                }
                break;
            case BundleEvent.UPDATED:
                // we just add the symbolic name to the list of updated bundles
                // we will use this info when the new start event is triggered
                synchronized ( this.updatedBundles ) {
                    this.updatedBundles.add(bundle.getSymbolicName());
                }
                break;
            case BundleEvent.UNINSTALLED:
                try {
                    session = this.getSession();
                    initialContentLoader.unregisterBundle(session, bundle);
                } catch (Throwable t) {
                    log.error(
                        "bundleChanged: Problem unloading initial content of bundle "
                            + bundle.getSymbolicName() + " ("
                            + bundle.getBundleId() + ")", t);
                } finally {
                    this.ungetSession(session);
                }
                break;
        }
    }

    // ---------- Implementation helpers --------------------------------------

    /** Returns the MIME type from the MimeTypeService for the given name */
    public String getMimeType(String name) {
        // local copy to not get NPE despite check for null due to concurrent
        // unbind
        MimeTypeService mts = mimeTypeService;
        return (mts != null) ? mts.getMimeType(name) : null;
    }

    protected void createRepositoryPath(final Session writerSession, final String repositoryPath)
    throws RepositoryException {
        if ( !writerSession.itemExists(repositoryPath) ) {
            Node node = writerSession.getRootNode();
            String path = repositoryPath.substring(1);
            int pos = path.lastIndexOf('/');
            if ( pos != -1 ) {
                final StringTokenizer st = new StringTokenizer(path.substring(0, pos), "/");
                while ( st.hasMoreTokens() ) {
                    final String token = st.nextToken();
                    if ( !node.hasNode(token) ) {
                        node.addNode(token, "sling:Folder");
                        node.save();
                    }
                    node = node.getNode(token);
                }
                path = path.substring(pos + 1);
            }
            if ( !node.hasNode(path) ) {
                node.addNode(path, "sling:Folder");
                node.save();
            }
        }
    }

    // ---------- SCR Integration ---------------------------------------------

    /** Activates this component, called by SCR before registering as a service */
    protected void activate(ComponentContext componentContext) {
        this.slingId = this.settingsService.getSlingId();
        this.initialContentLoader = new Loader(this);

        componentContext.getBundleContext().addBundleListener(this);

        Session session = null;
        try {
            session = this.getSession();
            this.createRepositoryPath(session, ContentLoaderService.BUNDLE_CONTENT_NODE);
            log.debug(
                    "Activated - attempting to load content from all "
                    + "bundles which are neither INSTALLED nor UNINSTALLED");

            int ignored = 0;
            Bundle[] bundles = componentContext.getBundleContext().getBundles();
            for (Bundle bundle : bundles) {
                if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {

                    // load content for bundles which are neither INSTALLED nor
                    // UNINSTALLED
                    try {
                        initialContentLoader.registerBundle(session, bundle, false);
                    } catch (Throwable t) {
                        log.error(
                            "Problem loading initial content of bundle "
                                + bundle.getSymbolicName() + " ("
                                + bundle.getBundleId() + ")", t);
                    } finally {
                        if ( session.hasPendingChanges() ) {
                            session.refresh(false);
                        }
                    }
                } else {
                    ignored++;
                }

            }

            log.debug(
                    "Out of {} bundles, {} were not in a suitable state for initial content loading",
                    bundles.length, ignored
                    );

        } catch (Throwable t) {
            log.error("activate: Problem while loading initial content and"
                + " registering mappings for existing bundles", t);
        } finally {
            this.ungetSession(session);
        }
    }

    /** Deactivates this component, called by SCR to take out of service */
    protected void deactivate(ComponentContext componentContext) {
        componentContext.getBundleContext().removeBundleListener(this);

        if ( this.initialContentLoader != null ) {
            this.initialContentLoader.dispose();
            this.initialContentLoader = null;
        }
    }

    // ---------- internal helper ----------------------------------------------

    /** Returns the JCR repository used by this service. */
    protected SlingRepository getRepository() {
        return repository;
    }

    /**
     * Returns an administrative session to the default workspace.
     */
    private Session getSession()
    throws RepositoryException {
        return getRepository().loginAdministrative(null);
    }

    /**
     * Return the administrative session and close it.
     */
    private void ungetSession(final Session session) {
        if ( session != null ) {
            try {
                session.logout();
            } catch (Throwable t) {
                log.error("Unable to log out of session: " + t.getMessage(), t);
            }
        }
    }

    /**
     * Return the bundle content info and make an exclusive lock.
     * @param session
     * @param bundle
     * @return The map of bundle content info or null.
     * @throws RepositoryException
     */
    public Map<String, Object> getBundleContentInfo(final Session session, final Bundle bundle, boolean create)
    throws RepositoryException {
        final String nodeName = bundle.getSymbolicName();
        final Node parentNode = (Node)session.getItem(BUNDLE_CONTENT_NODE);
        if ( !parentNode.hasNode(nodeName) ) {
            if ( !create ) {
                return null;
            }
            try {
                final Node bcNode = parentNode.addNode(nodeName, "nt:unstructured");
                bcNode.addMixin("mix:lockable");
                parentNode.save();
            } catch (RepositoryException re) {
                // for concurrency issues (running in a cluster) we ignore exceptions
                this.log.warn("Unable to create node " + nodeName, re);
                session.refresh(true);
            }
        }
        final Node bcNode = parentNode.getNode(nodeName);
        if ( bcNode.isLocked() ) {
            return null;
        }
        try {
            bcNode.lock(false, true);
        } catch (LockException le) {
            return null;
        }
        final Map<String, Object> info = new HashMap<String, Object>();
        if ( bcNode.hasProperty(ContentLoaderService.PROPERTY_CONTENT_LOADED_AT)) {
            info.put(ContentLoaderService.PROPERTY_CONTENT_LOADED_AT, bcNode.getProperty(ContentLoaderService.PROPERTY_CONTENT_LOADED_AT).getDate());
        }
        if ( bcNode.hasProperty(ContentLoaderService.PROPERTY_CONTENT_LOADED) ) {
            info.put(ContentLoaderService.PROPERTY_CONTENT_LOADED,
                    bcNode.getProperty(ContentLoaderService.PROPERTY_CONTENT_LOADED).getBoolean());
        } else {
            info.put(ContentLoaderService.PROPERTY_CONTENT_LOADED, false);
        }
        if ( bcNode.hasProperty(ContentLoaderService.PROPERTY_UNINSTALL_PATHS) ) {
            final Value[] values = bcNode.getProperty(PROPERTY_UNINSTALL_PATHS).getValues();
            final String[] s = new String[values.length];
            for(int i=0; i<values.length; i++) {
                s[i] = values[i].getString();
            }
            info.put(ContentLoaderService.PROPERTY_UNINSTALL_PATHS, s);
        }
        return info;
    }

    public void unlockBundleContentInfo(final Session session,
                                        final Bundle  bundle,
                                        final boolean contentLoaded,
                                        final List<String> createdNodes)
    throws RepositoryException {
        final String nodeName = bundle.getSymbolicName();
        final Node parentNode = (Node)session.getItem(BUNDLE_CONTENT_NODE);
        final Node bcNode = parentNode.getNode(nodeName);
        if ( contentLoaded ) {
            bcNode.setProperty(ContentLoaderService.PROPERTY_CONTENT_LOADED, contentLoaded);
            bcNode.setProperty(PROPERTY_CONTENT_LOADED_AT, Calendar.getInstance());
            bcNode.setProperty(PROPERTY_CONTENT_LOADED_BY, this.slingId);
            bcNode.setProperty(PROPERTY_CONTENT_UNLOADED_AT, (String)null);
            bcNode.setProperty(PROPERTY_CONTENT_UNLOADED_BY, (String)null);
            if ( createdNodes != null && createdNodes.size() > 0 ) {
                bcNode.setProperty(PROPERTY_UNINSTALL_PATHS, createdNodes.toArray(new String[createdNodes.size()]));
            }
            bcNode.save();
        }
        bcNode.unlock();
    }

    public void contentIsUninstalled(final Session session,
                                     final Bundle  bundle) {
        final String nodeName = bundle.getSymbolicName();
        try {
            final Node parentNode = (Node)session.getItem(BUNDLE_CONTENT_NODE);
            if ( parentNode.hasNode(nodeName) ) {
                final Node bcNode = parentNode.getNode(nodeName);
                bcNode.setProperty(ContentLoaderService.PROPERTY_CONTENT_LOADED, false);
                bcNode.setProperty(PROPERTY_CONTENT_UNLOADED_AT, Calendar.getInstance());
                bcNode.setProperty(PROPERTY_CONTENT_UNLOADED_BY, this.slingId);
                bcNode.setProperty(PROPERTY_UNINSTALL_PATHS, (String[])null);
                bcNode.save();
            }
        } catch (RepositoryException re) {
            this.log.error("Unable to update bundle content info.", re);
        }
    }
}
