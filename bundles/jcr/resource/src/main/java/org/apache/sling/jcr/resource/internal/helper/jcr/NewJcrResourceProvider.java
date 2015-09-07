package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.HelperData;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(value = ResourceProvider.class)
@Properties({ @Property(name = ResourceProvider.PROPERTY_NAME, value = "JCR"),
        @Property(name = ResourceProvider.PROPERTY_ROOT, value = "/"),
        @Property(name = ResourceProvider.PROPERTY_MODIFIABLE, boolValue = true),
        @Property(name = ResourceProvider.PROPERTY_AUTHENTICATE, value = ResourceProvider.AUTHENTICATE_LAZY) })
public class NewJcrResourceProvider extends ResourceProvider<JcrProviderState> {

    /** Logger */
    private static final Logger log = LoggerFactory.getLogger(NewJcrResourceProvider.class);

    private static final String REPOSITORY_REFERNENCE_NAME = "repository";

    @Reference(name = REPOSITORY_REFERNENCE_NAME, referenceInterface = SlingRepository.class)
    private ServiceReference repositoryReference;

    @Reference
    private PathMapper pathMapper;

    /** The dynamic class loader */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private volatile DynamicClassLoaderManager dynamicClassLoaderManager;

    private SlingRepository repository;

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

    /**
     * Create a new ResourceResolver wrapping a Session object. Carries map of
     * authentication info in order to create a new resolver as needed.
     */
    @Override
    @Nonnull public JcrProviderState authenticate(final @Nonnull Map<String, Object> authenticationInfo)
    throws LoginException {
        HelperData helperData = new HelperData(dynamicClassLoaderManager.getDynamicClassLoader(), pathMapper);
        JcrProviderStateFactory factory = new JcrProviderStateFactory(repositoryReference, repository, helperData);
        return factory.createProviderState(authenticationInfo);
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
    public Resource getResource(ResolveContext<JcrProviderState> ctx, String path, Resource parent) {
        try {
            return ctx.getProviderState().getResourceFactory().createResource(ctx.getResourceResolver(), path, ctx.getResolveParameters());
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
                    parent.getResourceResolver(), parent.getPath(), Collections.<String, String> emptyMap());
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
                String version = ctx.getResolveParameters().get("v");
                if (version == null) {
                    Item item = ((JcrItemResource<?>) child).getItem();
                    if ("/".equals(item.getPath())) {
                        return null;
                    }
                    Node parentNode = item.getParent();
                    String parentPath = pathMapper.mapJCRPathToResourcePath(parentNode.getPath());
                    return new JcrNodeResource(ctx.getResourceResolver(), parentPath, version, parentNode,
                            ctx.getProviderState().getHelperData());
                }
            } catch (RepositoryException e) {
                throw new SlingException("Can't get parent", e);
            }
        }
        return super.getParent(ctx, child);
    }

}
