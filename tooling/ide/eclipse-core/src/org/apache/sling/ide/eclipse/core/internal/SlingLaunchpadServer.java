/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.eclipse.core.internal;

import org.apache.sling.ide.eclipse.core.ISlingLaunchpadConfiguration;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.osgi.framework.Version;

public class SlingLaunchpadServer extends ServerDelegate implements ISlingLaunchpadServer {

    private static final String PROP_AUTO_PUBLISH_SETTING = "auto-publish-setting";

    private static final String MODULE_TYPE_SLING_CONTENT = "sling.content";

    private static final String MODULE_TYPE_SLING_BUNDLE = "sling.bundle";

    private ISlingLaunchpadConfiguration config;

    @Override
    public ISlingLaunchpadConfiguration getConfiguration() {

        if (config != null) {
            return config;
        }

        return config = new SlingLaunchpadConfiguration(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.wst.server.core.model.ServerDelegate#canModifyModules(org.eclipse.wst.server.core.IModule[],
     * org.eclipse.wst.server.core.IModule[])
     */
    @Override
    public IStatus canModifyModules(IModule[] toAdd, IModule[] toRemove) {

        System.out.println("SlingLaunchpadServer.canModifyModules()");

        if (toAdd == null) {
            return Status.OK_STATUS;
        }

        for (IModule module : toAdd) {

            if (!MODULE_TYPE_SLING_CONTENT.equals(module.getModuleType().getId()) &&
            		!MODULE_TYPE_SLING_BUNDLE.equals(module.getModuleType().getId())) {
                return new Status(IStatus.ERROR, "org.apache.sling.slingclipse", 0,
                        "Will only handle modules of type 'sling.content' or 'sling.bundle'", null);
            }
        }

        return Status.OK_STATUS;
    }

    @Override
    public IModule[] getChildModules(IModule[] module) {
        if (module == null) {
            return null;
        }

        // no nested modules for now
        return new IModule[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.wst.server.core.model.ServerDelegate#getRootModules(org.eclipse.wst.server.core.IModule)
     */
    @Override
    public IModule[] getRootModules(IModule arg0) throws CoreException {

        if (MODULE_TYPE_SLING_CONTENT.equals(arg0.getModuleType().getId()) || 
        		MODULE_TYPE_SLING_BUNDLE.equals(arg0.getModuleType().getId())) {
            return new IModule[] { arg0 };
        }

        throw new CoreException(new Status(IStatus.ERROR, "org.apache.sling.slingclipse", 0,
                "Will only handle modules of type 'sling.content' or 'sling.bundle'", null));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.wst.server.core.model.ServerDelegate#modifyModules(org.eclipse.wst.server.core.IModule[],
     * org.eclipse.wst.server.core.IModule[], org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void modifyModules(IModule[] toAdd, IModule[] toRemove, IProgressMonitor arg2) throws CoreException {

        System.out.println("SlingLaunchpadServer.modifyModules()");

        IStatus status = canModifyModules(toAdd, toRemove);
        if (!status.isOK()) {
            throw new CoreException(status);
        }

        for (IModule module : toAdd) {
            System.out.println("Adding module " + module);
        }

        for (IModule module : toRemove) {
            System.out.println("Removing module " + module);
        }
    }

    @Override
    public void setDefaults(IProgressMonitor monitor) {

        setAttribute(PROP_PORT, 8080);
        setAttribute(PROP_DEBUG_PORT, 30303);
        setAttribute(PROP_CONTEXT_PATH, "/");
        setAttribute(PROP_USERNAME, "admin");
        setAttribute(PROP_PASSWORD, "admin");
    }

    @Override
    public int getPublishState() {
        return getAttribute(PROP_AUTO_PUBLISH_SETTING, PUBLISH_STATE_NEVER);
    }
    
    @Override
    public void setPublishState(int publishState, IProgressMonitor monitor) {
        System.out.println("[" + Thread.currentThread().getName() + "] Set " + PROP_AUTO_PUBLISH_SETTING + " to "
                + publishState);
        IServerWorkingCopy wc = getServer().createWorkingCopy();
		wc.setAttribute(PROP_AUTO_PUBLISH_SETTING, publishState);
		try {
			wc.save(false, monitor);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
    }

    @Override
    public Version getBundleVersion(String bundleSymbolicName) {

        String rawValue = getAttribute(String.format(PROP_BUNDLE_VERSION_FORMAT, bundleSymbolicName), (String) null);

        return rawValue != null ? new Version(rawValue) : null;
    }

    @Override
    public void setBundleVersion(String bundleSymbolicName, Version version, IProgressMonitor monitor) {

        String stringVersion = version != null ? version.toString() : null;

        IServerWorkingCopy wc = getServer().createWorkingCopy();
        wc.setAttribute(String.format(PROP_BUNDLE_VERSION_FORMAT, bundleSymbolicName), stringVersion);
        try {
            wc.save(false, monitor);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
}
