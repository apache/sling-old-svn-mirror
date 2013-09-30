package org.apache.sling.ide.eclipse.m2e.internal;

import org.apache.sling.ide.artifacts.EmbeddedArtifactLocator;
import org.apache.sling.ide.eclipse.core.ServiceUtil;
import org.apache.sling.ide.osgi.OsgiClientFactory;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator extends Plugin {

    public static final String PLUGIN_ID = "org.apache.sling.ide.eclipse-m2e-ui";
    public static Activator INSTANCE;

    private ServiceTracker<EmbeddedArtifactLocator, EmbeddedArtifactLocator> artifactLocator;
    private ServiceTracker<OsgiClientFactory, OsgiClientFactory> osgiClientFactory;

    public static Activator getDefault() {
        return INSTANCE;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        INSTANCE = this;

        artifactLocator = new ServiceTracker<EmbeddedArtifactLocator, EmbeddedArtifactLocator>(context, EmbeddedArtifactLocator.class, null);
        artifactLocator.open();

        osgiClientFactory = new ServiceTracker<OsgiClientFactory, OsgiClientFactory>(context, OsgiClientFactory.class,
                null);
        osgiClientFactory.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        INSTANCE = null;

        artifactLocator.close();

        super.stop(context);
    }

    public EmbeddedArtifactLocator getArtifactsLocator() {

        return ServiceUtil.getNotNull(artifactLocator);
    }

    public OsgiClientFactory getOsgiClientFactory() {

        return ServiceUtil.getNotNull(osgiClientFactory);
    }
}
