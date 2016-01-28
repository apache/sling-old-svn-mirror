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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
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
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.internal.HelperData;
import org.apache.sling.jcr.resource.internal.JcrModifiableValueMap;
import org.apache.sling.jcr.resource.internal.JcrResourceListener;
import org.apache.sling.jcr.resource.internal.NodeUtil;
import org.apache.sling.jcr.resource.internal.OakResourceListener;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ProviderContext;
import org.apache.sling.spi.resource.provider.ResolverContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true,
        label = "Apache Sling JCR Resource Provider Factory",
        description = "This provider adds  JCR resources to the resource tree")
@Service(value = ResourceProvider.class)
@Properties({ @Property(name = ResourceProvider.PROPERTY_NAME, value = "JCR"),
        @Property(name = ResourceProvider.PROPERTY_ROOT, value = "/"),
        @Property(name = ResourceProvider.PROPERTY_MODIFIABLE, boolValue = true),
        @Property(name = ResourceProvider.PROPERTY_ADAPTABLE, boolValue = true),
        @Property(name = ResourceProvider.PROPERTY_AUTHENTICATE, value = ResourceProvider.AUTHENTICATE_REQUIRED),
        @Property(name = ResourceProvider.PROPERTY_ATTRIBUTABLE, boolValue = true),
        @Property(name = ResourceProvider.PROPERTY_REFRESHABLE, boolValue = true),
})
public class JcrResourceProvider extends ResourceProvider<JcrProviderState> {

    /** Logger */
    private static final Logger log = LoggerFactory.getLogger(JcrResourceProvider.class);

    private static final String REPOSITORY_REFERNENCE_NAME = "repository";

    private static final Set<String> IGNORED_PROPERTIES = new HashSet<String>();
    static {
        IGNORED_PROPERTIES.add(NodeUtil.MIXIN_TYPES);
        IGNORED_PROPERTIES.add(NodeUtil.NODE_TYPE);
        IGNORED_PROPERTIES.add("jcr:created");
        IGNORED_PROPERTIES.add("jcr:createdBy");
    }

    private static final boolean DEFAULT_OPTIMIZE_FOR_OAK = true;
    @Property(boolValue=DEFAULT_OPTIMIZE_FOR_OAK,
              label="Optimize For Oak",
              description="If this switch is enabled, and Oak is used as the repository implementation, some optimized components are used.")
    private static final String PROPERTY_OPTIMIZE_FOR_OAK = "optimize.oak";

    private static final int DEFAULT_OBSERVATION_QUEUE_LENGTH = 1000;
    @Property(
            intValue = DEFAULT_OBSERVATION_QUEUE_LENGTH,
            label = "Observation queue length",
            description = "Maximum number of pending revisions in a observation listener queue")
    private static final String OBSERVATION_QUEUE_LENGTH = "oak.observation.queue-length";

    @Reference(name = REPOSITORY_REFERNENCE_NAME, referenceInterface = SlingRepository.class)
    private ServiceReference repositoryReference;

    @Reference
    private PathMapper pathMapper;

    /** The dynamic class loader */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private volatile DynamicClassLoaderManager dynamicClassLoaderManager;

    /** This service is only available on OAK, therefore optional and static) */
    @Reference(policy=ReferencePolicy.STATIC, cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    private Executor executor;

    /** The JCR observation listener. */
    private Closeable listener;

    private SlingRepository repository;

    private int observationQueueLength;

    private boolean optimizeForOak;

    private String root;

    private BundleContext bundleCtx;

    private JcrProviderStateFactory stateFactory;

    @Activate
    protected void activate(final ComponentContext context) throws RepositoryException {
        SlingRepository repository = (SlingRepository) context.locateService(REPOSITORY_REFERNENCE_NAME,
                this.repositoryReference);
        if (repository == null) {
            // concurrent unregistration of SlingRepository service
            // don't care, this component is going to be deactivated
            // so we just stop working
            log.warn("activate: Activation failed because SlingRepository may have been unregistered concurrently");
            return;
        }

        this.repository = repository;
        this.observationQueueLength = PropertiesUtil.toInteger(context.getProperties().get(OBSERVATION_QUEUE_LENGTH), DEFAULT_OBSERVATION_QUEUE_LENGTH);
        this.optimizeForOak = PropertiesUtil.toBoolean(context.getProperties().get(PROPERTY_OPTIMIZE_FOR_OAK), DEFAULT_OPTIMIZE_FOR_OAK);
        this.root = PropertiesUtil.toString(context.getProperties().get(ResourceProvider.PROPERTY_ROOT), "/");
        this.bundleCtx = context.getBundleContext();

        HelperData helperData = new HelperData(dynamicClassLoaderManager.getDynamicClassLoader(), pathMapper);
        this.stateFactory = new JcrProviderStateFactory(repositoryReference, repository, helperData);
    }

    @Deactivate
    protected void deactivate() {
    }

    @Override
    public void start(final ProviderContext ctx) {
        super.start(ctx);
        registerListener(ctx);
    }

    @Override
    public void stop() {
        unregisterListener();
        super.stop();
    }

    @Override
    public void update(long changeSet) {
        super.update(changeSet);
        unregisterListener();
        registerListener(getProviderContext());
    }

    @SuppressWarnings("unused")
    private void bindRepository(final ServiceReference ref) {
        this.repositoryReference = ref;
        this.repository = null; // make sure ...
    }

    @SuppressWarnings("unused")
    private void unbindRepository(final ServiceReference ref) {
        if (this.repositoryReference == ref) {
            this.repositoryReference = null;
            this.repository = null; // make sure ...
        }
    }

    private void registerListener(ProviderContext ctx) {
        // check for Oak
        boolean isOak = false;
        if ( optimizeForOak ) {
            final String repoDesc = this.repository.getDescriptor(Repository.REP_NAME_DESC);
            if ( repoDesc != null && repoDesc.toLowerCase().contains(" oak") ) {
                if ( this.executor != null ) {
                    isOak = true;
                } else {
                   log.error("Detected Oak based repository but no executor service available! Unable to use improved JCR Resource listener");
                }
            }
        }
        try {
            if (isOak) {
                try {
                    this.listener = new OakResourceListener(root, ctx, bundleCtx, executor, pathMapper, observationQueueLength, repository);
                    log.info("Detected Oak based repository. Using improved JCR Resource Listener with observation queue length {}", observationQueueLength);
                } catch ( final RepositoryException re ) {
                    throw new SlingException("Can't create the OakResourceListener", re);
                } catch ( final Throwable t ) {
                    log.error("Unable to instantiate improved JCR Resource listener for Oak. Using fallback.", t);
                }
            }
            if (this.listener == null) {
                this.listener = new JcrResourceListener(ctx, root, pathMapper, repository);
            }
        } catch (RepositoryException e) {
            throw new SlingException("Can't create the listener", e);
        }
    }

    private void unregisterListener() {
        if ( this.listener != null ) {
            try {
                this.listener.close();
            } catch (final IOException e) {
                // ignore this as the method above does not throw it
            }
            this.listener = null;
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
    public void logout(final @Nonnull ResolverContext<JcrProviderState> ctx) {
        ctx.getProviderState().logout();
    }

    @Override
    public boolean isLive(final @Nonnull ResolverContext<JcrProviderState> ctx) {
        return ctx.getProviderState().getSession().isLive();
    }

    @Override
    public Resource getResource(ResolverContext<JcrProviderState> ctx, String path, ResourceContext rCtx, Resource parent) {
        try {
            return ctx.getProviderState().getResourceFactory().createResource(ctx.getResourceResolver(), path, parent, rCtx.getResolveParameters());
        } catch (RepositoryException e) {
            throw new SlingException("Can't get resource", e);
        }
    }

    @Override
    public Iterator<Resource> listChildren(ResolverContext<JcrProviderState> ctx, Resource parent) {
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
    public @CheckForNull Resource getParent(final @Nonnull ResolverContext<JcrProviderState> ctx, final @Nonnull Resource child) {
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
                log.warn("Can't get parent for {}", child, e);
                return null;
            }
        }
        return super.getParent(ctx, child);
    }

    @Override
    public Collection<String> getAttributeNames(final @Nonnull ResolverContext<JcrProviderState> ctx) {
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
    public Object getAttribute(final @Nonnull ResolverContext<JcrProviderState> ctx, final @Nonnull String name) {
        if (isAttributeVisible(name)) {
            if (ResourceResolverFactory.USER.equals(name)) {
                return ctx.getProviderState().getSession().getUserID();
            }
            return ctx.getProviderState().getSession().getAttribute(name);
        }
        return null;
    }

    @Override
    public Resource create(final @Nonnull ResolverContext<JcrProviderState> ctx, final String path, final Map<String, Object> properties)
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
    public void delete(final @Nonnull ResolverContext<JcrProviderState> ctx, final @Nonnull Resource resource)
    throws PersistenceException {
        try {
            ((JcrItemResource<?>) resource).getItem().remove();
        } catch (final RepositoryException e) {
            throw new PersistenceException("Unable to delete resource", e, resource.getPath(), null);
        }
    }

    @Override
    public void revert(final @Nonnull ResolverContext<JcrProviderState> ctx) {
        try {
            ctx.getProviderState().getSession().refresh(false);
        } catch (final RepositoryException ignore) {
            log.warn("Unable to revert pending changes.", ignore);
        }
    }

    @Override
    public void commit(final @Nonnull ResolverContext<JcrProviderState> ctx)
    throws PersistenceException {
        try {
            ctx.getProviderState().getSession().save();
        } catch (final RepositoryException e) {
            throw new PersistenceException("Unable to commit changes to session.", e);
        }
    }

    @Override
    public boolean hasChanges(final @Nonnull ResolverContext<JcrProviderState> ctx) {
        try {
            return ctx.getProviderState().getSession().hasPendingChanges();
        } catch (final RepositoryException ignore) {
            log.warn("Unable to check session for pending changes.", ignore);
        }
        return false;
    }

    @Override
    public void refresh(final @Nonnull ResolverContext<JcrProviderState> ctx) {
        try {
            ctx.getProviderState().getSession().refresh(true);
        } catch (final RepositoryException ignore) {
            log.warn("Unable to refresh session.", ignore);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public @CheckForNull <AdapterType> AdapterType adaptTo(final @Nonnull ResolverContext<JcrProviderState> ctx,
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
                log.debug("not able to adapto Resource to Principal, let the base class try to adapt");
            } catch (RepositoryException e) {
                log.warn("error while adapting Resource to Principal, let the base class try to adapt", e);
            }
        }
        return super.adaptTo(ctx, type);
    }

    @Override
    public boolean copy(final  @Nonnull ResolverContext<JcrProviderState> ctx,
            final String srcAbsPath,
            final String destAbsPath) throws PersistenceException {
        return false;
    }

    @Override
    public boolean move(final  @Nonnull ResolverContext<JcrProviderState> ctx,
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
