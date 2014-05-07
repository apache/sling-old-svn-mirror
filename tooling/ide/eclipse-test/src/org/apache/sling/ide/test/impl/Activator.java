package org.apache.sling.ide.test.impl;

import org.apache.sling.ide.artifacts.EmbeddedArtifactLocator;
import org.apache.sling.ide.eclipse.core.ServiceUtil;
import org.apache.sling.ide.osgi.OsgiClientFactory;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator extends Plugin {

    private static Activator INSTANCE;

    private ServiceTracker<EmbeddedArtifactLocator, EmbeddedArtifactLocator> artifactLocator;

    private ServiceTracker<OsgiClientFactory, OsgiClientFactory> osgiClientFactory;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        artifactLocator = new ServiceTracker<EmbeddedArtifactLocator, EmbeddedArtifactLocator>(context,
                EmbeddedArtifactLocator.class, null);
        artifactLocator.open();

        osgiClientFactory = new ServiceTracker<OsgiClientFactory, OsgiClientFactory>(context, OsgiClientFactory.class,
                null);
        osgiClientFactory.open();

        INSTANCE = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {

        artifactLocator.close();
        osgiClientFactory.close();

        INSTANCE = null;

        super.stop(context);
    }

    public static Activator getDefault() {
        return INSTANCE;
    }

    public EmbeddedArtifactLocator getArtifactLocator() {

        return ServiceUtil.getNotNull(artifactLocator);
    }

    public OsgiClientFactory getOsgiClientFactory() {

        return ServiceUtil.getNotNull(osgiClientFactory);
    }
}
