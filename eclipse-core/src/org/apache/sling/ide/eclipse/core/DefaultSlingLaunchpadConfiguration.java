package org.apache.sling.ide.eclipse.core;

/**
 * The <tt>DefaultSlingLaunchpadConfiguration</tt> specifies reasonable defaults when
 * configuring a new Sling server instance.
 *
 */
public class DefaultSlingLaunchpadConfiguration implements ISlingLaunchpadConfiguration {

    public static final ISlingLaunchpadConfiguration INSTANCE = new DefaultSlingLaunchpadConfiguration();
    
    @Override
    public int getPort() {
        return 8080;
    }

    @Override
    public int getDebugPort() {
        return 30303;
    }

    @Override
    public String getContextPath() {
        return "/";
    }

    @Override
    public String getUsername() {
        return "admin";
    }

    @Override
    public String getPassword() {
        return "admin";
    }

    @Override
    public boolean bundleInstallLocally() {
        return true;
    }

    @Override
    public boolean resolveSourcesInDebugMode() {
        return true;
    }

}