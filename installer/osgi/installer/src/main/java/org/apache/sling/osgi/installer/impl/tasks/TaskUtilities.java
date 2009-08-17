package org.apache.sling.osgi.installer.impl.tasks;

import java.io.IOException;

import org.apache.sling.osgi.installer.impl.ConfigurationPid;
import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

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

    /** Get or create configuration */
    public static Configuration getConfiguration(ConfigurationPid cp, boolean createIfNeeded, OsgiInstallerContext ocs)
    throws IOException, InvalidSyntaxException
    {
    	final ConfigurationAdmin configurationAdmin = ocs.getConfigurationAdmin();
    	if(configurationAdmin == null) {
    		throw new IllegalStateException("Missing service: " + ConfigurationAdmin.class.getName());
    	}

        Configuration result = null;

        if (cp.getFactoryPid() == null) {
            result = configurationAdmin.getConfiguration(cp.getConfigPid(), null);
        } else {
            Configuration configs[] = configurationAdmin.listConfigurations(
                "(|(" + ConfigInstallRemoveTask.ALIAS_KEY
                + "=" + cp.getFactoryPid() + ")(.alias_factory_pid=" + cp.getFactoryPid()
                + "))");

            if (configs == null || configs.length == 0) {
                if(createIfNeeded) {
                    result = configurationAdmin.createFactoryConfiguration(cp.getConfigPid(), null);
                }
            } else {
                result = configs[0];
            }
        }

        return result;
    }
}
