package org.apache.sling.osgi.installer.impl.tasks;

import java.io.IOException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/** Misc.utilities for classes of this package */
public class TaskUtilities {
    /**
     * Returns a bundle with the same symbolic name as the bundle provided in
     * the installable data. If the installable data has no manifest file or the
     * manifest file does not have a <code>Bundle-SymbolicName</code> header,
     * this method returns <code>null</code>. <code>null</code> is also
     * returned if no bundle with the same symbolic name as provided by the
     * input stream is currently installed.
     * <p>
     * This method gets its own input stream from the installable data object
     * and closes it after haing read the manifest file.
     *
     * @param RegisteredResource The installable data providing the bundle jar file
     *            from the input stream.
     * @return The installed bundle with the same symbolic name as the bundle
     *         provided by the input stream or <code>null</code> if no such
     *         bundle exists or if the input stream does not provide a manifest
     *         with a symbolic name.
     * @throws IOException If an error occurrs reading from the input stream.
     */
    public static Bundle getMatchingBundle(BundleContext ctx, String bundleSymbolicName) {

        if (bundleSymbolicName != null) {

            Bundle[] bundles = ctx.getBundles();
            for (Bundle bundle : bundles) {
                if (bundleSymbolicName.equals(bundle.getSymbolicName())) {
                    return bundle;
                }
            }
        }

    	return null;
    }
}
