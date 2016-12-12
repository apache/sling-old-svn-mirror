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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.io.Closeable;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.jcr.resource.internal.JcrListenerBaseConfig;
import org.apache.sling.jcr.resource.internal.JcrModifiableValueMap;
import org.apache.sling.jcr.resource.internal.JcrResourceListener;
import org.apache.sling.jcr.resource.internal.NodeUtil;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;
import org.apache.sling.spi.resource.provider.ProviderContext;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name="org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderFactory",
           service = ResourceProvider.class,
           property = {
                   ResourceProvider.PROPERTY_NAME + "=JCR",
                   ResourceProvider.PROPERTY_ROOT + "=/",
                   ResourceProvider.PROPERTY_MODIFIABLE + ":Boolean=true",
                   ResourceProvider.PROPERTY_ADAPTABLE + ":Boolean=true",
                   ResourceProvider.PROPERTY_ATTRIBUTABLE + ":Boolean=true",
                   ResourceProvider.PROPERTY_REFRESHABLE + ":Boolean=true",
                   ResourceProvider.PROPERTY_AUTHENTICATE + "=" + ResourceProvider.AUTHENTICATE_REQUIRED,
                   Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
           })
public class JcrResourceProvider extends ResourceProvider<JcrProviderState> {

    /** Logger */
    private final Logger logger = LoggerFactory.getLogger(JcrResourceProvider.class);

    private static final String REPOSITORY_REFERNENCE_NAME = "repository";

    private static final Set<String> IGNORED_PROPERTIES = new HashSet<String>();
    static {
        IGNORED_PROPERTIES.add(NodeUtil.MIXIN_TYPES);
        IGNORED_PROPERTIES.add(NodeUtil.NODE_TYPE);
        IGNORED_PROPERTIES.add("jcr:created");
        IGNORED_PROPERTIES.add("jcr:createdBy");
    }

    @Reference(name = REPOSITORY_REFERNENCE_NAME, service = SlingRepository.class)
    private ServiceReference<SlingRepository> repositoryReference;

    @Reference
    private PathMapper pathMapper;

    /** This service is only available on OAK, therefore optional and static) */
    @Reference(policy=ReferencePolicy.STATIC, cardinality=ReferenceCardinality.OPTIONAL)
    private Executor executor;

    /** The JCR listener base configuration. */
    private volatile JcrListenerBaseConfig listenerConfig;

    /** The JCR observation listeners. */
    private final Map<ObserverConfiguration, Closeable> listeners = new HashMap<>();

    private volatile SlingRepository repository;

    private volatile JcrProviderStateFactory stateFactory;

    private final AtomicReference<DynamicClassLoaderManager> classLoaderManagerReference = new AtomicReference<DynamicClassLoaderManager>();

    @Activate
    protected void activate(final ComponentContext context) throws RepositoryException {
        SlingRepository repository = context.locateService(REPOSITORY_REFERNENCE_NAME,
                this.repositoryReference);
        if (repository == null) {
            // concurrent unregistration of SlingRepository service
            // don't care, this component is going to be deactivated
            // so we just stop working
            logger.warn("activate: Activation failed because SlingRepository may have been unregistered concurrently");
            return;
        }

        this.repository = repository;

        this.stateFactory = new JcrProviderStateFactory(repositoryReference, repository,
                classLoaderManagerReference, pathMapper);
    }

    @Deactivate
    protected void deactivate() {
        this.stateFactory = null;
    }

    @Reference(name = "dynamicClassLoaderManager",
            service = DynamicClassLoaderManager.class,
            cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void bindDynamicClassLoaderManager(final DynamicClassLoaderManager dynamicClassLoaderManager) {
        this.classLoaderManagerReference.set(dynamicClassLoaderManager);
    }

    protected void unbindDynamicClassLoaderManager(final DynamicClassLoaderManager dynamicClassLoaderManager) {
        this.classLoaderManagerReference.compareAndSet(dynamicClassLoaderManager, null);
    }

    @Override
    public void start(final ProviderContext ctx) {
        super.start(ctx);
        this.registerListeners();
    }

    @Override
    public void stop() {
        this.unregisterListeners();
        super.stop();
    }

    @Override
    public void update(final long changeSet) {
        super.update(changeSet);
        this.updateListeners();
    }

    @SuppressWarnings("unused")
    private void bindRepository(final ServiceReference<SlingRepository> ref) {
        this.repositoryReference = ref;
        this.repository = null; // make sure ...
    }

    @SuppressWarnings("unused")
    private void unbindRepository(final ServiceReference<SlingRepository> ref) {
        if (this.repositoryReference == ref) {
            this.repositoryReference = null;
            this.repository = null; // make sure ...
        }
    }

    /**
     * Register all observation listeners.
     */
    private void registerListeners() {
        if ( this.repository != null ) {
            logger.debug("Registering resource listeners...");
            try {
                this.listenerConfig = new JcrListenerBaseConfig(this.getProviderContext().getObservationReporter(),
                    this.pathMapper,
                    this.repository);
                for(final ObserverConfiguration config : this.getProviderContext().getObservationReporter().getObserverConfigurations()) {
                    logger.debug("Registering listener for {}", config.getPaths());
                    final Closeable listener = new JcrResourceListener(this.listenerConfig,
                            config);
                    this.listeners.put(config, listener);
                }
            } catch (final RepositoryException e) {
                throw new SlingException("Can't create the JCR event listener.", e);
            }
            logger.debug("Registered resource listeners");
        }
    }

    /**
     * Unregister all observation listeners.
     */
    private void unregisterListeners() {
        logger.debug("Unregistering resource listeners...");
        for(final Closeable c : this.listeners.values()) {
            try {
                logger.debug("Removing listener for {}", ((JcrResourceListener)c).getConfig().getPaths());
                c.close();
            } catch (final IOException e) {
                // ignore this as the method above does not throw it
            }
        }
        this.listeners.clear();
        if ( this.listenerConfig != null ) {
            try {
                this.listenerConfig.close();
            } catch (final IOException e) {
                // ignore this as the method above does not throw it
            }
            this.listenerConfig = null;
        }
        logger.debug("Unregistered resource listeners");
    }

    /**
     * Update observation listeners.
     */
    private void updateListeners() {
        if ( this.listenerConfig == null ) {
            this.unregisterListeners();
            this.registerListeners();
        } else {
            logger.debug("Updating resource listeners...");
            final Map<ObserverConfiguration, Closeable> oldMap = new HashMap<>(this.listeners);
            this.listeners.clear();
            try {
                for(final ObserverConfiguration config : this.getProviderContext().getObservationReporter().getObserverConfigurations()) {
                    // check if such a listener already exists
                    Closeable listener = oldMap.remove(config);
                    if ( listener == null ) {
                        logger.debug("Registering listener for {}", config.getPaths());
                        listener = new JcrResourceListener(this.listenerConfig, config);
                    } else {
                        logger.debug("Updating listener for {}", config.getPaths());
                        ((JcrResourceListener)listener).update(config);
                    }
                    this.listeners.put(config, listener);
                }
            } catch (final RepositoryException e) {
                throw new SlingException("Can't create the JCR event listener.", e);
            }
            for(final Closeable c : oldMap.values()) {
                try {
                    logger.debug("Removing listener for {}", ((JcrResourceListener)c).getConfig().getPaths());
                    c.close();
                } catch (final IOException e) {
                    // ignore this as the method above does not throw it
                }
            }
            logger.debug("Updated resource listeners");
        }
    }

    /**
     * Create a new ResourceResolver wrapping a Session object. Carries map of
     * authentication info in order to create a new resolver as needed.
     */
    @Override
    @Nonnull public JcrProviderState authenticate(final @Nonnull Map<String, Object> authenticationInfo)
    throws LoginException {
        return stateFactory.createProviderState(authenticationInfo);
    }

    @Override
    public void logout(final @Nonnull JcrProviderState state) {
        state.logout();
    }

    @Override
    public boolean isLive(final @Nonnull ResolveContext<JcrProviderState> ctx) {
        return ctx.getProviderState().getSession().isLive();
    }

    @Override
    public Resource getResource(ResolveContext<JcrProviderState> ctx, String path, ResourceContext rCtx, Resource parent) {
        try {
            return ctx.getProviderState().getResourceFactory().createResource(ctx.getResourceResolver(), path, parent, rCtx.getResolveParameters());
        } catch (RepositoryException e) {
            throw new SlingException("Can't get resource", e);
        }
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<JcrProviderState> ctx, Resource parent) {
        JcrItemResource<?> parentItemResource;

        // short cut for known JCR resources
        if (parent instanceof JcrItemResource) {
            parentItemResource = (JcrItemResource<?>) parent;
        } else {
            // try to get the JcrItemResource for the parent path to list
            // children
            try {
                parentItemResource = ctx.getProviderState().getResourceFactory().createResource(
                        parent.getResourceResolver(), parent.getPath(), null,
                        parent.getResourceMetadata().getParameterMap());
            } catch (RepositoryException re) {
                throw new SlingException("Can't list children", re);
            }
        }

        // return children if there is a parent item resource, else null
        return (parentItemResource != null)
                ? parentItemResource.listJcrChildren()
                : null;
    }

    @Override
    public @CheckForNull Resource getParent(final @Nonnull ResolveContext<JcrProviderState> ctx, final @Nonnull Resource child) {
        if (child instanceof JcrItemResource<?>) {
            try {
                String version = null;
                if (child.getResourceMetadata().getParameterMap() != null) {
                    version = child.getResourceMetadata().getParameterMap().get("v");
                }
                if (version == null) {
                    Item item = ((JcrItemResource<?>) child).getItem();
                    if ("/".equals(item.getPath())) {
                        return null;
                    }
                    Node parentNode;
                    try {
                        parentNode = item.getParent();
                    } catch(AccessDeniedException e) {
                        return null;
                    }
                    String parentPath = ResourceUtil.getParent(child.getPath());
                    return new JcrNodeResource(ctx.getResourceResolver(), parentPath, version, parentNode,
                            ctx.getProviderState().getHelperData());
                }
            } catch (RepositoryException e) {
                logger.warn("Can't get parent for {}", child, e);
                return null;
            }
        }
        return super.getParent(ctx, child);
    }

    @Override
    public Collection<String> getAttributeNames(final @Nonnull ResolveContext<JcrProviderState> ctx) {
        final Set<String> names = new HashSet<String>();
        final String[] sessionNames = ctx.getProviderState().getSession().getAttributeNames();
        for(final String name : sessionNames) {
            if ( isAttributeVisible(name) ) {
                names.add(name);
            }
        }
        return names;
    }

    @Override
    public Object getAttribute(final @Nonnull ResolveContext<JcrProviderState> ctx, final @Nonnull String name) {
        if (isAttributeVisible(name)) {
            if (ResourceResolverFactory.USER.equals(name)) {
                return ctx.getProviderState().getSession().getUserID();
            }
            return ctx.getProviderState().getSession().getAttribute(name);
        }
        return null;
    }

    @Override
    public Resource create(final @Nonnull ResolveContext<JcrProviderState> ctx, final String path, final Map<String, Object> properties)
    throws PersistenceException {
        // check for node type
        final Object nodeObj = (properties != null ? properties.get(NodeUtil.NODE_TYPE) : null);
        // check for sling:resourcetype
        final String nodeType;
        if ( nodeObj != null ) {
            nodeType = nodeObj.toString();
        } else {
            final Object rtObj =  (properties != null ? properties.get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY) : null);
            boolean isNodeType = false;
            if ( rtObj != null ) {
                final String resourceType = rtObj.toString();
                if ( resourceType.indexOf(':') != -1 && resourceType.indexOf('/') == -1 ) {
                    try {
                        ctx.getProviderState().getSession().getWorkspace().getNodeTypeManager().getNodeType(resourceType);
                        isNodeType = true;
                    } catch (final RepositoryException ignore) {
                        // we expect this, if this isn't a valid node type, therefore ignoring
                    }
                }
            }
            if ( isNodeType ) {
                nodeType = rtObj.toString();
            } else {
                nodeType = null;
            }
        }
        final String jcrPath = pathMapper.mapResourcePathToJCRPath(path);
        if ( jcrPath == null ) {
            throw new PersistenceException("Unable to create node at " + path, null, path, null);
        }
        Node node = null;
        try {
            final int lastPos = jcrPath.lastIndexOf('/');
            final Node parent;
            if ( lastPos == 0 ) {
                parent = ctx.getProviderState().getSession().getRootNode();
            } else {
                parent = (Node) ctx.getProviderState().getSession().getItem(jcrPath.substring(0, lastPos));
            }
            final String name = jcrPath.substring(lastPos + 1);
            if ( nodeType != null ) {
                node = parent.addNode(name, nodeType);
            } else {
                node = parent.addNode(name);
            }

            if ( properties != null ) {
                // create modifiable map
                final JcrModifiableValueMap jcrMap = new JcrModifiableValueMap(node, ctx.getProviderState().getHelperData());
                // check mixin types first
                final Object value = properties.get(NodeUtil.MIXIN_TYPES);
                if ( value != null ) {
                    jcrMap.put(NodeUtil.MIXIN_TYPES, value);
                }
                for(final Map.Entry<String, Object> entry : properties.entrySet()) {
                    if ( !IGNORED_PROPERTIES.contains(entry.getKey()) ) {
                        try {
                            jcrMap.put(entry.getKey(), entry.getValue());
                        } catch (final IllegalArgumentException iae) {
                            try {
                                node.remove();
                            } catch ( final RepositoryException re) {
                                // we ignore this
                            }
                            throw new PersistenceException(iae.getMessage(), iae, path, entry.getKey());
                        }
                    }
                }
            }

            return new JcrNodeResource(ctx.getResourceResolver(), path, null, node, ctx.getProviderState().getHelperData());
        } catch (final RepositoryException e) {
            throw new PersistenceException("Unable to create node at " + jcrPath, e, path, null);
        }
    }

    @Override
    public void delete(final @Nonnull ResolveContext<JcrProviderState> ctx, final @Nonnull Resource resource)
    throws PersistenceException {
        // try to adapt to Item
        Item item = resource.adaptTo(Item.class);
        try {
            if ( item == null ) {
                final String jcrPath = pathMapper.mapResourcePathToJCRPath(resource.getPath());
                if (jcrPath == null) {
                    logger.debug("delete: {} maps to an empty JCR path", resource.getPath());
                    throw new PersistenceException("Unable to delete resource", null, resource.getPath(), null);
                }
                item = ctx.getProviderState().getSession().getItem(jcrPath);
            }
            item.remove();
        } catch (final RepositoryException e) {
            throw new PersistenceException("Unable to delete resource", e, resource.getPath(), null);
        }
    }

    @Override
    public void revert(final @Nonnull ResolveContext<JcrProviderState> ctx) {
        try {
            ctx.getProviderState().getSession().refresh(false);
        } catch (final RepositoryException ignore) {
            logger.warn("Unable to revert pending changes.", ignore);
        }
    }

    @Override
    public void commit(final @Nonnull ResolveContext<JcrProviderState> ctx)
    throws PersistenceException {
        try {
            ctx.getProviderState().getSession().save();
        } catch (final RepositoryException e) {
            throw new PersistenceException("Unable to commit changes to session.", e);
        }
    }

    @Override
    public boolean hasChanges(final @Nonnull ResolveContext<JcrProviderState> ctx) {
        try {
            return ctx.getProviderState().getSession().hasPendingChanges();
        } catch (final RepositoryException ignore) {
            logger.warn("Unable to check session for pending changes.", ignore);
        }
        return false;
    }

    @Override
    public void refresh(final @Nonnull ResolveContext<JcrProviderState> ctx) {
        try {
            ctx.getProviderState().getSession().refresh(true);
        } catch (final RepositoryException ignore) {
            logger.warn("Unable to refresh session.", ignore);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public @CheckForNull <AdapterType> AdapterType adaptTo(final @Nonnull ResolveContext<JcrProviderState> ctx,
            final @Nonnull Class<AdapterType> type) {
        Session session = ctx.getProviderState().getSession();
        if (type == Session.class) {
            return (AdapterType) session;
        } else if (type == Principal.class) {
            try {
                if (session instanceof JackrabbitSession && session.getUserID() != null) {
                    JackrabbitSession s =((JackrabbitSession) session);
                    final UserManager um = s.getUserManager();
                    if (um != null) {
                        final Authorizable auth = um.getAuthorizable(s.getUserID());
                        if (auth != null) {
                            return (AdapterType) auth.getPrincipal();
                        }
                    }
                }
                logger.debug("not able to adapto Resource to Principal, let the base class try to adapt");
            } catch (RepositoryException e) {
                logger.warn("error while adapting Resource to Principal, let the base class try to adapt", e);
            }
        }
        return super.adaptTo(ctx, type);
    }

    @Override
    public boolean copy(final  @Nonnull ResolveContext<JcrProviderState> ctx,
            final String srcAbsPath,
            final String destAbsPath) throws PersistenceException {
        return false;
    }

    @Override
    public boolean move(final  @Nonnull ResolveContext<JcrProviderState> ctx,
            final String srcAbsPath,
            final String destAbsPath) throws PersistenceException {
        final String srcNodePath = pathMapper.mapResourcePathToJCRPath(srcAbsPath);
        final String dstNodePath = pathMapper.mapResourcePathToJCRPath(destAbsPath + '/' + ResourceUtil.getName(srcAbsPath));
        try {
            ctx.getProviderState().getSession().move(srcNodePath, dstNodePath);
            return true;
        } catch (final RepositoryException e) {
            throw new PersistenceException("Unable to move resource to " + destAbsPath, e, srcAbsPath, null);
        }
    }

    @Override
    public @CheckForNull QueryLanguageProvider<JcrProviderState> getQueryLanguageProvider() {
        final ProviderContext ctx = this.getProviderContext();
        if ( ctx != null ) {
            return new BasicQueryLanguageProvider(ctx);
        }
        return null;
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
    private static boolean isAttributeVisible(final String name) {
        return !name.equals(JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS)
            && !name.contains("password");
    }
}
