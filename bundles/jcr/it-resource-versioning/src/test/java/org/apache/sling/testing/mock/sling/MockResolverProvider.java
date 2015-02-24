package org.apache.sling.testing.mock.sling;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.helper.jcr.PathMapper;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.BundleContext;

public class MockResolverProvider {
    private MockResolverProvider() {
    }
    
    public static ResourceResolver getResourceResolver() throws RepositoryException, LoginException {
        final SlingRepository repository = RepositoryProvider.instance().getRepository();
        final BundleContext bundleContext = MockOsgi.newBundleContext();
        bundleContext.registerService(PathMapper.class.getName(), new PathMapper(), null);
        return new MockJcrResourceResolverFactory(repository, bundleContext).getAdministrativeResourceResolver(null);
    }
}
