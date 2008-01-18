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

import static org.apache.sling.jcr.resource.JcrResourceConstants.BUNDLE_RESOURCE_ROOTS;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.sling.jcr.resource.internal.helper.Mapping;
import org.apache.sling.jcr.resource.internal.helper.ResourceProvider;
import org.apache.sling.jcr.resource.internal.helper.ResourceProviderEntry;
import org.apache.sling.jcr.resource.internal.helper.bundle.BundleResourceProvider;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrNodeResource;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceResolverFactoryImpl</code> is the
 * {@link JcrResourceResolverFactory} service providing the following
 * functionality:
 * <ul>
 * <li><code>JcrResourceResolverFactory</code> service
 * <li>Bundle listener to load initial content and manage OCM mapping
 * descriptors provided by bundles.
 * <li>Fires OSGi EventAdmin events on behalf of internal helper objects
 * </ul>
 *
 * @scr.component immediate="true" label="%resource.resolver.name"
 *                description="%resource.resolver.description"
 * @scr.property name="service.description" value="Sling
 *               JcrResourceResolverFactory Implementation"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service interface="org.apache.sling.jcr.resource.JcrResourceResolverFactory"
 */
public class JcrResourceResolverFactoryImpl implements
        JcrResourceResolverFactory, SynchronousBundleListener, ResourceProvider {

    /**
     * @scr.property value="true" type="Boolean"
     */
    private static final String PROP_ALLOW_DIRECT = "resource.resolver.allowDirect";

    /**
     * The resolver.virtual property has no default configuration. But the sling
     * maven plugin and the sling management console cannot handle empty
     * multivalue properties at the moment. So we just add a dummy direct
     * mapping.
     *
     * @scr.property values.1="/-/"
     */
    private static final String PROP_VIRTUAL = "resource.resolver.virtual";

    /**
     * @scr.property values.1="/-/" values.2="/content/-/"
     *               Cvalues.3="/apps/&times;/docroot/-/"
     *               Cvalues.4="/libs/&times;/docroot/-/"
     *               values.5="/system/docroot/-/"
     */
    private static final String PROP_MAPPING = "resource.resolver.mapping";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

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

    /**
     * This services ServiceReference for use in
     * {@link #fireEvent(Bundle, String, Map)}
     */
    private ServiceReference serviceReference;

    /** all mappings */
    private Mapping[] mappings;

    /** The fake urls */
    private BidiMap virtualURLMap;

    /** <code>true</code>, if direct mappings from URI to handle are allowed */
    private boolean allowDirect = false;

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
     * managers on-demand on behalf of {@link JcrResourceResolver} instances.
     */
    private ObjectContentManagerFactory objectContentManagerFactory;

    /**
     * The initial content loader which is called to load initial content up
     * into the repository when the providing bundle is installed.
     */
    private Loader initialContentLoader;

    private ResourceProviderEntry rootProviderEntry;

    private Map<Long, BundleResourceProvider> bundleResourceProviderMap = new HashMap<Long, BundleResourceProvider>();

    public JcrResourceResolverFactoryImpl() {
        this.rootProviderEntry = new ResourceProviderEntry("/", this);
    }

    // ---------- JcrResourceResolverFactory -----------------------------------

    /**
     * Returns a new <code>ResourceResolve</code> for the given session. Note
     * that each call to this method returns a new resource manager instance.
     * The resource manager returned also implements the
     * {@link org.apache.sling.jcr.resource.PathResolver} interface to which it
     * may be cast.
     */
    public ResourceResolver getResourceResolver(Session session) {
        return new JcrResourceResolver(this, session);
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
        // This is synchronous - take care to not block the system !!
        //

        switch (event.getType()) {
            case BundleEvent.STARTING: // STARTED:
                // register content when the bundle content is available
                // as node types are registered when the bundle is installed
                // we can safely add the content at this point.
                try {
                    Session session = getAdminSession(null);
                    initialContentLoader.registerBundle(session,
                        event.getBundle());
                } catch (Throwable t) {
                    log.error(
                        "bundleChanged: Problem loading initial content of bundle "
                            + event.getBundle().getSymbolicName() + " ("
                            + event.getBundle().getBundleId() + ")", t);
                }

                // register mappings before the bundle gets activated
                objectContentManagerFactory.registerMapperClient(event.getBundle());
                addBundleResourceProvider(event.getBundle());
                break;

            case BundleEvent.STOPPED:
                // remove mappings after the bundle has stopped
                objectContentManagerFactory.unregisterMapperClient(event.getBundle());
                removeBundleResourceProvider(event.getBundle());
                break;

            case BundleEvent.UNINSTALLED:
                initialContentLoader.unregisterBundle(event.getBundle());
                break;
        }
    }

    // ---------- ResourceProvider ---------------------------------------------

    public String[] getRoots() {
        return new String[] { "/" };
    }

    public Resource getResource(JcrResourceResolver jcrResourceResolver,
            String path) throws RepositoryException {
        return jcrResourceResolver.createResource(path);
    }

    // ---------- EventAdmin Event Dispatching ---------------------------------

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

        // service information of this JcrResourceResolverFactoryImpl service
        ServiceReference sr = serviceReference;
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

    // ---------- Implementation helpers --------------------------------------

    /** return the ObjectContentManager, used by JcrResourceResolver */
    ObjectContentManager getObjectContentManager(Session session) {
        return objectContentManagerFactory.getObjectContentManager(session);
    }

    /** check existence of an item with admin session, used by JcrResourceResolver */
    boolean itemReallyExists(Session clientSession, String path)
            throws RepositoryException {

        // assume this session has more access rights than the client Session
        String workSpace = clientSession.getWorkspace().getName();
        Session adminSession = getAdminSession(workSpace);
        
        // SLING-159: Workaround for method throwing when called with
        //            a malformed path
        try {
            return adminSession.itemExists(path);
        } catch (RepositoryException re) {
            log.info(
                "itemReallyExists: Error checking for existence of {}: {}",
                path, re.toString());
            return false;
        }
    }

    /** Returns the MIME type from the MimeTypeService for the given name */
    public String getMimeType(String name) {
        // local copy to not get NPE despite check for null due to concurrent
        // unbind
        MimeTypeService mts = mimeTypeService;
        return (mts != null) ? mts.getMimeType(name) : null;
    }

    /** If uri is a virtual URI returns the real URI, otherwise returns null */
    String virtualToRealUri(String virtualUri) {
        return (virtualURLMap != null)
                ? (String) virtualURLMap.get(virtualUri)
                : null;
    }

    /**
     * If uri is a real URI for any virtual URI, the virtual URI is returned,
     * otherwise returns null
     */
    String realToVirtualUri(String realUri) {
        return (virtualURLMap != null)
                ? (String) virtualURLMap.getKey(realUri)
                : null;
    }

    Mapping[] getMappings() {
        return mappings;
    }

    // ---------- Bundle provided resources -----------------------------------

    private void addBundleResourceProvider(Bundle bundle) {
        String prefixes = (String) bundle.getHeaders().get(
            BUNDLE_RESOURCE_ROOTS);
        if (prefixes != null) {
            BundleResourceProvider brp = new BundleResourceProvider(bundle,
                prefixes);
            rootProviderEntry.addResourceProvider(brp);
            bundleResourceProviderMap.put(bundle.getBundleId(), brp);
        }
    }

    private void removeBundleResourceProvider(Bundle bundle) {
        BundleResourceProvider brp = bundleResourceProviderMap.get(bundle.getBundleId());
        if (brp != null) {
            rootProviderEntry.removeResourceProvider(brp);
        }
    }

    ResourceProvider getResourceProvider(String path) {
        return rootProviderEntry.getResourceProvider(path);
    }

    // ---------- SCR Integration ---------------------------------------------

    /** Activates this component, called by SCR before registering as a service */
    protected void activate(ComponentContext componentContext) {
        this.serviceReference = componentContext.getServiceReference();

        this.initialContentLoader = new Loader(this);
        this.objectContentManagerFactory = new ObjectContentManagerFactory(this);

        componentContext.getBundleContext().addBundleListener(this);

        try {
            Session session = getAdminSession(null);

            Bundle[] bundles = componentContext.getBundleContext().getBundles();
            for (Bundle bundle : bundles) {
                if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
                    // load content for bundles which are neither INSTALLED nor
                    // UNINSTALLED
                    initialContentLoader.registerBundle(session, bundle);
                }

                if (bundle.getState() == Bundle.ACTIVE) {
                    // register active bundles with the mapper client
                    objectContentManagerFactory.registerMapperClient(bundle);
                    addBundleResourceProvider(bundle);
                }
            }
        } catch (Throwable t) {
            log.error("activate: Problem while loading initial content and"
                + " registering mappings for existing bundles", t);
        }

        Dictionary<?, ?> properties = componentContext.getProperties();

        BidiMap virtuals = new TreeBidiMap();
        String[] virtualList = (String[]) properties.get(PROP_VIRTUAL);
        for (int i = 0; virtualList != null && i < virtualList.length; i++) {
            String[] parts = Mapping.split(virtualList[i]);
            virtuals.put(parts[0], parts[2]);
        }
        virtualURLMap = virtuals;

        List<Mapping> maps = new ArrayList<Mapping>();
        String[] mappingList = (String[]) properties.get(PROP_MAPPING);
        for (int i = 0; mappingList != null && i < mappingList.length; i++) {
            maps.add(new Mapping(mappingList[i]));
        }
        Mapping[] tmp = maps.toArray(new Mapping[maps.size()]);

        // check whether direct mappings are allowed
        Boolean directProp = (Boolean) properties.get(PROP_ALLOW_DIRECT);
        allowDirect = (directProp != null) ? directProp.booleanValue() : true;
        if (allowDirect) {
            Mapping[] tmp2 = new Mapping[tmp.length + 1];
            tmp2[0] = Mapping.DIRECT;
            System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
            mappings = tmp2;
        } else {
            mappings = tmp;
        }

    }

    /** Deativates this component, called by SCR to take out of service */
    protected void deactivate(ComponentContext componentContext) {
        componentContext.getBundleContext().removeBundleListener(this);

        objectContentManagerFactory.dispose();
        initialContentLoader.dispose();

        Session[] sessions = adminSessions.values().toArray(
            new Session[adminSessions.size()]);
        adminSessions.clear();
        for (int i = 0; i < sessions.length; i++) {
            sessions[i].logout();
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
    private Session getAdminSession(String workspaceName)
            throws RepositoryException {

        // ensure workspace name
        if (workspaceName == null) {
            workspaceName = getRepository().getDefaultWorkspace();
        }

        synchronized (adminSessions) {
            Session adminSession = adminSessions.get(workspaceName);
            if (adminSession != null && adminSession.isLive()) {
                return adminSession;
            }

            adminSession = getRepository().loginAdministrative(workspaceName);
            adminSessions.put(workspaceName, adminSession);
            return adminSession;
        }
    }

}
