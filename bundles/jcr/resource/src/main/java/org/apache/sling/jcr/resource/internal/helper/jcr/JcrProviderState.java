package org.apache.sling.jcr.resource.internal.helper.jcr;

import javax.jcr.Session;

import org.apache.sling.jcr.resource.internal.HelperData;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

class JcrProviderState {

    private final Session session;

    private final BundleContext bundleContext;

    private final ServiceReference repositoryRef;

    private final boolean logout;

    private final JcrItemResourceFactory resourceFactory;

    private final HelperData helperData;

    JcrProviderState(Session session, HelperData helperData, boolean logout) {
        this(session, helperData, logout, null, null);
    }

    JcrProviderState(Session session, HelperData helperData, boolean logout, BundleContext bundleContext, ServiceReference repositoryRef) {
        this.session = session;
        this.bundleContext = bundleContext;
        this.repositoryRef = repositoryRef;
        this.logout = logout;
        this.helperData = helperData;
        this.resourceFactory = new JcrItemResourceFactory(session, helperData);
    }

    Session getSession() {
        return session;
    }

    JcrItemResourceFactory getResourceFactory() {
        return resourceFactory;
    }

    HelperData getHelperData() {
        return helperData;
    }

    void logout() {
        if (logout) {
            session.logout();
        }
        if (bundleContext != null) {
            bundleContext.ungetService(repositoryRef);
        }
    }
}
