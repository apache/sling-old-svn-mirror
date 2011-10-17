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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.sling.jcr.resource.internal.helper.MapEntries;
import org.apache.sling.jcr.resource.internal.helper.Mapping;
import org.apache.sling.jcr.resource.internal.helper.ResourceProviderEntry;
import org.apache.sling.jcr.resource.internal.helper.RootResourceProviderEntry;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderEntry;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
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
 *                description="%resource.resolver.description" specVersion="1.1"
 * @scr.property name="service.description"
 *                value="Sling JcrResourceResolverFactory Implementation"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service interface="org.apache.sling.jcr.resource.JcrResourceResolverFactory"
 * @scr.service interface="org.apache.sling.api.resource.ResourceResolverFactory"
 * @scr.reference name="ResourceProvider"
 *                interface="org.apache.sling.api.resource.ResourceProvider"
 *                cardinality="0..n" policy="dynamic"
 * @scr.reference name="ResourceDecorator"
 *                interface="org.apache.sling.api.resource.ResourceDecorator"
 *                cardinality="0..n" policy="dynamic"
 *
 * First attempt of an resource resolver factory implementation.
 * WORK IN PROGRESS - see SLING-1262
 */
public class JcrResourceResolverFactoryImpl implements
        JcrResourceResolverFactory, ResourceResolverFactory {

    public final static class ResourcePattern {
        public final Pattern pattern;

        public final String replacement;

        public ResourcePattern(final Pattern p, final String r) {
            this.pattern = p;
            this.replacement = r;
        }
    }

    /**
     * Special value which, if passed to listener.workspaces, will have resource
     * events fired for all workspaces.
     */
    public static final String ALL_WORKSPACES = "*";

    /**
     * @scr.property values.1="/apps" values.2="/libs"
     */
    public static final String PROP_PATH = "resource.resolver.searchpath";

    /**
     * Defines whether namespace prefixes of resource names inside the path
     * (e.g. <code>jcr:</code> in <code>/home/path/jcr:content</code>) are
     * mangled or not.
     * <p>
     * Mangling means that any namespace prefix contained in the path is replaced
     * as per the generic substitution pattern <code>/([^:]+):/_$1_/</code>
     * when calling the <code>map</code> method of the resource resolver.
     * Likewise the <code>resolve</code> methods will unmangle such namespace
     * prefixes according to the substituation pattern
     * <code>/_([^_]+)_/$1:/</code>.
     * <p>
     * This feature is provided since there may be systems out there in the wild
     * which cannot cope with URLs containing colons, even though they are
     * perfectly valid characters in the path part of URI references with a
     * scheme.
     * <p>
     * The default value of this property if no configuration is provided is
     * <code>true</code>.
     *
     * @scr.property value="true" type="Boolean"
     */
    private static final String PROP_MANGLE_NAMESPACES = "resource.resolver.manglenamespaces";

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
     * @scr.property values.1="/:/"
     */
    private static final String PROP_VIRTUAL = "resource.resolver.virtual";

    /**
     * @scr.property values.1="/:/" values.2="/content/:/"
     *               values.3="/system/docroot/:/"
     */
    private static final String PROP_MAPPING = "resource.resolver.mapping";

    /**
     * @scr.property valueRef="MapEntries.DEFAULT_MAP_ROOT"
     */
    private static final String PROP_MAP_LOCATION = "resource.resolver.map.location";

    /**
     * @scr.property valueRef="DEFAULT_MULTIWORKSPACE"
     */
    private static final String PROP_MULTIWORKSPACE = "resource.resolver.multiworkspace";

    private static final boolean DEFAULT_MULTIWORKSPACE = false;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The JCR Repository we access to resolve resources
     *
     * @scr.reference
     */
    private SlingRepository repository;

    /** Tracker for the resource decorators. */
    private final ResourceDecoratorTracker resourceDecoratorTracker = new ResourceDecoratorTracker();

    // helper for the new JcrResourceResolver
    private MapEntries mapEntries = MapEntries.EMPTY;

    /** all mappings */
    private Mapping[] mappings;

    /** The fake urls */
    private BidiMap virtualURLMap;

    /** <code>true</code>, if direct mappings from URI to handle are allowed */
    private boolean allowDirect = false;

    // the search path for ResourceResolver.getResource(String)
    private String[] searchPath;

    // the root location of the /etc/map entries
    private String mapRoot;

    private final RootResourceProviderEntry rootProviderEntry;

    // whether to mangle paths with namespaces or not
    private boolean mangleNamespacePrefixes;

    private boolean useMultiWorkspaces;

    /** The resource listeners for the observation events. */
    private Set<JcrResourceListener> resourceListeners;

    /** The service tracker for the event admin
     */
    private ServiceTracker eventAdminTracker;

    /** The dynamic class loader
     * @scr.reference cardinality="0..1" policy="dynamic" */
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    public JcrResourceResolverFactoryImpl() {
        this.rootProviderEntry = new RootResourceProviderEntry();

    }

    public ResourceDecoratorTracker getResourceDecoratorTracker() {
        return this.resourceDecoratorTracker;
    }

    // ---------- JcrResourceResolverFactory -----------------------------------

    /**
     * Returns a new <code>ResourceResolve</code> for the given session. Note
     * that each call to this method returns a new resource manager instance.
     *
     * @see org.apache.sling.jcr.resource.JcrResourceResolverFactory#getResourceResolver(javax.jcr.Session)
     */
    public ResourceResolver getResourceResolver(Session session) {
        Map<String, Object> authInfo = new HashMap<String, Object>(1);
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session);
        try {
            return getResourceResolver(authInfo);
        } catch (LoginException le) {
            // we don't expect a LoginException here because just a
            // ResourceResolver wrapping the given session is to be created.
            throw new InternalError("Unexpected LoginException");
        }
    }

    // ---------- Resource Resolver Factory ------------------------------------

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getAdministrativeResourceResolver(java.util.Map)
     */
    public ResourceResolver getAdministrativeResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {
        return getResourceResolverInternal(authenticationInfo, true);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getResourceResolver(java.util.Map)
     */
    public ResourceResolver getResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {
        return getResourceResolverInternal(authenticationInfo, false);
    }

    /**
     * Create a new ResourceResolver wrapping a Session object. Carries map of
     * authentication info in order to create a new resolver as needed.
     */
    private ResourceResolver getResourceResolverInternal(
            final Map<String, Object> authenticationInfo, final boolean isAdmin)
            throws LoginException {

        // by default any session used by the resource resolver returned is
        // closed when the resource resolver is closed
        boolean logoutSession = true;

        // derive the session to be used
        Session session;
        try {
            final String workspace = getWorkspace(authenticationInfo);
            if (isAdmin) {
                // requested admin session to any workspace (or default)
                session = getRepository().loginAdministrative(workspace);

            } else {

                session = getSession(authenticationInfo);
                if (session == null) {
                    // requested non-admin session to any workspace (or default)
                    final Credentials credentials = getCredentials(authenticationInfo);
                    session = getRepository().login(credentials, workspace);

                } else if (workspace != null) {
                    // session provided by map; but requested a different
                    // workspace impersonate can only change the user not switch
                    // the workspace as a workaround we login to the requested
                    // workspace with admin and then switch to the provided
                    // session's user (if required)
                    Session tmpSession = null;
                    try {
                        tmpSession = getRepository().loginAdministrative(
                            workspace);
                        if (tmpSession.getUserID().equals(session.getUserID())) {
                            session = tmpSession;
                            tmpSession = null;
                        } else {
                            session = tmpSession.impersonate(new SimpleCredentials(
                                session.getUserID(), new char[0]));
                        }
                    } finally {
                        if (tmpSession != null) {
                            tmpSession.logout();
                        }
                    }

                } else {
                    // session provided; no special workspace; just make sure
                    // the session is not logged out when the resolver is closed
                    logoutSession = false;
                }
            }
        } catch (RepositoryException re) {
            throw getLoginException(re);
        }

        session = handleImpersonation(session, authenticationInfo, logoutSession);

        final JcrResourceProviderEntry sessionRoot = new JcrResourceProviderEntry(
            session, rootProviderEntry, this.getDynamicClassLoader(),
            useMultiWorkspaces);

        if (logoutSession) {
            return new JcrResourceResolver(sessionRoot, this, isAdmin,
                authenticationInfo, useMultiWorkspaces);
        }

        return new JcrResourceResolver(sessionRoot, this, isAdmin,
            authenticationInfo, useMultiWorkspaces) {
            protected void closeSession() {
            }
        };
    }

    // ---------- Implementation helpers --------------------------------------

    /** Get the dynamic class loader if available */
    ClassLoader getDynamicClassLoader() {
        final DynamicClassLoaderManager dclm = this.dynamicClassLoaderManager;
        if ( dclm != null ) {
            return dclm.getDynamicClassLoader();
        }
        return null;
    }

    /**
     * This method is called from {@link MapEntries}
     */
    public BidiMap getVirtualURLMap() {
        return virtualURLMap;
    }

    /**
     * This method is called from {@link MapEntries}
     */
    public Mapping[] getMappings() {
        return mappings;
    }

    String[] getSearchPath() {
        return searchPath;
    }

    boolean isMangleNamespacePrefixes() {
        return mangleNamespacePrefixes;

    }

    public String getMapRoot() {
        return mapRoot;
    }

    MapEntries getMapEntries() {
        return mapEntries;
    }

    String getDefaultWorkspaceName() {
        return this.repository.getDefaultWorkspace();
    }

    /**
     * Getter for rootProviderEntry, making it easier to extend
     * JcrResourceResolverFactoryImpl. See <a
     * href="https://issues.apache.org/jira/browse/SLING-730">SLING-730</a>
     *
     * @return Our rootProviderEntry
     */
    protected ResourceProviderEntry getRootProviderEntry() {
        return rootProviderEntry;
    }

    // ---------- SCR Integration ---------------------------------------------

    /** Activates this component, called by SCR before registering as a service */
    protected void activate(final ComponentContext componentContext) {
        // setup tracker first as this is used in the bind/unbind methods
        this.eventAdminTracker = new ServiceTracker(componentContext.getBundleContext(),
                EventAdmin.class.getName(), null);
        this.eventAdminTracker.open();

        final Dictionary<?, ?> properties = componentContext.getProperties();

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

        // from configuration if available
        searchPath = OsgiUtil.toStringArray(properties.get(PROP_PATH));
        if (searchPath != null && searchPath.length > 0) {
            for (int i = 0; i < searchPath.length; i++) {
                // ensure leading slash
                if (!searchPath[i].startsWith("/")) {
                    searchPath[i] = "/" + searchPath[i];
                }
                // ensure trailing slash
                if (!searchPath[i].endsWith("/")) {
                    searchPath[i] += "/";
                }
            }
        }
        if (searchPath == null) {
            searchPath = new String[] { "/" };
        }

        // namespace mangling
        mangleNamespacePrefixes = OsgiUtil.toBoolean(
            properties.get(PROP_MANGLE_NAMESPACES), false);

        // the root of the resolver mappings
        mapRoot = OsgiUtil.toString(properties.get(PROP_MAP_LOCATION),
            MapEntries.DEFAULT_MAP_ROOT);

        // set up the map entries from configuration
        try {
            mapEntries = new MapEntries(this, componentContext.getBundleContext(), this.eventAdminTracker);
        } catch (Exception e) {
            log.error(
                "activate: Cannot access repository, failed setting up Mapping Support",
                e);
        }


        // start observation listener
        try {
            this.resourceListeners = new HashSet<JcrResourceListener>();

            // first - add a listener for the default workspace
            this.resourceListeners.add(new JcrResourceListener(null, this, "/", "/", this.eventAdminTracker));

            // check if multi workspace support is enabled
            this.useMultiWorkspaces = OsgiUtil.toBoolean(properties.get(PROP_MULTIWORKSPACE), DEFAULT_MULTIWORKSPACE);
            if (this.useMultiWorkspaces) {
                final String[] listenerWorkspaces = getAllWorkspaces();
                for (final String wspName : listenerWorkspaces) {
                    if (!wspName.equals(this.repository.getDefaultWorkspace())) {
                        this.resourceListeners.add(
                            new JcrResourceListener(wspName, this, "/", "/", this.eventAdminTracker));
                    }
                }
            }
        } catch (Exception e) {
            log.error(
                "activate: Cannot create resource listener; resource events for JCR resources will be disabled.",
                e);
        }

        try {
            plugin = new JcrResourceResolverWebConsolePlugin(componentContext.getBundleContext(), this);
        } catch (Throwable ignore) {
            // an exception here propably means the web console plugin is not available
            log.debug(
                    "activate: unable to setup web console plugin.", ignore);
        }
    }

    private JcrResourceResolverWebConsolePlugin plugin;

    /** Deativates this component, called by SCR to take out of service */
    protected void deactivate(final ComponentContext componentContext) {
        if (plugin != null) {
            plugin.dispose();
            plugin = null;
        }

        if (mapEntries != null) {
            mapEntries.dispose();
            mapEntries = MapEntries.EMPTY;
        }
        if ( this.eventAdminTracker != null ) {
            this.eventAdminTracker.close();
            this.eventAdminTracker = null;
        }
        if ( this.resourceListeners != null && !this.resourceListeners.isEmpty() ) {
            for ( JcrResourceListener resourceListener : this.resourceListeners ) {
                resourceListener.dispose();
            }
            this.resourceListeners = null;
        }
        this.resourceDecoratorTracker.close();
    }

    protected void bindResourceProvider(final ResourceProvider provider, final Map<String, Object> props) {
        this.rootProviderEntry.bindResourceProvider(provider, props, this.eventAdminTracker);
    }

    protected void unbindResourceProvider(final ResourceProvider provider, final Map<String, Object> props) {
        this.rootProviderEntry.unbindResourceProvider(provider, props, this.eventAdminTracker);
    }

    protected void bindResourceDecorator(final ResourceDecorator decorator, final Map<String, Object> props) {
        this.resourceDecoratorTracker.bindResourceDecorator(decorator, props);
    }

    protected void unbindResourceDecorator(final ResourceDecorator decorator, final Map<String, Object> props) {
        this.resourceDecoratorTracker.unbindResourceDecorator(decorator, props);
    }

    // ---------- internal helper ----------------------------------------------

    /** Returns the JCR repository used by this factory */
    protected SlingRepository getRepository() {
        return repository;
    }

    /**
     * Create a login exception from a repository exception.
     * If the repository exception is a  {@link javax.jcr.LoginException}
     * a {@link LoginException} is created with the same information.
     * Otherwise a {@link LoginException} is created which wraps the
     * repository exception.
     * @param re The repository exception.
     * @return The login exception.
     */
    private LoginException getLoginException(final RepositoryException re) {
        if ( re instanceof javax.jcr.LoginException ) {
            return new LoginException(re.getMessage(), re.getCause());
        }
        return new LoginException("Unable to login " + re.getMessage(), re);
    }

    /**
     * Get an array of all workspaces.
     */
    private String[] getAllWorkspaces() throws RepositoryException {
        Session session =  null;
        try {
            session = repository.loginAdministrative(null);
            return session.getWorkspace().getAccessibleWorkspaceNames();
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Returns the session provided as the user.jcr.session property of the
     * <code>authenticationInfo</code> map or <code>null</code> if the
     * property is not contained in the map or is not a <code>javax.jcr.Session</code>.
     * @param authenticationInfo Optional authentication info.
     * @return The user.jcr.session property or <code>null</code>
     */
    private Session getSession(final Map<String, Object> authenticationInfo) {
        if (authenticationInfo != null) {
            final Object sessionObject = authenticationInfo.get(JcrResourceConstants.AUTHENTICATION_INFO_SESSION);
            if (sessionObject instanceof Session) {
                return (Session) sessionObject;
            }
        }
        return null;
    }

    /**
     * Return the workspace name.
     * If the workspace name is provided, it is returned, otherwise
     * <code>null</code> is returned.
     * @param authenticationInfo Optional authentication info.
     * @return The configured workspace name or <code>null</code>
     */
    private String getWorkspace(final Map<String, Object> authenticationInfo) {
        if (authenticationInfo != null) {
            final Object workspaceObject = authenticationInfo.get(JcrResourceConstants.AUTHENTICATION_INFO_WORKSPACE);
            if (workspaceObject instanceof String) {
                return (String) workspaceObject;
            }
        }
        return null;
    }

    /**
     * Return the sudo user information.
     * If the sudo user info is provided, it is returned, otherwise
     * <code>null</code> is returned.
     * @param authenticationInfo Optional authentication info.
     * @return The configured sudo user information or <code>null</code>
     */
    private String getSudoUser(final Map<String, Object> authenticationInfo) {
        if (authenticationInfo != null) {
            final Object sudoObject = authenticationInfo.get(ResourceResolverFactory.USER_IMPERSONATION);
            if (sudoObject instanceof String) {
                return (String) sudoObject;
            }
        }
        return null;
    }

    /**
     * Handle the sudo if configured. If the authentication info does not
     * contain a sudo info, this method simply returns the passed in session. If
     * a sudo user info is available, the session is tried to be impersonated.
     * The new impersonated session is returned. The original session is closed.
     * The session is also closed if the impersonation fails.
     *
     * @param session The session.
     * @param authenticationInfo The optional authentication info.
     * @param logoutSession whether to logout the <code>session</code> after
     *            impersonation or not.
     * @return The original session or impersonated session.
     * @throws LoginException If something goes wrong.
     */
    private Session handleImpersonation(final Session session,
            final Map<String, Object> authenticationInfo, boolean logoutSession)
            throws LoginException {
        final String sudoUser = getSudoUser(authenticationInfo);
        if (sudoUser != null && !session.getUserID().equals(sudoUser)) {
            try {
                final SimpleCredentials creds = new SimpleCredentials(sudoUser,
                    new char[0]);
                copyAttributes(creds, authenticationInfo);
                creds.setAttribute(ResourceResolver.USER_IMPERSONATOR,
                    session.getUserID());
                return session.impersonate(creds);
            } catch (RepositoryException re) {
                throw getLoginException(re);
            } finally {
                if (logoutSession) {
                    session.logout();
                }
            }
        }
        return session;
    }

    /**
     * Create a credentials object from the provided authentication info.
     * If no map is provided, <code>null</code> is returned.
     * If a map is provided and contains a credentials object, this object is
     * returned.
     * If a map is provided but does not contain a credentials object nor a
     * user, <code>null</code> is returned.
     * if a map is provided with a user name but without a credentials object
     * a new credentials object is created and all values from the authentication
     * info are added as attributes.
     * @param authenticationInfo Optional authentication info
     * @return A credentials object or <code>null</code>
     */
    private Credentials getCredentials(final Map<String, Object> authenticationInfo) {
        if (authenticationInfo == null) {
            return null;
        }

        final Object credentialsObject = authenticationInfo.get(JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS);
        if (credentialsObject instanceof Credentials) {
            return (Credentials) credentialsObject;
        }

        // otherwise try to create SimpleCredentials if the userId is set
        final Object userId = authenticationInfo.get(USER);
        if (userId instanceof String) {
            final Object password = authenticationInfo.get(PASSWORD);
            final SimpleCredentials credentials = new SimpleCredentials(
                (String) userId, ((password instanceof char[])
                        ? (char[]) password
                        : new char[0]));

            // add attributes
            copyAttributes(credentials, authenticationInfo);

            return credentials;
        }

        // no user id (or not a String)
        return null;
    }

    /**
     * Copies the contents of the source map as attributes into the target
     * <code>SimpleCredentials</code> object with the exception of the
     * <code>user.jcr.credentials</code> and <code>user.password</code>
     * attributes to prevent leaking passwords into the JCR Session attributes
     * which might be used for break-in attempts.
     *
     * @param target The <code>SimpleCredentials</code> object whose attributes
     *            are to be augmented.
     * @param source The map whose entries (except the ones listed above) are
     *            copied as credentials attributes.
     */
    private void copyAttributes(final SimpleCredentials target,
            final Map<String, Object> source) {
        final Iterator<Map.Entry<String, Object>> i = source.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry<String, Object> current = i.next();
            if (isAttributeVisible(current.getKey())) {
                target.setAttribute(current.getKey(), current.getValue());
            }
        }
    }

    /**
     * Returns <code>true</code> unless the name is
     * <code>user.jcr.credentials</code> (
     * {@link JcrResourceConstants#AUTHENTICATION_INFO_CREDENTIALS}) or contains
     * the string <code>password</code> as in <code>user.password</code> (
     * {@link org.apache.sling.api.resource.ResourceResolverFactory#PASSWORD})
     *
     * @param name The name to check whether it is visible or not
     * @return <code>true</code> if the name is assumed visible
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     */
    static boolean isAttributeVisible(final String name) {
        return !name.equals(JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS)
            && !name.contains("password");
    }

}