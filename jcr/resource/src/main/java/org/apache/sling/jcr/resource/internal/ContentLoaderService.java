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
package org.apache.sling.jcr.resource.internal;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.loader.Loader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ContentLoaderService</code> is the service
 * providing the following functionality:
 * <ul>
 * <li>Bundle listener to load initial content and manage OCM mapping
 * descriptors provided by bundles.
 * <li>Fires OSGi EventAdmin events on behalf of internal helper objects
 * </ul>
 *
 * @scr.component metatype="false"
 * @scr.property name="service.description" value="Sling
 *               Content Loader Implementation"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 */
public class ContentLoaderService implements BundleListener {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The JCR Repository we access to resolve resources
     *
     * @scr.reference
     */
    private SlingRepository repository;

    /**
     * The MimeTypeService used by the initial content initialContentLoader to
     * resolve MIME types for files to be installed.
     *
     * @scr.reference
     */
    private MimeTypeService mimeTypeService;

    /**
     * Administrative sessions used to check item existence.
     */
    private Session adminSession;

    /**
     * The initial content loader which is called to load initial content up
     * into the repository when the providing bundle is installed.
     */
    private Loader initialContentLoader;

    // ---------- BundleListener -----------------------------------------------

    /**
     * Loads and unloads any components provided by the bundle whose state
     * changed. If the bundle has been started, the components are loaded. If
     * the bundle is about to stop, the components are unloaded.
     *
     * @param event The <code>BundleEvent</code> representing the bundle state
     *            change.
     */
    public void bundleChanged(BundleEvent event) {

        //
        // NOTE:
        // This is synchronous - take care to not block the system !!
        //

        switch (event.getType()) {
            case BundleEvent.STARTING: // STARTED:
                // register content when the bundle content is available
                // as node types are registered when the bundle is installed
                // we can safely add the content at this point.
                try {
                    Session session = getAdminSession();
                    initialContentLoader.registerBundle(session,
                        event.getBundle());
                } catch (Throwable t) {
                    log.error(
                        "bundleChanged: Problem loading initial content of bundle "
                            + event.getBundle().getSymbolicName() + " ("
                            + event.getBundle().getBundleId() + ")", t);
                }

            case BundleEvent.UNINSTALLED:
                initialContentLoader.unregisterBundle(event.getBundle());
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

    // ---------- SCR Integration ---------------------------------------------

    /** Activates this component, called by SCR before registering as a service */
    protected void activate(ComponentContext componentContext) {
        this.initialContentLoader = new Loader(this);

        componentContext.getBundleContext().addBundleListener(this);

        try {
            final Session session = getAdminSession();

            log.debug(
                    "Activated - attempting to load content from all "
                    + "bundles which are neither INSTALLED nor UNINSTALLED");

            int ignored = 0;
            Bundle[] bundles = componentContext.getBundleContext().getBundles();
            for (Bundle bundle : bundles) {
                if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
                    // load content for bundles which are neither INSTALLED nor
                    // UNINSTALLED
                    initialContentLoader.registerBundle(session, bundle);
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
        }
    }

    /** Deativates this component, called by SCR to take out of service */
    protected void deactivate(ComponentContext componentContext) {
        componentContext.getBundleContext().removeBundleListener(this);

        if ( this.initialContentLoader != null ) {
            this.initialContentLoader.dispose();
            this.initialContentLoader = null;
        }

        if ( adminSession != null ) {
            this.adminSession.logout();
            this.adminSession = null;
        }
    }

    // ---------- internal helper ----------------------------------------------

    /** Returns the JCR repository used by this factory */
    protected SlingRepository getRepository() {
        return repository;
    }

    /**
     * Returns an administrative session to the given workspace or the default
     * workspaces if the supplied name is <code>null</code>.
     */
    private synchronized Session getAdminSession()
            throws RepositoryException {
        if ( adminSession == null ) {
            adminSession = getRepository().loginAdministrative(null);
        }
        return adminSession;
    }


}
