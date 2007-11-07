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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.sling.api.resource.ResourceManager;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceManagerFactory;
import org.apache.sling.jcr.resource.internal.loader.Loader;
import org.apache.sling.jcr.resource.internal.mapping.ObjectContentManagerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

/**
 * The <code>JcrResourceManagerFactoryImpl</code> is the
 * {@link JcrResourceManagerFactory} service providing the following
 * functionality:
 * <ul>
 * <li><code>JcrResourceManagerFactory</code> service
 * <li>Bundle listener to load initial content and manage OCM mapping
 * descriptors provided by bundles.
 * <li>Fires OSGi EventAdmin events on behalf of internal helper objects
 * </ul>
 *
 * @scr.component immediate="true" label="%content.name"
 *                description="%content.description" metatype="false"
 * @scr.property name="service.description" value="Sling
 *               JcrResourceManagerFactory Implementation"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service interface="org.apache.sling.jcr.resource.JcrResourceManagerFactory"
 */
public class JcrResourceManagerFactoryImpl implements
        JcrResourceManagerFactory, SynchronousBundleListener {

    /**
     * The JCR Repository we access to resolve resources
     *
     * @scr.reference
     */
    private SlingRepository repository;

    /**
     * The OSGi EventAdmin service used to dispatch events
     *
     * @scr.reference cardinality="0..1" policy="dynamic"
     */
    private EventAdmin eventAdmin;

    /**
     * The MimeTypeService used by the initial content initialContentLoader to
     * resolve MIME types for files to be installed.
     *
     * @scr.reference cardinality="0..1" policy="dynamic"
     */
    private MimeTypeService mimeTypeService;

    /** The OSGi Component Context */
    private ComponentContext componentContext;

    /**
     * Map of administrative sessions used to check item existence. Indexed by
     * workspace name. The map is filled on-demand. The sessions are closed when
     * the factory is deactivated.
     *
     * @see #itemReallyExists(Session, String)
     */
    private Map<String, Session> adminSessions = new HashMap<String, Session>();

    /**
     * The {@link ObjectContentManagerFactory} used retrieve object content
     * managers on-demand on behalf of {@link JcrResourceManager} instances.
     */
    private ObjectContentManagerFactory objectContentManagerFactory;

    /**
     * The initial content loader which is called to load initial content up
     * into the repository when the providing bundle is installed.
     */
    private Loader initialContentLoader;

    // ---------- ContentManagerFactory ----------------------------------------

    /**
     * Returns a new <code>ResourceManager</code> for the given session. Note
     * that each call to this method returns a new resource manager instance.
     */
    public ResourceManager getResourceManager(Session session) {
        return new JcrResourceManager(this, session);
    }

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
        //    This is synchronous - take care to not block the system !!
        //

        switch (event.getType()) {
            case BundleEvent.INSTALLED:
                // register content and types when the bundle content is
                // available
                Session session = null;
                try {
                    session = getRepository().loginAdministrative(null);
                    initialContentLoader.registerBundle(session,
                        event.getBundle());
                } catch (RepositoryException re) {
                    // TODO: log and/or handle !!
                } finally {
                    if (session != null) {
                        session.logout();
                    }
                }
                break;

            case BundleEvent.STARTING: // STARTED:
                // register mappings before the bundle gets activated
                objectContentManagerFactory.registerMapperClient(event.getBundle());
                break;

            case BundleEvent.STOPPED:
                // remove mappings after the bundle has stopped
                objectContentManagerFactory.unregisterMapperClient(event.getBundle());
                break;

            case BundleEvent.UNINSTALLED:
                initialContentLoader.unregisterBundle(event.getBundle());
                break;
        }
    }

    // ---------- EventAdmin Event Dispatching

    /**
     * Fires an OSGi event through the EventAdmin service.
     *
     * @param sourceBundle The Bundle from which the event originates. This may
     *            be <code>null</code> if there is no originating bundle.
     * @param eventName The name of the event
     * @param props Event properties. This must not be <code>null</code>.
     * @throws NullPointerException if eventName or props is <code>null</code>.
     */
    public void fireEvent(Bundle sourceBundle, String eventName,
            Map<String, Object> props) {
        // check event admin service, return if not available
        EventAdmin ea = eventAdmin;
        if (ea == null) {
            return;
        }

        // get a private copy of the properties
        Dictionary<String, Object> table = new Hashtable<String, Object>(props);

        // service information of this JcrResourceManagerFactoryImpl service
        ServiceReference sr = componentContext.getServiceReference();
        if (sr != null) {
            table.put(EventConstants.SERVICE, sr);
            table.put(EventConstants.SERVICE_ID,
                sr.getProperty(org.osgi.framework.Constants.SERVICE_ID));
            table.put(EventConstants.SERVICE_OBJECTCLASS,
                sr.getProperty(org.osgi.framework.Constants.OBJECTCLASS));
            if (sr.getProperty(org.osgi.framework.Constants.SERVICE_PID) != null) {
                table.put(EventConstants.SERVICE_PID,
                    sr.getProperty(org.osgi.framework.Constants.SERVICE_PID));
            }
        }

        // source bundle information (if available)
        if (sourceBundle != null) {
            table.put(EventConstants.BUNDLE_SYMBOLICNAME,
                sourceBundle.getSymbolicName());
        }

        // timestamp the event
        table.put(EventConstants.TIMESTAMP,
            new Long(System.currentTimeMillis()));

        // create the event
        ea.postEvent(new Event(eventName, table));
    }

    // ---------- Implementation helpers

    /** return the ObjectContentManager, used by JcrResourceManager */
    ObjectContentManager getObjectContentManager(Session session) {
        return objectContentManagerFactory.getObjectContentManager(session);
    }

    /** check existence of an item with admin session, used by JcrResourceManager */
    boolean itemReallyExists(Session clientSession, String path)
            throws RepositoryException {

        Session adminSession;
        synchronized (adminSessions) {
            String workSpace = clientSession.getWorkspace().getName();
            adminSession = adminSessions.get(workSpace);
            if (adminSession == null) {
                adminSession = getRepository().loginAdministrative(workSpace);
                adminSessions.put(workSpace, adminSession);
            }
        }

        // assume this session has more access rights than the client Session
        return adminSession.itemExists(path);
    }

    /** Returns the JCR repository used by this factory */
    private SlingRepository getRepository() {
        return repository;
    }

    /** Returns the MIME type from the MimeTypeService for the given name */
    public String getMimeType(String name) {
        // local copy to not get NPE despite check for null due to concurrent
        // unbind
        MimeTypeService mts = mimeTypeService;
        return (mts != null) ? mts.getMimeType(name) : null;
    }

    // ---------- SCR Integration

    protected void activate(ComponentContext componentContext) {
        this.componentContext = componentContext;
        this.initialContentLoader = new Loader(this);
        this.objectContentManagerFactory = new ObjectContentManagerFactory(this);

        componentContext.getBundleContext().addBundleListener(this);

        Session session = null;
        try {
            session = getRepository().loginAdministrative(null);
        } catch (RepositoryException re) {
            // TODO: log and handle
        }

        Bundle[] bundles = componentContext.getBundleContext().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
                // load content for bundles which are neither INSTALLED nor
                // UNINSTALLED
                initialContentLoader.registerBundle(session, bundles[i]);
            }

            if (bundles[i].getState() == Bundle.ACTIVE) {
                // register active bundles with the mapper client
                objectContentManagerFactory.registerMapperClient(bundles[i]);
            }
        }
    }

    protected void deactivate(ComponentContext oldContext) {
        componentContext.getBundleContext().removeBundleListener(this);

        objectContentManagerFactory.dispose();
        initialContentLoader.dispose();
        componentContext = null;

        Session[] sessions = adminSessions.values().toArray(
            new Session[adminSessions.size()]);
        adminSessions.clear();
        for (int i = 0; i < sessions.length; i++) {
            sessions[i].logout();
        }
    }
}
